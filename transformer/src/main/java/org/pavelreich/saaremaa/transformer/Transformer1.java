package org.pavelreich.saaremaa.transformer;

public class Transformer1 {

	public int add(int i, int j) {
		if (i < 0) {
			return i + j;
		}
		if (i > 30) {
			return i + j;
		}
		return i + j + 0;
	}

	public double divide(double a, double b) {
		if (b != 0) {
			return a / b;
		}
		return 0.0;
	}
}
