package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

public class HelperTest {

	@Test
	public void testCasePrefix() {
		String tcn = "org.apache.commons.io.IOUtilsCopyTestCase";
		String s = Helper.getProdClassName(tcn);
		assertEquals("org.apache.commons.io.IOUtilsCopy", s);
		assertTrue(Helper.isTest(tcn));
	}

	@Test
	public void testPrefix() {
		String tcn = "org.joda.time.tz.TestFixedDateTimeZone";
		String s = Helper.getProdClassName(tcn);
		assertEquals("org.joda.time.tz.FixedDateTimeZone", s);
		assertTrue(Helper.isTest(tcn));
	}

	@Test
	public void testSuffix() {
		String tcn = "org.joda.time.tz.FixedDateTimeZone_ESTest";
		String s = Helper.getProdClassName(tcn);
		assertEquals("org.joda.time.tz.FixedDateTimeZone", s);
		assertTrue(Helper.isTest(tcn));
	}

	@Test
	public void testMethod() {
		assertEquals("name", Helper.extractName("(set)", "setName/1").get());
	}

	@Test
	public void testCat() {
		assertEquals("evo", Helper.classifyTest("com.google.earth.kml._2.PointTypeEvoSuiteTest"));
	}

	
	@Test
	public void testMethods() {
		Set<String> fields = new HashSet(Arrays.asList("name","age"));
		List<String> methods = Arrays.asList("setName/1","setAge/1","getAge/0","hashCode/0","calculateX");
		List<Optional<String>> setters = Helper.getMethods(methods, "^(set).*", "(set)");
		List<Optional<String>> getters = Helper.getMethods(methods, "^(get|is).*", "(get|is)");
		Collection<String> dataMethods = Helper.calculateDataMethods(fields, methods, setters, getters);
		long dataMethodsRatio = 0;
		if (!methods.isEmpty() && !dataMethods.isEmpty()) {
			dataMethodsRatio = Math.round(100*Double.valueOf(dataMethods.size()/Double.valueOf(methods.size())));	
		}
		assertEquals(80, dataMethodsRatio);

		assertTrue("dataMethods: " + dataMethods, dataMethods.contains("setName/1"));
		assertTrue(dataMethods.contains("getAge/0"));
	}
}
