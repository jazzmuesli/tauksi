package org.pavelreich.saaremaa.testdepan;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileProcessorTest {

	private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessorTest.class);
	
	@Mock Socket socket;
	@Mock File file;
	

	@Test
	public void testAssertions2() throws Exception {
		File f = Mockito.mock(File.class);
		File f1 = Mockito.mock(File.class);
		File f2 = Mockito.mock(File.class);
	}
	
	@Test
	public void testAssertions() throws Exception {
		TestFileProcessor processor = TestFileProcessor.run("./src/test/java/org/pavelreich/saaremaa/testdepan/TestFileProcessorTest.java", null);
		LOG.info("processor: " + processor);
		List<ITestClass> classes = processor.getElements();
		assertEquals(1, classes.size());
		Map map = new HashMap();
		ITestClass myClass = classes.get(0);
//		LOG.info("line: " + myClass.toCSV());
		File f = Mockito.mock(File.class);
		assertEquals(getClass().getName(), myClass.getClassName());
		List<ITestMethod> methods = myClass.getTestMethods();
		assertEquals(2, myClass.getMockFields().size());
		assertEquals(2, methods.size());
		for (ITestMethod myMethod : methods.stream().filter(p->p.getName().equals("testAssertions")).collect(Collectors.toList())) {
			assertEquals(Sets.newHashSet("Test"),myMethod.getAnnotations());
			assertEquals(1, myMethod.getMocks().size());
			List<ObjectCreationOccurence> mocksInMethod = myMethod.getMocks();
			ObjectCreationOccurence mockInMethod = mocksInMethod.iterator().next();
			assertEquals("f",mockInMethod.getName());
			assertEquals(43, mockInMethod.getLine());
			assertEquals("java.io.File", mockInMethod.getClassName());
			assertEquals(InstanceType.MOCKITO, mockInMethod.getInstanceType());
			assertEquals(2, myMethod.getAssertions().get(0).getArgTypes().size());
			assertEquals("org.junit.Assert", myMethod.getAssertions().get(0).getClassName());
			assertEquals("int", myMethod.getAssertions().get(0).getArgTypes().get(0));
			assertEquals(18, myMethod.getAssertions().size());
		}
		processor.writeResults("results.json", processor);
		processor.writeCSVResults("asserts.csv");
	}
}
