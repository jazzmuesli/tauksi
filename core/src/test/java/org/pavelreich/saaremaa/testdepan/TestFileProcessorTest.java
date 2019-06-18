package org.pavelreich.saaremaa.testdepan;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.commons.compress.utils.Sets;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileProcessorTest {

	private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessorTest.class);
	
	@Mock File file;
	
	@Test
	public void testAssertions() throws Exception {
		TestFileProcessor processor = TestFileProcessor.run("./src/test/java", null);
		LOG.info("processor: " + processor);
		List<ITestClass> classes = processor.getElements();
		assertEquals(1, classes.size());
		ITestClass myClass = classes.get(0);
		File f = Mockito.mock(File.class);
		assertEquals(getClass().getName(), myClass.getClassName());
		List<ITestMethod> methods = myClass.getTestMethods();
		assertEquals(1, myClass.getMockFields().size());
		assertEquals(1, methods.size());
		for (ITestMethod myMethod : methods) {
			assertEquals(Sets.newHashSet("Test"),myMethod.getAnnotations());
			assertEquals(1, myMethod.getMocks().size());
			assertEquals(2, myMethod.getAssertions().get(0).getArgTypes().size());
			assertEquals("org.junit.Assert", myMethod.getAssertions().get(0).getClassName());
			assertEquals("int", myMethod.getAssertions().get(0).getArgTypes().get(0));
			assertEquals(14, myMethod.getAssertions().size());
		}
	}
}
