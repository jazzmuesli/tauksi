package org.pavelreich.saaremaa.testdepan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.pavelreich.saaremaa.CSVReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;

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
	public static final String DELIM = ";";

	private final Logger LOG;

	private List<ITestClass> elements = new CopyOnWriteArrayList<>();

	private ObjectCreationContainer objectsCreated;

	public TestFileProcessor(Logger log, ObjectCreationContainer objectsCreated) {
		this.LOG = log;
		this.objectsCreated = objectsCreated;
	}

	public List<ITestClass> getElements() {
		return elements;
	}

	@Override
	public void process(CtClass ctClass) {
		MyClass myClass = new MyClass(ctClass);
		boolean h = myClass.hasTests();
		if (h) {
			this.elements.add(myClass);
		}
//		LOG.info("myClass.hasTests[" + h + ":" + myClass);
	}
/*
	public static void main(String[] args) {
		try {
			String srcTestDir = args.length > 0 ? args[0] : ".";
			run(srcTestDir, "result.json");
		} catch (FileNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
	}
*/
	
	public static TestFileProcessor run(Logger log, String pathname, String resultFileName) throws FileNotFoundException {
		try {
			Launcher launcher = new Launcher();
			SpoonResource resource = SpoonResourceHelper.createResource(new File(pathname));
			launcher.addInputResource(resource);
			launcher.buildModel();

			CtModel model = launcher.getModel();

			ObjectCreationContainer objectsCreated = new ObjectCreationContainer();
			TestFileProcessor processor = new TestFileProcessor(log, objectsCreated);
			processor.processWithModel(model, new MockProcessor(log, objectsCreated));
			processor.processWithModel(model, new AnnotatedMockProcessor(log, objectsCreated));
			processor.processWithModel(model, new ObjectInstantiationProcessor(log, objectsCreated));
			// order matters and it's bad :(
			processor.processWithModel(model, processor);
			if (resultFileName != null) {
				processor.writeResults(resultFileName);
			}
			return processor;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	void writeResults(String resultFileName) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(getElements().stream().map(x -> x.toJSON()).collect(Collectors.toList()));
		try {
			FileWriter fw = new FileWriter(resultFileName);
			fw.write(json);
			fw.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void processWithModel(CtModel model, Processor processor) {
		try {
			model.processWith(processor);
		} catch (Exception e) {
			LOG.error("Can't process using processor " + processor + " due to " + e.getMessage(), e);
		}
	}

	

	public class MyClass implements ITestClass {

		private final CtClass ctClass;
		private Map<String, ITestMethod> methods = new HashMap<>();
		private Set<String> annotations = new HashSet<>();
		private List<MyField> fields = new ArrayList<>();
		private Map<String, Object> annotationsMap;

		public String getClassName() {
			return ctClass.getQualifiedName();
		}
		public MyClass(CtClass ctClass) {
			this.ctClass = ctClass;
			try {
				this.annotations = createAnnotations(ctClass);
				this.annotationsMap = ctClass.getAnnotations().stream().filter(p -> p.getValues().containsKey("value"))
						.collect(Collectors.toMap(e -> e.getAnnotationType().getQualifiedName(),
								e -> e.getValues().get("value").toString()));
			} catch (Exception e) {
				//TODO: use junit5-api
//				LOG.error("Error occured for annotations of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
			try {
				Set<CtMethod> allMethods = ctClass.getAllMethods();
				this.methods = allMethods.stream().map(x -> new MyMethod(this, x)).filter(p -> p.isPublicVoidMethod())
						.collect(Collectors.toMap(e -> e.simpleName, e -> e));
			} catch (Exception e) {
//				LOG.error("Error occured for methods of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
			try {
				List<CtField> fields = ctClass.getFields();
				this.fields = fields.stream().map(x -> new MyField(this, x)).collect(Collectors.toList());
			} catch (Exception e) {
//				LOG.error("Error occured for fields of class:" + ctClass + ", error: " + e.getMessage(), e);
			}
		}

		public Map<String, Object> toJSON() {
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
			return !this.ctClass.isAbstract() && this.methods.values().stream().anyMatch(p -> p.isTest());
		}

		public List<ITestMethod> getTestMethods() {
			return this.methods.values().stream().filter(p -> p.isTest()).collect(Collectors.toList());
		}

		List<ITestMethod> getSetupMethods() {
			return this.methods.values().stream().filter(p -> p.isSetup()).collect(Collectors.toList());
		}

		@Deprecated
		public
		List<ITestField> getMockFields() {
			return this.fields.stream().filter(p -> !p.getMockType().isEmpty()).collect(Collectors.toList());
		}

		@Override
		public String toString() {
			return "ctClass=" + ctClass.getQualifiedName() + ", mocks=" + getMocks().size();
		}
		@Override
		public List<String> toCSV() {
			List<String> ret = getTestMethods().stream().map(x->getClassName()+DELIM+x.toCSV()).collect(Collectors.toList());
			return ret;
		}
	}


	class MyField implements ITestField {

		private final CtField ctField;
		private final Set<String> annotations;
		private final String simpleName;
		private final String typeName;
		private final CtExpression defaultExpression;
		private MyClass myClass;
		private int startLine;

		public MyField(MyClass myClass, CtField ctField) {
			this.myClass = myClass;
			this.ctField = ctField;
			this.simpleName = ctField.getSimpleName();
			this.annotations = createAnnotations(ctField);
			this.typeName = ctField.getType().getQualifiedName();
			this.defaultExpression = ctField.getDefaultExpression();
			this.startLine = ctField.getPosition().isValidPosition() ? ctField.getPosition().getLine() : -1;
		}

		public HashMap toJSON() {
			HashMap map = new HashMap();
			map.put("simpleName", simpleName);
			map.put("annotations", annotations);
			map.put("typeName", typeName);
			map.put("mockType", getMockType());
			return map;
		}

		@Override
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

		@Override
		public int getLine() {
			return startLine;
		}

		@Override
		public String getName() {
			return simpleName;
		}
	}

	class MyAssert implements ITestAssert {
		final String className;
		final String methodName;
		final List<String> argTypes;
		final int line;

		public MyAssert(String className, String methodName, List<String> args, int linePos) {
			this.className = className;
			this.methodName = methodName;
			this.argTypes = args;
			this.line = linePos;
		}


		public HashMap toJSON() {
			HashMap map = new HashMap();
			map.put("className", className);
			map.put("methodName", methodName);
			map.put("argTypes", argTypes);
			map.put("line", line);
			return map;
		}


		public String toString() {
			return "[className=" + className + ", methodName=" +methodName + "]";
		}
		public String getClassName() {
			return className;
		}


		public String getMethodName() {
			return methodName;
		}


		public List<String> getArgTypes() {
			return argTypes;
		}


		public int getLine() {
			return line;
		}
		
	}
	class MyMethod implements ITestMethod {

		final Set<String> annotations;
		final List<ITestAssert> assertions;
		final String simpleName;
		final CtMethod method;
		private MyClass myClass;
		private int startLine;

		public MyMethod(MyClass myClass, CtMethod e) {
			this.myClass = myClass;
			this.simpleName = e.getSimpleName();
			this.annotations = createAnnotations(e);
			this.assertions = getAssertions(e);
			this.startLine = e.getPosition().isValidPosition() ? e.getPosition().getLine() : -1;
			this.method = e;
		}
		
		@Override
		public Set<String> getAnnotations() {
			return annotations;
		}

		@Override
		public List<ITestAssert> getAssertions() {
			return assertions;
		}
		private List<ITestAssert> getAssertions(CtMethod method) {
			List<CtInvocation> invocations =new CopyOnWriteArrayList<>();
			invocations = method.getElements(new Filter() {

				@Override
				public boolean matches(CtElement element) {
					return element instanceof CtInvocation && isAssertMethod((CtInvocation) element);
				}
				
			});
			List<ITestAssert> asserts = invocations.stream().
			map(x -> new MyAssert(getClassName(x), 
					x.getExecutable().getSimpleName(), 
					getArgTypes(x),
					x.getPosition().getLine())).collect(Collectors.toList());
			return asserts;
		}
		private String getClassName(CtInvocation x) {
			String className = String.valueOf(x.getTarget());
			try {
				CtExecutableReference executable = x.getExecutable();
				CtTypeReference declaringType = executable.getDeclaringType();
				return declaringType.getQualifiedName();
			} catch (Exception e) {
				return className;
			}
		}

		private boolean isAssertMethod(CtInvocation p) {
			CtExecutableReference executable = p.getExecutable();
			return executable.getSimpleName().toLowerCase().contains("assert");
//			Method actualMethod = executable.getActualMethod();
//			return actualMethod.getName().toLowerCase().contains("assert");
		}

		private List<String> getArgTypes(CtInvocation x) {
			List<CtExpression> arguments = x.getArguments();
			Stream<String> map = arguments.stream().map(a -> getArgType(a));
			return map.collect(Collectors.toList());
		}

		private String getArgType(CtExpression a) {
			CtTypeReference type = a.getType();
			if (type == null) {
				return "unknown";
			}
			return type.getQualifiedName();
		}

		public HashMap toJSON() {
			HashMap map = new HashMap();
			map.put("simpleName", simpleName);
			map.put("annotations", annotations);
			map.put("assertions", assertions);
			map.put("LOC", lineCount());
			map.put("startLine", startLine);
			map.put("statementCount", statementCount());
			List mocks = getMocks().stream().map(x->x.toJSON()).collect(Collectors.toList());
			map.put("mocks", mocks);

			return map;
		}

		@Override
		public List<ObjectCreationOccurence> getMocks() {
			ObjectCreator key = new ObjectCreator(this.method.getParent(CtClass.class), this.method);
			Collection<ObjectCreationOccurence> creations = objectsCreated.get(key);
			return creations.stream().filter(p->p.getInstanceType() != InstanceType.REAL).collect(Collectors.toList());
		}

		private boolean isPublicVoidMethod() {
			CtMethod p = method;
			return p.getParameters().isEmpty() && p.isPublic() && isVoid(p);
		}

		public boolean isTest() {
			return annotations.contains("Test");
		}

		public boolean isSetup() {
			return !annotations.isEmpty() && SETUP_CLASSES.containsAll(annotations);
		}

		private boolean isVoid(CtMethod p) {
			String simpleName = p.getType().getSimpleName();
			return simpleName.contains("void");
		}

		int lineCount() {
			try {
			CtBlock body = this.method.getBody();
			int loc = body.toString().split("\n").length;
			return loc;
			} catch (Exception e) {
				return 0;
			}
		}

		int statementCount() {
			try {
			List<CtStatement> statements = method.getBody().getStatements();
			return statements.size();
			} catch (Exception e) {
				return 0;
			}
		}

		@Override
		public String toString() {
			return "MyMethod[simpleName=" + simpleName + ", annotations=" + annotations + ", LOC=" + lineCount()
					+ ", statementCount=" + statementCount() + "]";
		}

		@Override
		public String getName() {
			return simpleName;
		}

		@Override
		public String toCSV() {
			Optional<ITestAssert> assertsStartAtLine = getAssertions().stream().min((a,b) -> Long.compare(a.getLine(), b.getLine()));
			Optional<ObjectCreationOccurence> mocksStartAtLine = getMocks().stream().min((a,b) -> Long.compare(a.getLine(), b.getLine()));
			String line = Arrays.asList(
					getName(), 
					getAssertions().size(), 
					getMocks().size(),
					this.myClass.getMockFields().size(),
					startLine,
					assertsStartAtLine.isPresent() ? assertsStartAtLine.get().getLine() : -1,
					mocksStartAtLine.isPresent() ? mocksStartAtLine.get().getLine() : -1,
					lineCount()
					).stream().map(x->String.valueOf(x)).
					collect(Collectors.joining(DELIM));
			return line;
		}
	}

	private static Set<String> createAnnotations(CtElement element) {
		return element.getAnnotations().stream().map(a -> a.getAnnotationType().getSimpleName())
				.collect(Collectors.toSet());
	}

	enum MockType { VARIABLE, FIELD}
	static class MockOccurence {

		MockType mockType;
		String className;
		String mockClass;
		int line;
		String name;
		public MockOccurence(MockType mockType, String className, String mockClass, int line, String name) {
			super();
			this.mockType = mockType;
			this.className = className;
			this.mockClass = mockClass;
			this.line = line;
			this.name = name;
		}

		
	}
	public void writeMockito(String fname) {
		List<MockOccurence> mocks = getMocks();
		try {
			CSVReporter reporter = new CSVReporter(fname, "mockType","testClassName","mockClassName","mockOccurenceLine","mockName");
			for (MockOccurence mock : mocks) {
				reporter.write(mock.mockType.name(), mock.className, mock.mockClass, mock.line, mock.name);
			}
			reporter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<MockOccurence> getMocks() {
		List<ITestClass> classes = getElements();

		List<MockOccurence> mocks = new ArrayList();
		for(ITestClass clazz : classes) {
			for (ITestField mockField : clazz.getMockFields()) {
				mocks.add(new MockOccurence(MockType.FIELD,clazz.getClassName(), mockField.getMockType(), mockField.getLine(), mockField.getName()));
			}

			for (ITestMethod testMethod : clazz.getTestMethods()) {
				for (ObjectCreationOccurence x: testMethod.getMocks()) {
					if (x.getInstanceType() != InstanceType.REAL) {
						mocks.add(new MockOccurence(MockType.VARIABLE, clazz.getClassName(), x.getClassName(), x.getLine(), x.getName()));
					}
				}
			}
		}
		return mocks;
	}
	public void writeCSVResults(String assertsFileName) {
		List<ITestClass> elements = getElements();
		String[] fields = ITestClass.getFields().split(TestFileProcessor.DELIM);
		try {
			CSVFormat format = CSVFormat.DEFAULT.
			withHeader(fields).
			withDelimiter(';').
			withQuoteMode(QuoteMode.MINIMAL).
			withSystemRecordSeparator();

			CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(assertsFileName)),
					format
					);
			CSVReporter assertsReporter = new CSVReporter(printer);
			for (ITestClass element : elements) {
				List<String> lines = element.toCSV();
				for (String line : lines) {
					String[] vals = line.split(DELIM);
					assertsReporter.write(vals);
				}
				assertsReporter.flush();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
	}

}
