package org.pavelreich.saaremaa;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface TestExtractor {

	Map<String,Set<String>> extractTestCasesByClass(String path);
}
