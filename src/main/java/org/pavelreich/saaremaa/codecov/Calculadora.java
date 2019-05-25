package org.pavelreich.saaremaa.codecov;

import java.util.Arrays;
import java.util.List;

public class Calculadora {
	public Calculadora() {
	}

	public int add(int x, final int y) {
		return x + y;
	}

	public Number minus(int x, int y) {
		int ret = x - y;
		try {
			return new CalculationResult(ret, x, y);
			//return ret;
		} catch (Throwable e) {
			e.printStackTrace();
			return ret;
		}
	}
	
	public List<Number> asList(int x) {
		return Arrays.asList(x);
	}

}