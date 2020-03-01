package org.pavelreich.saaremaa;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;
import org.slf4j.Logger;

public class AnalyseTask {

	private Logger logger;
	private Collection<String> testCompileSourceRoots;

	public AnalyseTask(Logger logger, Collection<String> testCompileSourceRoots) {
		this.logger = logger;
		this.testCompileSourceRoots = testCompileSourceRoots;
	}

	public void execute() {
		for (String dirName : Helper.extractDirs(testCompileSourceRoots)) {
			try {
				// process test directory
				logger.info("Processing " + dirName);
				if (new File(dirName).exists()) {
					String resultFileName = dirName + File.separator + "result.json";
					String assertsFileName = dirName + File.separator + "asserts.csv";
					String mockitoFileName = dirName + File.separator + "mockito.csv";
					TestFileProcessor processor = TestFileProcessor.run(logger, dirName, resultFileName);
					List<ITestClass> classes = processor.getElements();
					logger.info("Analysed " + classes.size() + " classes with " + processor.getMocks().size()
							+ " mocks, creating files: " + assertsFileName + ", " + resultFileName + ", "
							+ mockitoFileName);
					processor.writeCSVResults(assertsFileName);
					processor.writeMockito(mockitoFileName);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

}
