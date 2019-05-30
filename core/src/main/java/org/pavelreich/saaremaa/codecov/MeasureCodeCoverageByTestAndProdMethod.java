package org.pavelreich.saaremaa.codecov;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.pavelreich.saaremaa.BuildProjects;
import org.pavelreich.saaremaa.CSVReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * from
 * https://stackoverflow.com/questions/40797698/record-test-coverage-per-test-case-using-eclemma-tool?rq=1
 * 
 * @author preich
 *
 */
public class MeasureCodeCoverageByTestAndProdMethod {
	/**
	 * A class loader that loads classes from in-memory data.
	 */
	public static class MemoryClassLoader extends ClassLoader {
		private final Map<String, byte[]> definitions = new HashMap<String, byte[]>();

		/**
		 * Add a in-memory representation of a class.
		 * 
		 * @param name  name of the class
		 * @param bytes class definition
		 */
		public void addDefinition(final String name, final byte[] bytes) {
			definitions.put(name, bytes);
		}

		@Override
		protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
			final byte[] bytes = definitions.get(name);
			if (bytes != null)
				return defineClass(name, bytes, 0, bytes.length);
			return super.loadClass(name, resolve);
		}

		public Class findClass(String name) throws ClassNotFoundException {
			return super.loadClass(name, true);
		}
	}

	private InputStream getTargetClass(final String name) {
		final String resource = '/' + name.replace('.', '/') + ".class";
		return getClass().getResourceAsStream(resource);
	}

	private void printCounter(final String unit, final ICounter counter) {
		final Integer missed = Integer.valueOf(counter.getMissedCount());
		final Integer total = Integer.valueOf(counter.getTotalCount());

		System.out.printf("%s of %s %s missed%n", missed, total, unit);
	}

	private String getColor(final int status) {
		switch (status) {
		case ICounter.NOT_COVERED:
			return "red";
		case ICounter.PARTLY_COVERED:
			return "yellow";
		case ICounter.FULLY_COVERED:
			return "green";
		}
		return "";
	}

	static final Logger LOG = LoggerFactory.getLogger(MeasureCodeCoverageByTestAndProdMethod.class);

	private List<TestCoverage> measureTestCoverage(Class clazz) throws Exception {
		String junitName = clazz.getName();
		Collection<Class> loadedClasses = getDependentClasses(junitName);
		List<String> methodNames = getTestMethods(clazz);

//    	final String targetName = Calculadora.class.getName();
		// For instrumentation and runtime we need a IRuntime instance to collect
		// execution data:
		// The Instrumenter creates a modified version of our test target class that
		// contains additional probes for execution data recording:
		List<TestCoverage> ret = new ArrayList();
		for (String methodName : methodNames) {
			// TODO: jada, jada - something needs to be reused
			final IRuntime runtime = new LoggerRuntime();
			final Instrumenter instr = new Instrumenter(runtime);
			// Now we're ready to run our instrumented class and need to startup the runtime
			// first:
			final RuntimeData data = new RuntimeData();
			runtime.startup(data);
			final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
			Class<?> junitClass = null;
			for (Class loadedClass : loadedClasses) {
				String targetName = loadedClass.getName();
				final byte[] instrumented = instr.instrument(getTargetClass(targetName), "");
				// In this tutorial we use a special class loader to directly load the
				// instrumented class definition from a byte[] instances.

				memoryClassLoader.addDefinition(targetName, instrumented);
				// We need to load it explicitly so that the instrumented version is used
				final Class<?> targetClass = memoryClassLoader.loadClass(targetName);
				if (targetName.equals(junitName)) {
					junitClass = targetClass;
				}

			}

			// Here we execute our test target class through its Runnable interface:
			/*
			 * final Runnable targetInstance = (Runnable) targetClass.newInstance();
			 * targetInstance.run();
			 */

			if (junitClass == null) {
				memoryClassLoader.addDefinition(junitName, instr.instrument(getTargetClass(junitName), ""));
				junitClass = memoryClassLoader.findClass(junitName);
			}

			Map<String, IClassCoverage> coverageByClass = runTest(loadedClasses, runtime, data, junitClass, methodName);
			List<ProdClassCoverage> xs = new ArrayList();
			for (Entry<String, IClassCoverage> entry : coverageByClass.entrySet()) {
				IClassCoverage coverage = entry.getValue();
				ProdClassCoverage cc = new ProdClassCoverage(coverage.getName().replaceAll("/", "."), coverage
						.getMethods().stream().collect(Collectors.
								toMap(m -> getMethodName(m), 
										m -> m.getLineCounter(),
										(a,b) -> {
											return a;
										})));
				xs.add(cc);
			}
			TestCoverage testCoverage = new TestCoverage(junitName, methodName, xs);
			ret.add(testCoverage);
		}
		return ret;
	}

	private String getMethodName(IMethodCoverage m) {
		return m.getName() + m.getDesc();
	}

	private Map<String, IClassCoverage> runTest(final Collection<Class> loadedClasses, final IRuntime runtime,
			final RuntimeData data, Class<?> junitClass, String methodName) {
		JUnitCore junit = new JUnitCore();
		Request request = Request.method(junitClass, methodName);
		Result result = junit.run(request);

		LOG.info("Failures: " + result.getFailures());

		// At the end of test execution we collect execution data and shutdown the
		// runtime:
		final ExecutionDataStore executionData = new ExecutionDataStore();
		data.collect(executionData, new SessionInfoStore(), false);
		runtime.shutdown();
		// Together with the original class definition we can calculate coverage
		// information:
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
		try {
			for (Class loadedClass : loadedClasses) {
				String targetName = loadedClass.getName();
				analyzer.analyzeClass(getTargetClass(targetName), targetName);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Let's dump some metrics and line coverage information:

		Map<String, IClassCoverage> map = new HashMap();
		for (final IClassCoverage cc : coverageBuilder.getClasses()) {
			String className = cc.getName().replaceAll("/", ".");
			System.out.printf("Coverage of class %s%n", className);
			printCounter("instructions", cc.getInstructionCounter());
			printCounter("branches", cc.getBranchCounter());
			printCounter("lines", cc.getLineCounter());
			printCounter("methods", cc.getMethodCounter());
			printCounter("complexity", cc.getComplexityCounter());
			for (IMethodCoverage x : cc.getMethods()) {
				// System.out.println("x:"+x.getName()+"," + x.getLineCounter());
				printCounter(getMethodName(x), x.getLineCounter());
			}
			for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
				// System.out.printf("Line %s: %s%n", Integer.valueOf(i),
				// getColor(cc.getLine(i).getStatus()));
			}
			map.put(className, cc);
		}
		return map;
	}

	public Collection<Class> getDependentClasses(String junitClassName) {
		final List<Class> loadedClasses = new ArrayList();
		MyClassLoaderListener x = new MyClassLoaderListener() {

			@Override
			public void classLoaded(Class<?> c) {
				LOG.info("loaded:" + c);
				if (!c.getName().contains("junit")) {
					loadedClasses.add(c);
				}
			}
		};
		MyClassLoader cl = new MyClassLoader(Thread.currentThread().getContextClassLoader(), x);
		Class<?> junitClass;
		try {
			junitClass = cl.loadClass(junitClassName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
		JUnitCore junit = new JUnitCore();
		// Request request = Request.classes(junitClass);
		Result result = junit.runClasses(junitClass);
		LOG.info("result: " + result.getFailures());
		return loadedClasses;

	}

	@Test
	public void testMethodDetection() {
		List<String> requests = getTestMethods(CalculadoraTest.class);
		assertFalse(requests.isEmpty());
		assertTrue("methods: " + requests, requests.contains("testAdd"));
	}

	private List<String> getTestMethods(Class clazz) {
		List<String> ret = Arrays.asList(clazz.getMethods()).stream().filter(p -> p.getAnnotation(Test.class) != null)
				.map(m -> m.getName()).collect(Collectors.toList());
		return ret;
	}

	@Test
	public void testDependenciesFound() {
		Collection<String> depClasses = getDependentClasses(CalculadoraTest.class.getName()).stream()
				.map(x -> x.getName()).collect(Collectors.toList());
		assertTrue("depClasses:" + depClasses, depClasses.contains(Calculadora.class.getName()));
		assertTrue("depClasses:" + depClasses, depClasses.contains(CalculadoraTest.class.getName()));
		assertTrue("depClasses:" + depClasses, depClasses.contains(CalculationResult.class.getName()));
		assertTrue("depClasses:" + depClasses, depClasses.contains(BuildProjects.class.getName()));
	}

	class ProdClassCoverage {
		private String className;
		private Map<String, ICounter> lineCoverageByMethod;

		ProdClassCoverage(String name, Map<String, ICounter> lineCoverageByMethod) {
			this.className = name;
			this.lineCoverageByMethod = lineCoverageByMethod;
		}

		public List<String> asCSV() {
			return lineCoverageByMethod.entrySet().stream().map(x -> className + DELIM + x.getKey() + DELIM
					+ x.getValue().getMissedCount() + DELIM + x.getValue().getCoveredCount())
					.collect(Collectors.toList());
					
		}

		@Override
		public String toString() {
			return asCSV().stream().collect(Collectors.joining("\n"));
		}
	}
	private static final String DELIM = "|";

	class TestCoverage {
		private String testClassName;
		private String testMethod;
		private Map<String, ProdClassCoverage> prodClassCoverage;

		TestCoverage(String testClassName, String testMethod, Collection<ProdClassCoverage> prodClassCoverage) {
			this.testClassName = testClassName;
			this.testMethod = testMethod;
			this.prodClassCoverage = prodClassCoverage.stream().collect(Collectors.toMap(e -> e.className, e -> e));
		}
		
		public List<String> asCSV() {
			List<String> ret = new ArrayList();
			for (ProdClassCoverage pcc : prodClassCoverage.values()) {
				List<String> x = pcc.asCSV().stream().map(s -> testClassName+DELIM+testMethod+DELIM + s).collect(Collectors.toList());
				ret.addAll(x);
			}
			return ret;
		}
	}

	public static void writeCSV(Collection<TestCoverage> testCoverages) throws IOException {
		CSVReporter reporter = createReporter();
		reportCoverages(testCoverages, reporter);
		reporter.close();
	}

	private static CSVReporter createReporter() throws IOException {
		String fname = "coverageByMethod.csv";
		List<String> fields = Arrays.asList("testClassName","testMethod","prodClassName","prodMethod","missedLines","coveredLines");
		CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(fname)),
				CSVFormat.DEFAULT.withHeader(fields.toArray(new String[0])).withDelimiter('|'));
		CSVReporter reporter = new CSVReporter(printer);
		return reporter;
	}

	private static void reportCoverages(Collection<TestCoverage> testCoverages, CSVReporter reporter) {
		for (TestCoverage testCov : testCoverages) {
			testCov.asCSV().stream().forEach(x -> reporter.write(x.split("\\" + DELIM)));
		}
	}
	@Test
	public void testAllMethodsCovered() throws Exception {
		List<TestCoverage> result = new MeasureCodeCoverageByTestAndProdMethod().measureTestCoverage(CalculadoraTest.class);
		writeCSV(result);
		Map<String, TestCoverage> coverateByMethod = result.stream()
				.collect(Collectors.toMap(e -> e.testMethod, e -> e));
		assertEquals(3, coverateByMethod.size());
		TestCoverage testAddCoverage = coverateByMethod.get("testAdd");
		ProdClassCoverage calculadoraProdCoverage = testAddCoverage.prodClassCoverage.get(Calculadora.class.getName());
		ICounter addMethodCoverage = calculadoraProdCoverage.lineCoverageByMethod.get("add(II)I");
		LOG.info("add: " + addMethodCoverage);
		assertEquals(1, addMethodCoverage.getCoveredCount());
		assertEquals(0, addMethodCoverage.getMissedCount());
		ICounter minusMethodCoverage = calculadoraProdCoverage.lineCoverageByMethod.get("minus(II)Ljava/lang/Number;");
		LOG.info("minusMethodCoverage: " + minusMethodCoverage);
		assertEquals(0, minusMethodCoverage.getCoveredCount());
		assertEquals(5, minusMethodCoverage.getMissedCount());

		TestCoverage testMinusCoverage = coverateByMethod.get("testSubtract");
		calculadoraProdCoverage = testMinusCoverage.prodClassCoverage.get(Calculadora.class.getName());
		addMethodCoverage = calculadoraProdCoverage.lineCoverageByMethod.get("add(II)I");
		LOG.info("add: " + addMethodCoverage);
		assertEquals(0, addMethodCoverage.getCoveredCount());
		assertEquals(1, addMethodCoverage.getMissedCount());
		minusMethodCoverage = calculadoraProdCoverage.lineCoverageByMethod.get("minus(II)Ljava/lang/Number;");
		LOG.info("minusMethodCoverage: " + minusMethodCoverage);
		assertEquals(2, minusMethodCoverage.getCoveredCount());
		assertEquals(3, minusMethodCoverage.getMissedCount());
	}

	public static void main(final String[] args) throws Exception {
		LOG.info("classpath: " + System.getProperty("java.class.path"));
		CSVReporter reporter = createReporter();
		for (String junitClassName : args) {
			List<TestCoverage> result = new MeasureCodeCoverageByTestAndProdMethod().measureTestCoverage(Class.forName(junitClassName));
			reportCoverages(result, reporter);
			reporter.flush();
		}
		reporter.close();
	}
}