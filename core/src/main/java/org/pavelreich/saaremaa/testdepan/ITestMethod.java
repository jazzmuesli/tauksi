package org.pavelreich.saaremaa.testdepan;

import java.util.List;
import java.util.Set;

public interface ITestMethod extends IJSONable {

	boolean isTest();

	boolean isSetup();

	Set<String> getAnnotations();

	List<ObjectCreationOccurence> getMocks();

	List<ITestAssert> getAssertions();

	String getName();
	
	String toCSV();

}
