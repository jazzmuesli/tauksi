package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HelperTest {
	
	@Test
	public void testPrefix() {
		String s = Helper.getProdClassName("org.joda.time.tz.TestFixedDateTimeZone");
		assertEquals("org.joda.time.tz.FixedDateTimeZone", s);
	}

	@Test
	public void testSuffix() {
		String s = Helper.getProdClassName("org.joda.time.tz.FixedDateTimeZone_ESTest");
		assertEquals("org.joda.time.tz.FixedDateTimeZone", s);
	}

}
