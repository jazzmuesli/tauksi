package org.pavelreich.saaremaa.testdepan;

import java.util.Set;

import org.slf4j.Logger;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * process mock instantiation in methods
 * 
 * @author preich
 *
 */
class MockProcessor extends AbstractProcessor<CtInvocation> {
	private final Logger LOG;
	private final ObjectCreationContainer objectsCreated;

	public MockProcessor(Logger log, ObjectCreationContainer objectsCreated) {
		this.LOG = log;
		this.objectsCreated = objectsCreated;
	}

	@Override
	public void process(CtInvocation element) {
		processMockInvocation(element, "org.mockito.Mockito.mock", InstanceType.MOCKITO);
//		TODO: processMockInvocation(element, "PowerMockito.mock", InstanceType.POWERMOCK);
	}

	private void processMockInvocation(CtInvocation element, String mockMask, InstanceType mockType) {
		try {
			//TODO: link element to method name
			CtExecutableReference exec = element.getExecutable();
			
			if ((element.toString().contains(mockMask) || exec.getSimpleName().startsWith("mock"))
					&& element.getTarget() != null) {
				@SuppressWarnings("unused")
				String elType = element.getTarget().toString();
				CtMethod method = element.getParent(CtMethod.class);
				CtClass klasse = element.getParent(CtClass.class);
//				LOG.info("invocation.element: " + element.getPosition() + ", klasse: " + klasse + ", target: " + element.getTarget() + ", element: " + element.toString());
				if (klasse == null) {
					LOG.warn("Element " + element + " has no CtClass");
					return;
				}
				String simpleName = getSimpleName(element);

				if (element.getArguments().isEmpty()) {
					LOG.warn("Element " + element + " has no arguments");
					return;
				}
				Object firstArg = element.getArguments().get(0);
				CtTypeReference mock = null;
				if (firstArg instanceof CtFieldRead) {
					CtExpression type = ((CtFieldRead) firstArg).getTarget();
					Set<CtTypeReference<?>> refTypes = type.getReferencedTypes();
					if (!refTypes.isEmpty()) {
						mock = refTypes.iterator().next();
					}
				} else if (firstArg instanceof CtVariableRead) {
					mock = ((CtVariableRead) firstArg).getType();
				} else if (firstArg instanceof CtNewClass) {
					CtExpression type = ((CtNewClass) firstArg).getTarget();
					if (type != null) {
						Set<CtTypeReference<?>> x = type.getReferencedTypes();
						mock = x.iterator().next();
					}
				}
				if (mock == null) {
					LOG.info("Element " + element + " produced no mock");
					return;
				}
				if (method == null || method.getSignature() == null) {
					LOG.warn("Element " + element + "  has no parent");
					return;
				}
				ObjectCreator objectCreator = new ObjectCreator(klasse, method);
				ObjectCreationOccurence objectCreationOccurence = new ObjectCreationOccurence(mock, element, mockType);
				objectsCreated.put(objectCreator, objectCreationOccurence);
				LOG.info("invocation [" + element.getClass() + "]=" + element + "  args=" + element.getArguments()
						+ " annotations=" + element.getAnnotations() + " resulted in  " + objectCreationOccurence);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			LOG.error("Can't parse element " + element + " due to error "
					+ e.getMessage(), e);
		}
	}

	private String getSimpleName(CtInvocation element) {
		if (element instanceof CtAssignment) {
			CtExpression x = ((CtAssignment) element).getAssignment();
			return x.toString();
		}
		CtElement parent = element.getParent();
		if (parent instanceof CtNamedElement) {
			String simpleName = ((CtNamedElement) parent).getSimpleName();
			return simpleName;
		} else {
			return "UNKNOWN:" + parent.getClass().getSimpleName() + "/" + element.getClass().getSimpleName();
		}
	}

}
