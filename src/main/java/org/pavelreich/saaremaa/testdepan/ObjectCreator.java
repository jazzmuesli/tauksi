package org.pavelreich.saaremaa.testdepan;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

public class ObjectCreator {
	enum CreatorType {FIELD, METHOD};
	private CreatorType type;
	private String parentClass;
	private String parentMethod;

	ObjectCreator(CtClass parentClass) {
		this.parentClass = parentClass.getQualifiedName();
		this.type = CreatorType.FIELD;
	}
	ObjectCreator(CtClass parentClass, CtMethod parentMethod) {
		this(parentClass);
		this.parentMethod = parentMethod.getSignature();
		this.type = CreatorType.METHOD;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parentClass == null) ? 0 : parentClass.hashCode());
		result = prime * result + ((parentMethod == null) ? 0 : parentMethod.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectCreator other = (ObjectCreator) obj;
		if (parentClass == null) {
			if (other.parentClass != null)
				return false;
		} else if (!parentClass.equals(other.parentClass))
			return false;
		if (parentMethod == null) {
			if (other.parentMethod != null)
				return false;
		} else if (!parentMethod.equals(other.parentMethod))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ObjectCreator [type=" + type + ", parentClass=" + parentClass + ", parentMethod=" + parentMethod + "]";
	}


}
