package org.pavelreich.saaremaa.extractors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SingleClassExtractor implements TestExtractor {
	private final String testClassName;

	public SingleClassExtractor(String testClassName) {
		this.testClassName = testClassName;
	}

	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String path) {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		map.put(testClassName, Collections.emptySet());
		return map;
	}
}
