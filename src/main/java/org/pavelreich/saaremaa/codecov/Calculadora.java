package org.pavelreich.saaremaa.codecov;
public class Calculadora
{
    public Calculadora() { }

    public int add(int x, final int y) {
        return x + y;
    }
    public Number minus(int x, int y) {
    	return new CalculationResult(x-y, x, y);
    }
    
}