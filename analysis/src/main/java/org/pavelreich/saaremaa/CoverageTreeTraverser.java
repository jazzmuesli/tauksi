package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverageTreeTraverser {

	static class TestPair {
		String testClassName, prodClassName;

		public TestPair(String testClassName, String prodClassName) {
			super();
			this.testClassName = testClassName;
			this.prodClassName = prodClassName;
		}
		
	}
	static class CoverageSummary {
		String testClassName;
		Long prodClassesCovered;
		int level;
		private String parentTest;
		String rootProject;
		public CoverageSummary(String testClassName, String parentTest, Long prodClassesCovered, int level, String rootProject) {
			this.testClassName = testClassName;
			this.parentTest = parentTest;
			this.prodClassesCovered = prodClassesCovered;
			this.level = level;
			this.rootProject = rootProject;
		}
		
	}
	private static final Logger LOG = LoggerFactory.getLogger(CoverageTreeTraverser.class);
	private List<CSVRecord> records;
	public CoverageTreeTraverser(List<CSVRecord> records) {
		this.records = records;
	}
	Map<String, List<TestPair>> prodMap ;
	private Map<String, Long> prodClassesByTest;
	private List<TestPair> pairs;
	public static void main(String[] args) throws IOException {
		CSVParser parser = CSVParser.parse(new File("/tmp/pccm.csv"), Charset.defaultCharset(), CSVFormat.DEFAULT.withFirstRecordAsHeader());
		List<CSVRecord> records = parser.getRecords();
		LOG.info("records: " +records.size());
		CoverageTreeTraverser traverser = new CoverageTreeTraverser(records);
		Set<String> projects = records.stream().map(x->x.get("rootproject")).collect(Collectors.toSet());
		for (String project : projects) {
			traverser.run(project);			
		}
		
		traverser.finish();

	}
	private void finish() {
		LOG.info("ret: " + ret.size());
		CSVReporter reporter;
		try {
			reporter = new CSVReporter("/tmp/tree.csv","parentTest","testClassName","level","prodClassesCovered","rootproject");
			for (CoverageSummary summary : ret) {
				reporter.write(summary.parentTest, summary.testClassName, summary.level, summary.prodClassesCovered,summary.rootProject);	
			}
			reporter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	List<CoverageSummary> ret = new ArrayList();
	private String project;
	private void run(String project) {
		this.project = project;
		this.pairs = records.stream().filter(p->project.equals(p.get("rootproject"))).map(x->new TestPair(x.get("testClassName"),x.get("prodClassName"))).collect(Collectors.toList());
		LOG.info("pairs: " +pairs.size());
		this.prodClassesByTest = pairs.stream().map(x->x.testClassName).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		this.prodMap = pairs.stream().collect(Collectors.groupingBy(e -> e.testClassName));
		LOG.info("m: " +prodClassesByTest.size());

		Set<String> visitedTests = new HashSet();
		Map<String, Long> sorted = prodClassesByTest
		        .entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .collect(
		            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
		                LinkedHashMap::new));
		int i=0;
		LOG.info("sorted: " + sorted.size());
		for (String testClassName : sorted.keySet()) {
			if (i++==0) {
				LOG.info("TCN: " + testClassName);
				traverseTree(testClassName, "root", ret, visitedTests, prodClassesByTest, 0);
//				break;
				
			}
		}
		
		
	}
	private void traverseTree(String testClassName, String parentTest, List<CoverageSummary> ret, Set<String> visitedTests, Map<String, Long> prodClassesByTest, int level) {
		String key = parentTest+":"+testClassName;
		key=testClassName;
		if (visitedTests.contains(key)) {
			return;
		}
		Long prodCovered = prodClassesByTest.getOrDefault(testClassName, 0L);
		visitedTests.add(key);
//		LOG.info("testClassName: " + testClassName + ", level: " + level + ", visited: " + visitedTests.size());
		if (prodCovered <= 0) {
			return;
		}
		CoverageSummary summary = new CoverageSummary(testClassName, parentTest, prodCovered, level,project);
		ret.add(summary);
		Set<String> testClasses = prodMap.getOrDefault(testClassName, Collections.emptyList()).stream().map(x->x.prodClassName+"Test").collect(Collectors.toSet());
		for (String tcn : testClasses) {
			traverseTree(tcn, testClassName, ret, visitedTests, prodClassesByTest, level+1);
		}
	}
}
