package org.pavelreich.saaremaa.testdepan;

import java.util.List;

public interface ITestAssert extends IJSONable {
	public String getClassName();

	public String getMethodName();

	public List<String> getArgTypes();

	public int getLine();

}
