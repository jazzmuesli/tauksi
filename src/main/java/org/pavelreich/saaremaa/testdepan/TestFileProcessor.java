package org.pavelreich.saaremaa.testdepan;

import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import spoon.Launcher;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.CtVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Analyse test classes, extract methods with annotations extract fields with
 * types and annotations.
 *
 * java -classpath target/tauksi-1.0-SNAPSHOT-jar-with-dependencies.jar
 * org.pavelreich.saaremaa.testdepan.TestFileProcessor
 */
public class TestFileProcessor extends AbstractProcessor<CtClass> {
	static final Set<String> SETUP_CLASSES = new HashSet<String>(Arrays.asList(BeforeClass.class.getSimpleName(),
			Before.class.getSimpleName(), After.class.getSimpleName(), AfterClass.class.getSimpleName()));

	private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessor.class);

	private List<MyClass> elements = new CopyOnWriteArrayList<>();

	private ObjectCreationContainer objectsCreated;

	public TestFileProcessor(ObjectCreationContainer objectsCreated) {
		this.objectsCreated = objectsCreated;
	}

	public List<MyClass> getElements() {
		return elements;
	}

	@Override
	public void process(CtClass ctClass) {
		MyClass myClass = new MyClass(ctClass);
		if (myClass.hasTests()) {
			LOG.info("myClass:" + myClass);
			this.elements.add(myClass);
		}

	}

	public static void main(String[] args) {
		try {
			String srcTestDir = args.length > 0 ? args[0] : ".";
			run(srcTestDir, "result.json");
		} catch (FileNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static TestFileProcessor run(String pathname, String resultFileName) throws FileNotFoundException {
		Launcher launcher = new Launcher();
		SpoonResource resource = SpoonResourceHelper.createResource(new File(pathname));
		launcher.addInputResource(resource);
		launcher.buildModel();

		CtModel model = launcher.getModel();

		ObjectCreationContainer objectsCreated = new ObjectCreationContainer();
		TestFileProcessor processor = new TestFileProcessor(objectsCreated);
		processWithModel(model, new MockProcessor(objectsCreated));
		processWithModel(model, new AnnotatedMockProcessor(objectsCreated));
		processWithModel(model, new ObjectInstantiationProcessor(objectsCreated));
		// order matters and it's bad :(
		processWithModel(model, processor);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(processor.getElements().stream().map(x -> x.toJSON()).collect(Collectors.toList()));
		try {
			FileWriter fw = new FileWriter(resultFileName);
			fw.write(json);
			fw.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return processor;
	}

	private static void processWithModel(CtModel model, Processor processor) {
		try {
			model.processWith(processor);
		} catch (Exception e) {
			LOG.error("Can't process using processor " + processor + " due to " + e.getMessage(), e);
		}
	}

	class MyClass {

		private final CtClass ctClass;
		private Map<String, MyMethod> methods = new HashMap<>();
		private Set<String> annotations = new HashSet<>();
		private List<MyField> fields = new ArrayList<>();
		private Map<String, Object> annotationsMap;

		public MyClass(CtClass ctClass) {
			this.ctClass = ctClass;
			try {
				this.annotations = getAnnotations(ctClass);
				this.annotationsMap = ctClass.getAnnotations().stream().filter(p -> p.getValues().containsKey("value"))
						.collect(Collectors.toMap(e -> e.getAnnotationType().getQualifiedName(),
								e -> e.getValues().get("value").toString()));
			} catch (Exception e) {
				LOG.error("Error occured for annotations of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
			try {
				Set<CtMethod> allMethods = ctClass.getAllMethods();
				this.methods = allMethods.stream().map(x -> new MyMethod(this, x)).filter(p -> p.isPublicVoidMethod())
						.collect(Collectors.toMap(e -> e.simpleName, e -> e));
			} catch (Exception e) {
				LOG.error("Error occured for methods of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
			try {
				List<CtField> fields = ctClass.getFields();
				this.fields = fields.stream().map(x -> new MyField(this, x)).collect(Collectors.toList());
			} catch (Exception e) {
				LOG.error("Error occured for fields of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
		}

		Map<String, Object> toJSON() {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("simpleName", ctClass.getQualifiedName());
			map.put("annotations", annotations);
			map.put("annotationsMap", annotationsMap);
			map.put("testMethods", getTestMethods().stream().map(x -> x.toJSON()).collect(Collectors.toList()));
			map.put("setupMethods", getSetupMethods().stream().map(x -> x.toJSON()).collect(Collectors.toList()));
			Collection<ObjectCreationOccurence> mockFields = objectsCreated.get(new ObjectCreator(ctClass));
			map.put("mockFields", mockFields.stream().map(x -> x.toJSON()).collect(Collectors.toList()));
			map.put("fields", fields.stream().map(x -> x.toJSON()).collect(Collectors.toList()));
			return map;
		}

		boolean hasTests() {
			return this.methods.values().stream().anyMatch(p -> p.isTest());
		}

		List<MyMethod> getTestMethods() {
			return this.methods.values().stream().filter(p -> p.isTest()).collect(Collectors.toList());
		}

		List<MyMethod> getSetupMethods() {
			return this.methods.values().stream().filter(p -> p.isSetup()).collect(Collectors.toList());
		}

		@Deprecated
		List<MyField> getMockFields() {
			return this.fields.stream().filter(p -> !p.getMockType().isEmpty()).collect(Collectors.toList());
		}

		@Override
		public String toString() {
			List<MyMethod> testMethods = getTestMethods();
			List<MyField> mockFields = getMockFields();
			return "ctClass=" + ctClass.getQualifiedName() + ", annotations.size=" + annotations.size() + ":"
					+ annotations + ", methods.size=" + testMethods.size() + ":" + testMethods + ", fields.size="
					+ mockFields.size() + ":" + mockFields;
		}
	}

	class MyField {

		private final CtField ctField;
		private final Set<String> annotations;
		private final String simpleName;
		private final String typeName;
		private final CtExpression defaultExpression;
		private MyClass myClass;

		public MyField(MyClass myClass, CtField ctField) {
			this.myClass = myClass;
			this.ctField = ctField;
			this.simpleName = ctField.getSimpleName();
			this.annotations = getAnnotations(ctField);
			this.typeName = ctField.getType().getQualifiedName();
			this.defaultExpression = ctField.getDefaultExpression();
		}

		public HashMap toJSON() {
			HashMap map = new HashMap();
			map.put("simpleName", simpleName);
			map.put("annotations", annotations);
			map.put("typeName", typeName);
			map.put("mockType", getMockType());
			return map;
		}

		public String getMockType() {
			if (defaultExpression instanceof CtInvocation) {
				CtExecutableReference exec = ((CtInvocation) defaultExpression).getExecutable();
				if (!exec.getSimpleName().contains("mock")) {
					return "";
				}
				List arguments = ((CtInvocation) defaultExpression).getArguments();
				Optional found = arguments.stream().filter(p -> p.toString().contains(".class")).findFirst();
				if (found.isPresent()) {
					return String.valueOf(found.get()).replace(".class", "");
				}
			}
			if (annotations.contains("Mock")) {
				return typeName;
			}
			return "";
		}

		@Override
		public String toString() {
			return "MyField[simpleName=" + simpleName + ", typeName=" + typeName + ", annotations=" + annotations
					+ ", mockType=" + getMockType() + "]";
		}
	}

	class MyMethod {

		final Set<String> annotations;
		final String simpleName;
		final CtMethod method;
		private MyClass myClass;

		public MyMethod(MyClass myClass, CtMethod e) {
			this.myClass = myClass;
			this.simpleName = e.getSimpleName();
			this.annotations = getAnnotations(e);
			this.method = e;
		}

		public HashMap toJSON() {
			HashMap map = new HashMap();
			map.put("simpleName", simpleName);
			map.put("annotations", annotations);
			map.put("LOC", lineCount());
			map.put("statementCount", statementCount());
			ObjectCreator key = new ObjectCreator(this.method.getParent(CtClass.class), this.method);
			Collection<ObjectCreationOccurence> creations = objectsCreated.get(key);
			List mocks = creations.stream().map(x -> x.toJSON()).collect(Collectors.toList());
			map.put("mocks", mocks);

			return map;
		}

		private boolean isPublicVoidMethod() {
			CtMethod p = method;
			return p.getParameters().isEmpty() && p.isPublic() && isVoid(p);
		}

		boolean isTest() {
			return annotations.contains("Test");
		}

		boolean isSetup() {
			return !annotations.isEmpty() && SETUP_CLASSES.containsAll(annotations);
		}

		private boolean isVoid(CtMethod p) {
			String simpleName = p.getType().getSimpleName();
			return simpleName.contains("void");
		}

		int lineCount() {
			CtBlock body = this.method.getBody();
			int loc = body.toString().split("\n").length;
			return loc;
		}

		int statementCount() {
			List<CtStatement> statements = method.getBody().getStatements();
			return statements.size();
		}

		@Override
		public String toString() {
			return "MyMethod[simpleName=" + simpleName + ", annotations=" + annotations + ", LOC=" + lineCount()
					+ ", statementCount=" + statementCount() + "]";
		}
	}

	private static Set<String> getAnnotations(CtElement element) {
		return element.getAnnotations().stream().map(a -> a.getAnnotationType().getSimpleName())
				.collect(Collectors.toSet());
	}

}
