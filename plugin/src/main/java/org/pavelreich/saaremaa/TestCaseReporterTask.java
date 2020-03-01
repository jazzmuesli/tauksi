package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.pavelreich.saaremaa.extractors.SpoonTestExtractor;
import org.pavelreich.saaremaa.extractors.SurefireTestExtractor;
import org.slf4j.Logger;

import com.github.mauricioaniche.ck.plugin.CKMetricsMojo;

public class TestCaseReporterTask {
	private Logger logger;
	private Collection<String> testSrcDirs;

	public TestCaseReporterTask(Logger logger, Collection<String> testSrcDirs) {
		this.logger = logger;
		this.testSrcDirs = testSrcDirs;
	}

	public void execute() {
		try {
			CombinedTestExtractor extractor = new CombinedTestExtractor(new SurefireTestExtractor(logger),
					new SpoonTestExtractor(logger));
			for (String dirName : Helper.extractDirs(testSrcDirs)) {
				logger.info("Processing dir=" + dirName);
				if (!new File(dirName).exists()) {
					continue;
				}

				Map<String, Set<String>> testCasesMap = extractor.extractTestCasesByClass(dirName);
				CSVReporter reporter;
				reporter = new CSVReporter(dirName + File.separator + "testcases.csv", "testClassName", "testCaseName",
						"i");
				for (Entry<String, Set<String>> entry : testCasesMap.entrySet()) {
					int i = 0;
					for (String testCase : entry.getValue()) {
						reporter.write(entry.getKey(), testCase, i++);
					}
					reporter.write(entry.getKey(), "TOTAL", i);
				}
				reporter.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalArgumentException(e.getMessage(), e);
		}

	}
}
