package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.List;
import java.util.Map;

import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.*;

public class TestClass implements ITestClass {

	private String className;
	private String packageName;

	@Override
	public Map<String, Object> toJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassName() {
		return packageName + "." + className;
	}

	@Override
	public List<ITestMethod> getTestMethods() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ITestField> getMockFields() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setPackageName(String fullyQualifiedName) {
		this.packageName = fullyQualifiedName;
	}

}
