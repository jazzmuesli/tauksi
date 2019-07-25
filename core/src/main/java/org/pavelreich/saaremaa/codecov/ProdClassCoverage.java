package org.pavelreich.saaremaa.codecov;

import java.util.*;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;

public class ProdClassCoverage {

	static final String DELIM = "|";
	String className;
	Map<String, ICounter> lineCoverageByMethod;

	ProdClassCoverage(String name, Map<String, ICounter> lineCoverageByMethod) {
		this.className = name;
		this.lineCoverageByMethod = lineCoverageByMethod;
	}

	public List<String> asCSV() {
		return lineCoverageByMethod.entrySet().stream().map(x -> className + DELIM + x.getKey() + DELIM
				+ x.getValue().getMissedCount() + DELIM + x.getValue().getCoveredCount()).collect(Collectors.toList());

	}

	private static String getMethodName(IMethodCoverage m) {
		return m.getName() + m.getDesc();
	}

	public static ProdClassCoverage createProdCoverage(IClassCoverage coverage) {
		ProdClassCoverage cc = new ProdClassCoverage(coverage.getName().replaceAll("/", "."), coverage
				.getMethods().stream().collect(Collectors.
						toMap(m -> getMethodName(m), 
								m -> m.getLineCounter(),
								(a,b) -> {
									return a;
								})));
		return cc;
	}

	
	@Override
	public String toString() {
		return asCSV().stream().collect(Collectors.joining("\n"));
	}
}