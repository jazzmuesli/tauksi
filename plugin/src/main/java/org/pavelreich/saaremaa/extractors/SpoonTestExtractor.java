package org.pavelreich.saaremaa.extractors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;
import org.slf4j.Logger;

public class SpoonTestExtractor implements TestExtractor {

	private Logger logger;
	private String testClassName = null;
	
	public SpoonTestExtractor(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String dirName) {
		TestFileProcessor processor;
		try {
			processor = TestFileProcessor.run(logger, dirName, null);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}

		// extract junit class names
		List<ITestClass> testClasses = processor.getElements();
		if (testClassName != null && !testClassName.trim().isEmpty()) {
			testClasses = testClasses.stream().filter(p->p.getClassName().contains(testClassName)).collect(Collectors.toList());
		}
		Map<String,Set<String>> testClassToMethods = new HashMap<String, Set<String>>();
		for (ITestClass element : testClasses) {
			Set<String> testMethods = element.getTestMethods().stream().map(x->x.getName()).collect(Collectors.toSet());
			testClassToMethods.put(element.getClassName(), testMethods);
		}
		return testClassToMethods;
	}


}
