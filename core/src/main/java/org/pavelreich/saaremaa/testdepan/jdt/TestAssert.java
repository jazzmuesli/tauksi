package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.pavelreich.saaremaa.testdepan.ITestAssert;

public class TestAssert implements ITestAssert{

	private String methodName;
	List<String> argTypes = new ArrayList();
	int line = -1;

	public TestAssert(String methodName) {
		this.methodName = methodName;
	}
	@Override
	public Map<String, Object> toJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethodName() {
		// TODO Auto-generated method stub
		return methodName;
	}

	@Override
	public List<String> getArgTypes() {
		// TODO Auto-generated method stub
		return argTypes;
	}

	@Override
	public int getLine() {
		// TODO Auto-generated method stub
		return line;
	}

}
