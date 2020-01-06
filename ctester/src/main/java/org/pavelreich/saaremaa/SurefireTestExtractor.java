package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerDecorator;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser;
import org.slf4j.Logger;

public class SurefireTestExtractor implements TestExtractor {

	private Logger logger;

	public SurefireTestExtractor(Logger logger) {
		this.logger = logger;
	}

	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String srcDir) {
		Collection<String> files;
		Map<String, Set<String>> ret = new ConcurrentHashMap();
		try {
			files = getFiles(srcDir);
			List<Map<String, Set<String>>> results = files.parallelStream().map(x -> parseFile(x))
					.collect(Collectors.toList());
//TODO: implement using streams
			// Stream<Entry<String, List<String>>> streams = null;
//			Map<String,List<String>> combined = streams.collect(Collectors.toMap(x->x.getKey(), x->x.getValue(), (x1,x2)->Stream.concat(x1.stream(), x2.stream()).collect(Collectors.toList())));
			for (Map<String, Set<String>> e : results) {
				for (Entry<String, Set<String>> entry : e.entrySet()) {
					Set<String> current = ret.get(entry.getKey());
					if (current == null) {
						current = new HashSet();
						ret.put(entry.getKey(), current);
					}
					current.addAll(entry.getValue());
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return ret;
		}

		return ret;
	}

	private Collection<String> getFiles(String srcDir) throws IOException {
		Path path = java.nio.file.Paths.get(srcDir);
		if (!path.toFile().exists()) {
			logger.info("Path " + srcDir + " doesn't exist");
			return Collections.emptyList();
		}
		List<String> files = java.nio.file.Files.walk(path).filter(p -> p.toFile().getName().matches("TEST-.*?xml"))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
		return files;
	}

	private Map<String, Set<String>> parseFile(String fname) {
		try {
			ConsoleLogger consoleLogger = new ConsoleLoggerDecorator(logger);
			TestSuiteXmlParser parser = new TestSuiteXmlParser(consoleLogger);
			List<ReportTestSuite> testSuites = parser.parse(fname);
//			System.out.println("reports: " + testSuites);
			List<ReportTestCase> ret = testSuites.stream().map(x -> x.getTestCases()).flatMap(List::stream)
					.collect(Collectors.toList());
//			ret.forEach(x->LOG.info("testClassName:"+ x.getClassName() +", testMethodName:" + x.getName()));
			Map<String, Set<String>> result = ret.stream().collect(Collectors.groupingBy(
					ReportTestCase::getFullClassName, Collectors.mapping(ReportTestCase::getName, Collectors.toSet())));
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}
}
