package org.pavelreich.saaremaa;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CombinedTestExtractor implements TestExtractor {

	private TestExtractor extractor1;
	private TestExtractor extractor2;

	public CombinedTestExtractor(TestExtractor extractor1, TestExtractor extractor2) {
		this.extractor1=  extractor1;
		this.extractor2 = extractor2;
	}

	@Override
	public Map<String, Set<String>> extractTestCasesByClass(String path) {
		Stream<Entry<String, Set<String>>> streams = Stream.concat(extractor1.extractTestCasesByClass(path).entrySet().stream(), 
				extractor2.extractTestCasesByClass(path).entrySet().stream());

		
		Map<String,Set<String>> combined = streams.collect(Collectors.toMap(x->x.getKey(), x->x.getValue(), 
				(x1,x2)->Stream.concat(x1.stream(), x2.stream()).collect(Collectors.toSet())));

		return combined;
	}

}
