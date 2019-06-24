package org.pavelreich.saaremaa.testdepan.jdt;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.commons.compress.utils.Sets;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.ITestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDTestFileProcessorTest {

	private static final Logger LOG = LoggerFactory.getLogger(JDTestFileProcessorTest.class);

	@Mock
	File file;

	@Test
	public void testAssertions() throws Exception {
		List<ITestClass> classes = JDTTestFileProcessor
				.analyse("./src/test/java/org/pavelreich/saaremaa/testdepan/jdt/JDTestFileProcessorTest.java");
		assertEquals(1, classes.size());
		ITestClass myClass = classes.get(0);
		File f = Mockito.mock(File.class);
		assertEquals(getClass().getName(), myClass.getClassName());
		List<ITestMethod> methods = myClass.getTestMethods();
		assertEquals(1, myClass.getMockFields().size());
		assertEquals(1, methods.size());
		for (ITestMethod myMethod : methods) {
			assertEquals(Sets.newHashSet("Test"), myMethod.getAnnotations());
			//TODO: assertEquals(1, myMethod.getMocks().size());
			assertEquals(2, myMethod.getAssertions().get(0).getArgTypes().size());
			//TODO: assertEquals("org.junit.Assert", myMethod.getAssertions().get(0).getClassName());
			assertEquals(29, myMethod.getAssertions().get(0).getLine());

			assertEquals("int", myMethod.getAssertions().get(0).getArgTypes().get(0));
			assertEquals(9, myMethod.getAssertions().size());
		}
	}
}
