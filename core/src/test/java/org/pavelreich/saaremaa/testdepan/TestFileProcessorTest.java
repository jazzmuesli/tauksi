package org.pavelreich.saaremaa.testdepan;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor.MockOccurence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock; 

public class TestFileProcessorTest {

	private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessorTest.class);
	
	@Mock Socket socket;
	@Mock File file;
	

	@Test
	public void testAssertions2() throws Exception {
		try(FileOutputStream file= new FileOutputStream("/tmp/1.txt")) {
			File f = mock(File.class);
			File f1 = Mockito.mock(File.class);
			File f2 = Mockito.mock(File.class);
		} finally {
			
		}
	}
	
	@Test
	public void testAssertions() throws Exception {
		TestFileProcessor processor = TestFileProcessor.run(LOG,"./src/test/java/org/pavelreich/saaremaa/testdepan/TestFileProcessorTest.java", null);
		LOG.info("processor: " + processor);
		List<ITestClass> classes = processor.getElements();
		assertEquals(1, classes.size());
		Map map = new HashMap();
		ITestClass myClass = classes.get(0);
		File f = Mockito.mock(File.class);
		assertEquals(getClass().getName(), myClass.getClassName());
		List<ITestMethod> methods = myClass.getTestMethods();
		List<ITestField> mockFields = myClass.getMockFields();
		assertEquals(2, mockFields.size());
		assertEquals(4, methods.size());
		Map<String, ITestMethod> methodsMap = methods.stream().collect(Collectors.toMap(x -> x.getName(), x->x));
		ITestMethod methodTestAssertions2 = methodsMap.get("testAssertions2");
		List<ObjectCreationOccurence> testAssertions2Mocks = methodTestAssertions2.getMocks();
		assertEquals(3, testAssertions2Mocks.size());
		for (ITestMethod myMethod : Arrays.asList(methodsMap.get("testAssertions"))) {
			assertEquals(Sets.newHashSet("Test"),myMethod.getAnnotations());
			assertEquals(1, myMethod.getMocks().size());
			List<ObjectCreationOccurence> mocksInMethod = myMethod.getMocks();
			ObjectCreationOccurence mockInMethod = mocksInMethod.iterator().next();
			assertEquals("f",mockInMethod.getName());
//			assertEquals(51, mockInMethod.getLine());
			assertEquals("java.io.File", mockInMethod.getClassName());
			assertEquals(InstanceType.MOCKITO, mockInMethod.getInstanceType());
			assertEquals(2, myMethod.getAssertions().get(0).getArgTypes().size());
			assertEquals("org.junit.Assert", myMethod.getAssertions().get(0).getClassName());
			assertEquals("int", myMethod.getAssertions().get(0).getArgTypes().get(0));
			//assertEquals(20, myMethod.getAssertions().size());
		}
		processor.writeResults("results.json");
		processor.writeCSVResults("asserts.csv");
		assertEquals(2, mockFields.size());
		ITestField mockField = mockFields.get(0);
//		assertEquals(28, mockField.getLine());
		assertEquals("socket", mockField.getName());
		assertEquals("java.net.Socket", mockField.getMockType());
		ObjectCreationOccurence mock1 = methodTestAssertions2.getMocks().get(0);
		assertEquals("java.io.File", mock1.getClassName());
		assertEquals("f", mock1.getName());
//		assertEquals(35, mock1.getLine());
		processor.writeMockito("mockito1.csv");
	}
	
	@Test
	public void testIdentifyMocksInUnresolvedCode() throws FileNotFoundException {
		TestFileProcessor processor = TestFileProcessor.run(LOG,"code-examples", null);
		List<ITestClass> classes = processor.getElements();
		assertEquals(1, classes.size());
		Map<String, ITestClass> classesMap = classes.stream().collect(Collectors.toMap(x->x.getClassName(), x->x));
		ITestClass myClass = classesMap.get("org.apache.storm.daemon.logviewer.utils.WorkerLogsTest");
		List<ITestMethod> methods = myClass.getTestMethods();
		Map<String, ITestMethod> methodsMap = methods.stream().collect(Collectors.toMap(x -> x.getName(), x->x));
		LOG.info("methodsMap: " + methodsMap);
		ITestMethod method = methodsMap.get("testIdentifyWorkerLogDirs");
		LOG.info("json: " + method.toJSON());
		List<ObjectCreationOccurence> mocks = method.getMocks();
		assertEquals(1, mocks.size());
		LOG.info("mock: " + mocks.get(0).toJSON());
		LOG.info("assertions: " + method.getAssertions().get(0).toJSON());
		LOG.info("lines: " + myClass.toCSV());
		assertEquals(0, myClass.getMockFields().size());
		processor.writeMockito("mockito2.csv");
	}
	
	@Ignore
	@Test
	public void testCayenne() throws FileNotFoundException {
		
		TestFileProcessor processor = TestFileProcessor.run(LOG,"/Users/preich/Documents/github/cayenne/cayenne-server/src/test/java", null);
		List<MockOccurence> mocks = processor.getMocks();
		LOG.info("mocks:"+mocks);
		
	}
}
