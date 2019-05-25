package org.pavelreich.saaremaa.codecov;

public class CalculationResult extends Number {

	private Number result;
	private Number[] args;

	CalculationResult(Number result, Number ... args) {
		this.result = result;
		this.args = args;
	}

	@Override
	public int intValue() {
		return result.intValue();
	}

	@Override
	public long longValue() {
		return result.longValue();
	}

	@Override
	public float floatValue() {
		return result.floatValue();
	}

	@Override
	public double doubleValue() {
		return result.doubleValue();
	}
}
