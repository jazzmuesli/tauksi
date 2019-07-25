package org.pavelreich.saaremaa.codecov;

import java.util.Map;

import org.jacoco.core.analysis.IClassCoverage;
import org.junit.runner.Result;

public class TestExecutionResults {
	public Map<String, IClassCoverage> coverageByProdClass;
	public Result result;

	public TestExecutionResults(Map<String, IClassCoverage> map, Result result) {
		this.coverageByProdClass = map;
		this.result = result;
	}

}