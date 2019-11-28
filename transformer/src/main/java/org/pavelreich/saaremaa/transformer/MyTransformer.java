package org.pavelreich.saaremaa.transformer;

import java.io.PrintWriter;

public class MyTransformer {

	public void print(PrintWriter writer) {
		writer.println("banana");
	}

	public String transform(String x) {
		Calculator calculator = new Calculator();
		return calculator.doubleString(x);
	}
	
	public String getName(User u) {
		return u.toString();
	}
	
	public String uncoveredMethod(String a) {
		return a+"uncovered";
	}
	
	static class User {
		String name;

		public User(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}

}
