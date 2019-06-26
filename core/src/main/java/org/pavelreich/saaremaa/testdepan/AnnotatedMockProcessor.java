package org.pavelreich.saaremaa.testdepan;

import java.util.List;
import java.util.stream.Collectors;

import org.mockito.Mock;
import org.slf4j.Logger;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;

public class AnnotatedMockProcessor extends AbstractProcessor<CtField> {
	final Logger LOG;
	private ObjectCreationContainer objectsCreated;
	

	public AnnotatedMockProcessor(Logger log, ObjectCreationContainer objectsCreated) {
		this.LOG = log;
		this.objectsCreated = objectsCreated;
	}

	@Override
	public void process(CtField element) {
		List<String> anns = element.getAnnotations().stream().map(p->p.getAnnotationType().getQualifiedName()).collect(Collectors.toList());
		if (anns.contains("org.mockito.Mock") || element.getAnnotation(Mock.class) != null) {
			ObjectCreator objectCreator = new ObjectCreator(element.getParent(CtClass.class));
			ObjectCreationOccurence objectCreationOccurence = new ObjectCreationOccurence(element.getType(), element, InstanceType.MOCKITO);
			objectsCreated.put(objectCreator,
					objectCreationOccurence);
			LOG.info("field [" + element.getClass() + "]=" + element + "  annotations=" + element.getAnnotation(Mock.class));
		}

	}

}