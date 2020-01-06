package org.pavelreich.saaremaa.transformer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Transformer1Test {
	Transformer1 transformer1 = new Transformer1();

	@Test
	public void testAdd() {
		int result = transformer1.add(3, 4);
		assertEquals(7, result);
	}

	@Test
	public void testDivide() {
		double result = transformer1.divide(3, 4);
		assertEquals(0.75, result, 1e-6);
	}

}
