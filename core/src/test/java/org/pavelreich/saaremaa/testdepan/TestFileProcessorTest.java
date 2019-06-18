package org.pavelreich.saaremaa.testdepan;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.compress.utils.Sets;
import org.junit.Test;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor.MyClass;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor.MyMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileProcessorTest {

	private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessorTest.class);
	
	@Test
	public void testAssertions() throws Exception {
		TestFileProcessor processor = TestFileProcessor.run("./src/test/java", null);
		LOG.info("processor: " + processor);
		List<MyClass> classes = processor.getElements();
		assertEquals(1, classes.size());
		MyClass myClass = classes.get(0);
		assertEquals(getClass().getName(), myClass.getClassName());
		List<MyMethod> methods = myClass.getTestMethods();
		assertEquals(1, methods.size());
		MyMethod myMethod = methods.get(0);
		assertEquals(Sets.newHashSet("Test"),myMethod.annotations);
		assertEquals(7, myMethod.assertions.size());
		assertEquals(2, myMethod.assertions.get(0).argTypes.size());
		assertEquals("int", myMethod.assertions.get(0).argTypes.get(0));
	}
}
