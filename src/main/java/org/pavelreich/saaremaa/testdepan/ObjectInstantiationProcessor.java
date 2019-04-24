package org.pavelreich.saaremaa.testdepan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtPath;

import java.util.Map;

/**
 * Created by preich on 19/02/19.
 */
class ObjectInstantiationProcessor extends AbstractProcessor<CtConstructorCall> {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectInstantiationProcessor.class);

    private final ObjectCreationContainer objectsCreated;

    public ObjectInstantiationProcessor(ObjectCreationContainer objectsCreated) {
        this.objectsCreated = objectsCreated;
    }

    public void process(CtConstructorCall element) {
        CtElement parent = element.getParent();
        String name = "unknown";
        if (parent instanceof CtNamedElement) {
            name = ((CtNamedElement) parent).getSimpleName();
        }
        CtMethod method = element.getParent(CtMethod.class);
        CtClass parentClass = element.getParent(CtClass.class);
        ObjectCreator objectCreator;
        if (parentClass == null) {
        	return;
        }
        if (method != null) {
        	objectCreator = new ObjectCreator(parentClass, method);
        } else {
        	objectCreator = new ObjectCreator(parentClass);
        }
		
		ObjectCreationOccurence objectCreationOccurence = new ObjectCreationOccurence(element.getType(), element, InstanceType.REAL);
		objectsCreated.put(objectCreator, objectCreationOccurence);
        if (!element.getArguments().isEmpty() && element.getArguments().get(0) instanceof CtVariableRead) {
            CtVariableRead read = (CtVariableRead) element.getArguments().get(0);
            CtPath path = read.getPath();
//            if (objectsCreated.containsKey(read.getVariable().getSimpleName())) {
//                LOG.info("constructor [{}]={}, args={} path={}",
//                        new Object[]{element.getClass(), element, element.getArguments(), path});
//            }

        }

    }
}
