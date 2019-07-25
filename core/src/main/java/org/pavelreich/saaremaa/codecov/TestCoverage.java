package org.pavelreich.saaremaa.codecov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.runner.Result;
import org.pavelreich.saaremaa.CSVReporter;


public class TestCoverage {

	static final String DELIM = "|";
	String testClassName;
	String testMethod;
	Map<String, ProdClassCoverage> prodClassCoverage;
	Result result;

	public TestCoverage(String testClassName, String testMethod, Collection<ProdClassCoverage> prodClassCoverage,
			Result result) {
		this.testClassName = testClassName;
		this.testMethod = testMethod;
		this.prodClassCoverage = prodClassCoverage.stream().collect(Collectors.toMap(e -> e.className, e -> e));
		this.result = result;
	}

	public static CSVReporter createReporter(String jc) throws IOException {
		String fname = "coverageByMethod" + jc + ".csv";
		List<String> fields = Arrays.asList(
				"testClassName","testMethod","testsFailed","testsIgnored","testsCount","testRunTime","prodClassName","prodMethod","missedLines","coveredLines"
				);
		CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(fname)),
				CSVFormat.DEFAULT.withHeader(fields.toArray(new String[0])).withDelimiter('|'));
		CSVReporter reporter = new CSVReporter(printer);
		return reporter;
	}


	public List<String> asCSV() {
		List<String> ret = new ArrayList<String>();
		for (ProdClassCoverage pcc : prodClassCoverage.values()) {
			List<String> x = pcc.asCSV().stream()
					.map(s -> testClassName + DELIM + testMethod + DELIM + result.getFailureCount() + DELIM
							+ result.getIgnoreCount() + DELIM + result.getRunCount() + DELIM + result.getRunTime()
							+ DELIM + s)
					.collect(Collectors.toList());
			ret.addAll(x);
		}
		return ret;
	}

	public static void reportCoverages(Collection<TestCoverage> testCoverages, CSVReporter reporter) {
		for (TestCoverage testCov : testCoverages) {
			testCov.asCSV().stream().forEach(x -> reporter.write(x.split("\\" + DELIM)));
		}
	}
}