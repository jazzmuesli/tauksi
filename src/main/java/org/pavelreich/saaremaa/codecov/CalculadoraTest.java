package org.pavelreich.saaremaa.codecov;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
public class CalculadoraTest extends TestCase
{
    private Calculadora c1;

    @BeforeClass
    public void setUp() { c1 = new Calculadora(); }

    @AfterClass
    public void tearDown() { c1 = null; }

    @Test
    public void testAdd() { assertTrue(c1.add(1, 0) == 1); }
    
    @Test
    public void testSubtract() { assertEquals(c1.minus(3, 2), 1); }
}