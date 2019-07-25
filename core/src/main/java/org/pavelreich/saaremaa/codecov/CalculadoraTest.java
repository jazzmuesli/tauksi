package org.pavelreich.saaremaa.codecov;

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CalculadoraTest {
	private Calculadora c1;

	@Before
	public void setUp() {
		c1 = new Calculadora();
		System.out.println(c1.toString());
	}

	@After
	public void tearDown() {
		c1 = null;
	}
	@Test
	public void testAsLis() {
		Assert.assertTrue(c1.asList(4).equals(Arrays.asList(4)));
	}

	@Test
	public void testAdd() {
		Assert.assertTrue(c1.add(1, 0) == 1);
	}

	@Test
	public void testAdd2() {
		Assert.assertTrue(c1.add(1, 3) == 4);
	}

	@Test
	public void testSubtract() {
		Assert.assertEquals(1, c1.minus(3, 2).intValue());
	}
}