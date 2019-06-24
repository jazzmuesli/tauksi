package org.pavelreich.saaremaa.testdepan;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface ITestClass extends IJSONable {

	String getClassName();

	List<ITestMethod> getTestMethods();

	List<ITestField> getMockFields();

	List<String> toCSV();

	static String getFields() {
		return Arrays.asList("testClassName",
				"testMethodName",
				"asserts",
				"mocksInMethod",
				"mocksInClass",
				"methodStartLine",
				"assertsStartLine",
				"mocksStartLine",
				"lineCount").stream().collect(Collectors.joining(TestFileProcessor.DELIM));
	}
	
}