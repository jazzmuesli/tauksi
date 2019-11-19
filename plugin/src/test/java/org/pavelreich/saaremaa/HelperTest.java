package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HelperTest {
	
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

}
