package org.pavelreich.zoo;

import java.util.Calendar;

/**
 * example of a state-less class 
 * @author preich
 *
 */
public class Calculator {

	public int add(int a, int b) {
		return a + b;
	}

	public int multiply(int a, int b) {
		return a * b;
	}
	
	public int getAge(User user) {
		Calendar cal = Calendar.getInstance();
		int currentYear = cal.get(Calendar.YEAR);
		return currentYear - user.getBirthYear();
	}
}
