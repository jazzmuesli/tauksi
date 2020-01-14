package org.pavelreich.saaremaa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.pavelreich.saaremaa.extractors.SpoonTestExtractor;
import org.pavelreich.saaremaa.extractors.SurefireTestExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class TestSuiteParserTest {
	private static final Logger LOG = LoggerFactory.getLogger(TestSuiteParserTest.class);

	@Test
	public void testUsingSpoon() {
		SpoonTestExtractor parser = new SpoonTestExtractor(LOG);
		Map<String, Set<String>> results = parser.extractTestCasesByClass("/Users/preich/Documents/github/jfreechart/src/test/java");
		int testClassMethods = results.values().stream().mapToInt(x -> x.size()).sum();
		LOG.info("testClassNames: " + results.size());
		LOG.info("testClassMethods: " + testClassMethods);
		Assert.assertEquals(343, results.size());
		Assert.assertEquals(2180, testClassMethods);
	}

	@Test
	public void testUsingSurefire() {
		String path = "/Users/preich/Documents/github/jfreechart/target/surefire-reports";
		SurefireTestExtractor parser = new SurefireTestExtractor(LOG);
		Map<String, Set<String>> results = parser.extractTestCasesByClass(path);
		int testClassMethods = results.values().stream().mapToInt(x -> x.size()).sum();
		LOG.info("testClassNames: " + results.size());
		LOG.info("testClassMethods: " + testClassMethods);
		Assert.assertEquals(341, results.size());
		Assert.assertEquals(2176, testClassMethods);
	}

	
	@Test
	public void testCombineMaps() {
		Map<String, List<String>>a = new HashMap();
		a.put("a", Arrays.asList("a1","a2"));
		
		Map<String, List<String>>b = new HashMap();
		b.put("a", Arrays.asList("a4","a4"));
		b.put("b", Arrays.asList("b1","b2"));
		Stream<Entry<String, List<String>>> streams = Stream.concat(a.entrySet().stream(), b.entrySet().stream());
		Map<String,List<String>> combined = streams.collect(Collectors.toMap(x->x.getKey(), x->x.getValue(), (x1,x2)->Stream.concat(x1.stream(), x2.stream()).collect(Collectors.toList())));
		Assert.assertEquals(2, combined.size());
		Assert.assertEquals(4, combined.get("a").size());
	}
}
