package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pavelreich.saaremaa.testdepan.ITestAssert;
import org.pavelreich.saaremaa.testdepan.ITestMethod;

public class TestMethod implements ITestMethod{

	public TestMethod(String methodName) {
		// TODO Auto-generated constructor stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> getMocks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ITestAssert> getAssertions() {
		// TODO Auto-generated method stub
		return null;
	}

}
