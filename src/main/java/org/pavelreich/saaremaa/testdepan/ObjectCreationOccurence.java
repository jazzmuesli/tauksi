package org.pavelreich.saaremaa.testdepan;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtTypeReference;

/**
 * Created by preich on 19/02/19.
 */
class ObjectCreationOccurence {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectCreationOccurence.class);

    private CtElement element;
    private CtTypeReference typeRef;
    private InstanceType instanceType;

    public ObjectCreationOccurence(CtTypeReference mock, CtElement element, InstanceType instanceType) {
        this.typeRef = mock;
        this.element = element;
        this.instanceType = instanceType;
    }

    public String toCSV() {
        Integer line = null;
        String absolutePath = null;
        try {
            absolutePath = getAbsolutePath();
            line = typeRef.getPosition() instanceof NoSourcePosition ? null : typeRef.getPosition().getLine();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return instanceType + ";" + typeRef.toString() + ";" + absolutePath + ";" + line;
    }

    String getAbsolutePath() {
        try {
            return element.getPosition().getFile().getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public String getName() {
    	if (element instanceof CtInvocation || element instanceof CtConstructorCall) {
    		CtLocalVariable x = element.getParent(CtLocalVariable.class);
    		if (x != null) {
    			return x.getSimpleName();
    		}
    		CtAssignment a = element.getParent(CtAssignment.class);
    		if (a != null && a.getAssigned() instanceof CtFieldAccess) {
    			return ((CtFieldAccess)a.getAssigned()).getVariable().getSimpleName();
    		}
    	}
    	if (element instanceof CtField) {
    		return ((CtField) element).getSimpleName();
    	}
    	return "unknown";
    }
    @Override
    public String toString() {
        return "[name=" + getName()+ ", type=" + this.instanceType + ", class=" + getType() + ", element.position=" + element.getPosition() + "]";
    }

	public Map<String,Object> toJSON() {
		Map<String, Object> map = new HashMap();
		map.put("name", getName());
		map.put("class", getType());
		map.put("type", this.instanceType);
		return map;
	}

	private String getType() {
		if(typeRef != null && typeRef.getTypeDeclaration() != null) {
			return typeRef.getTypeDeclaration().getQualifiedName();
		}
		if (typeRef != null) {
			return typeRef.getQualifiedName();
		}
		return "unknown";
	}
}
