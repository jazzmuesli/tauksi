package org.pavelreich.zoo;

import org.junit.Assert;
import org.junit.Test;
import org.pavelreich.zoo.User.Gender;

public class CalculatorTest {

	@Test
	public void testAdd() {
		Calculator calculator = new Calculator();
		Assert.assertEquals(4, calculator.add(2, 2));
	}
	
	@Test
	public void testUserAge() {
		Calculator calculator = new Calculator();
		User user = new User();
		user.setGender(Gender.MALE);
		user.setBirthYear(2001);
		user.setFirstName("Joe");
		Assert.assertEquals("Mr", user.getTitle());
		Assert.assertEquals(18, calculator.getAge(user));
		Assert.assertEquals("Joe", user.getFirstName());
		Assert.assertEquals("Mr Joe", user.getSalutation());
	}
}
