package org.pavelreich.saaremaa;

import org.bson.Document;

public class TestExecutionCommand {
	String testClassName;
	String testMethodName;

	public static TestExecutionCommand forTestClass(String className) {
		TestExecutionCommand cmd = new TestExecutionCommand();
		cmd.testClassName = className;
		return cmd;
	}
	
	public static TestExecutionCommand forTestClassMethod(String className, String methodName) {
		TestExecutionCommand cmd = forTestClass(className);
		cmd.testMethodName = methodName;
		return cmd;
	}

	public Document asDocument() {
		Document doc = new Document();
		doc = doc.append("testClassName", testClassName).append("testMethodName", testMethodName);
		return doc;
	}
	
	public String getTestClassName() {
		return testClassName;
	}
	
	
	
	@Override
	public String toString() {
		return testClassName + ((testMethodName != null) ? (":" + testMethodName) : "");
	}

}