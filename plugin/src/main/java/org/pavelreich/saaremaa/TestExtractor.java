package org.pavelreich.saaremaa;

import java.util.Map;
import java.util.Set;

public interface TestExtractor {

	Map<String,Set<String>> extractTestCasesByClass(String path);
}
