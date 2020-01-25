package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pavelreich.saaremaa.testdepan.ITestAssert;
import org.pavelreich.saaremaa.testdepan.ITestMethod;
import org.pavelreich.saaremaa.testdepan.ObjectCreationContainer;
import org.pavelreich.saaremaa.testdepan.ObjectCreationOccurence;

public class TestMethod implements ITestMethod{

	private String methodName;
	List<ITestAssert> assertions = new ArrayList();
	Collection<String> annotations;
	List<ObjectCreationOccurence> mocks = new ArrayList();

	public TestMethod(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public Map<String, Object> toJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isTest() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSetup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<String> getAnnotations() {
		return new HashSet<String>(annotations);
	}

	@Override
	public List<ObjectCreationOccurence> getMocks() {
		return mocks;
	}

	@Override
	public List<ITestAssert> getAssertions() {
		return assertions;
	}

	@Override
	public String getName() {
		return methodName;
	}

	@Override
	public String toCSV() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int lineCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
