package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.Map;

import org.pavelreich.saaremaa.testdepan.ITestField;

public class TestField implements ITestField {

	private String type;

	public TestField(String type) {

		this.type = type;
	}
	@Override
	public Map<String, Object> toJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMockType() {
		return type;
	}
	@Override
	public int getLine() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
