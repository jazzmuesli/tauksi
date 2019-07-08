package org.pavelreich.saaremaa;

public class ExampleClass {

	public static void main(String[] args) {
		int i = 0;
		i = add10(i);
		System.out.println(i);
	}

	private static int add10(int i) {
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		i += 1;
		return i;
	}
}
