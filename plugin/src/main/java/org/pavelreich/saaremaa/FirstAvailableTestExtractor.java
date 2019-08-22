package org.pavelreich.saaremaa;

import java.util.Map;
import java.util.Set;

public class FirstAvailableTestExtractor implements TestExtractor {

	private TestExtractor extractor1;
	private TestExtractor extractor2;

	public FirstAvailableTestExtractor(TestExtractor extractor1, TestExtractor extractor2) {
		this.extractor1 = extractor1;
		this.extractor2 = extractor2;
	}

	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String path) {
		Map<String, Set<String>> results = extractor2.extractTestCasesByClass(path);
		if (!results.isEmpty()) {
			return results;
		}
		return extractor1.extractTestCasesByClass(path);
	}

}
