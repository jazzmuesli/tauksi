package org.pavelreich.saaremaa.testdepan;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * process mock instantiation in methods
 * @author preich
 *
 */
class MockProcessor extends AbstractProcessor<CtInvocation> {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectCreationOccurence.class);
    private final ObjectCreationContainer objectsCreated;

    public MockProcessor(ObjectCreationContainer objectsCreated) {
        this.objectsCreated = objectsCreated;
    }

    @Override
    public void process(CtInvocation element) {
        processMockInvocation(element, "org.mockito.Mockito.mock", InstanceType.MOCKITO);
        processMockInvocation(element, "PowerMockito.mock", InstanceType.POWERMOCK);
    }

    private void processMockInvocation(CtInvocation element, String mockMask, InstanceType mockType) {
        if (element.toString().contains(mockMask)) {
            @SuppressWarnings("unused")
            String elType = element.getTarget().toString();
            try {
            	CtMethod method = element.getParent(CtMethod.class);
            	CtClass klasse = element.getParent(CtClass.class);
            	if (klasse == null) {
            		return;
            	}
                String simpleName = getSimpleName(element);
                
                CtExpression type = ((CtFieldRead) element.getArguments().get(0)).getTarget();
                Set<CtTypeReference<?>> x = type.getReferencedTypes();
                CtTypeReference<?> mock = x.iterator().next();
                ObjectCreator objectCreator = new ObjectCreator(klasse, method);
				ObjectCreationOccurence objectCreationOccurence = new ObjectCreationOccurence(mock, element, mockType);
				objectsCreated.put(objectCreator, objectCreationOccurence);
                LOG.info("invocation [{}]={} args={} annotations={}", element.getClass(), element,
                        element.getArguments(), element.getAnnotations());
            } catch (Throwable e) {
                LOG.error("Can't parse element {} at {} due to error {}",
                        new Object[]{element, element.getPosition(), e.getMessage()}, e);
            }
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
