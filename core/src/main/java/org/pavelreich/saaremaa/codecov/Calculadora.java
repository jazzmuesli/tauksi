package org.pavelreich.saaremaa.codecov;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pavelreich.saaremaa.BuildProjects;

import com.google.common.annotations.VisibleForTesting;

public class Calculadora {
	static interface IntOperator {
		int add(int x, int y);

		int minus(int x, int y);
	}

	static class DefaultIntOperator implements IntOperator {

		@Override
		public int add(int x, int y) {
			return x + y;
		}

		@Override
		public int minus(int x, int y) {
			return x - y;
		}

	}

	private IntOperator operator;

	@VisibleForTesting
	public Calculadora(IntOperator operator) {
		this.operator = operator;
	}

	public Calculadora() {
		this(new DefaultIntOperator());
	}

	public int add(int x, final int y) {
		return operator.add(x, y);
	}

	public Number minus(int x, int y) {
		int ret = operator.minus(x, y);
		try {
			return new CalculationResult(ret, x, y);
			// return ret;
		} catch (Throwable e) {
			e.printStackTrace();
			return ret;
		}
	}

	public List<Number> asList(int x) {
		return Arrays.asList(x);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString("This is class " + BuildProjects.class.getName());
	}

}