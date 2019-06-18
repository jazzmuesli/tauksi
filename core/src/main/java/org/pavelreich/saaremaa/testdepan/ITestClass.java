package org.pavelreich.saaremaa.testdepan;

import java.util.List;

public interface ITestClass extends IJSONable {

	String getClassName();

	List<ITestMethod> getTestMethods();

	List<ITestField> getMockFields();
	
}