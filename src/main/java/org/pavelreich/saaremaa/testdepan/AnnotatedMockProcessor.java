package org.pavelreich.saaremaa.testdepan;

import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;

public class AnnotatedMockProcessor extends AbstractProcessor<CtField> {
	static final Logger LOG = LoggerFactory.getLogger(AnnotatedMockProcessor.class);
	private ObjectCreationContainer objectsCreated;
	

	public AnnotatedMockProcessor(ObjectCreationContainer objectsCreated) {
		this.objectsCreated = objectsCreated;
	}

	@Override
	public void process(CtField element) {
		if (element.getAnnotation(Mock.class) != null) {
			objectsCreated.put(new ObjectCreator(element.getParent(CtClass.class)),
					new ObjectCreationOccurence(element.getType(), element, InstanceType.MOCKITO));
			LOG.info("field [{}]={} annotations={}", element.getClass(), element,
					element.getAnnotation(Mock.class));
		}

	}

}