package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.pavelreich.saaremaa.testdepan.*;

public class TestClass implements ITestClass {

	private String className;
	private String packageName;
	List<ITestField> mockFields = new ArrayList();
	List<ITestMethod> testMethods = new ArrayList();

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
		return testMethods;
	}

	@Override
	public List<ITestField> getMockFields() {
		return mockFields;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setPackageName(String fullyQualifiedName) {
		this.packageName = fullyQualifiedName;
	}

	@Override
	public List<String> toCSV() {
		// TODO Auto-generated method stub
		return null;
	}

}
