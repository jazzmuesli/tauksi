package org.pavelreich.saaremaa.codecov;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jacoco.core.runtime.RuntimeData;
import org.jacoco.core.data.SessionInfoStore;

/**
 * from https://stackoverflow.com/questions/40797698/record-test-coverage-per-test-case-using-eclemma-tool?rq=1
 * @author preich
 *
 */
public class CoreTutorialTest
{
    /**
     * A class loader that loads classes from in-memory data.
     */
    public static class MemoryClassLoader extends ClassLoader
    {
        private final Map<String, byte[]> definitions = new HashMap<String, byte[]>();
        /**
         * Add a in-memory representation of a class.
         * 
         * @param name name of the class
         * @param bytes class definition
         */
        public void addDefinition(final String name, final byte[] bytes) {
            definitions.put(name, bytes);
        }
        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
        {
            final byte[] bytes = definitions.get(name);
            if (bytes != null)
                return defineClass(name, bytes, 0, bytes.length);
            return super.loadClass(name, resolve);
        }
    }
    private InputStream getTargetClass(final String name)
    {
        final String resource = '/' + name.replace('.', '/') + ".class";
        return getClass().getResourceAsStream(resource);
    }
    private void printCounter(final String unit, final ICounter counter)
    {
        final Integer missed    = Integer.valueOf(counter.getMissedCount());
        final Integer total     = Integer.valueOf(counter.getTotalCount());

        System.out.printf("%s of %s %s missed%n", missed, total, unit);
    }
    private String getColor(final int status)
    {
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

    static final Logger LOG = LoggerFactory.getLogger(CoreTutorialTest.class);
    class TrackingClassLoader extends ClassLoader {
    	@Override
    	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    		LOG.info("loadClass: " + name);
    		return super.loadClass(name, resolve);
    	}
    	@Override
    	public Class<?> loadClass(String name) throws ClassNotFoundException {
    		LOG.info("loadClass: " + name);
    		// TODO Auto-generated method stub
    		return super.loadClass(name);
    	}
    }
    private void runTutorial() throws Exception
    {
        final String targetName = Calculadora.class.getName();
        // For instrumentation and runtime we need a IRuntime instance to collect execution data:
        final IRuntime runtime = new LoggerRuntime();
        // The Instrumenter creates a modified version of our test target class that contains additional probes for execution data recording:
        final Instrumenter instr = new Instrumenter(runtime);
        final byte[] instrumented = instr.instrument(getTargetClass(targetName), "");
        // Now we're ready to run our instrumented class and need to startup the runtime first:
        final RuntimeData data = new RuntimeData();
        runtime.startup(data);
        // In this tutorial we use a special class loader to directly load the instrumented class definition from a byte[] instances.
        String junitName = CalculadoraTest.class.getName();
        Class<?> junitClass;
        TrackingClassLoader cl = new TrackingClassLoader();

		if (true) {
        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
        memoryClassLoader.addDefinition(targetName, instrumented);
        // We need to load it explicitly so that the instrumented version is used
        final Class<?> targetClass = memoryClassLoader.loadClass(targetName);
        // Here we execute our test target class through its Runnable interface:
        /*final Runnable targetInstance = (Runnable) targetClass.newInstance();
        targetInstance.run();*/

        memoryClassLoader.addDefinition(junitName, instr.instrument(getTargetClass(junitName), ""));
        junitClass = memoryClassLoader.loadClass(junitName);
        } else {
        	Thread.currentThread().setContextClassLoader(cl);
			junitClass = cl.loadClass(junitName);
        
        }
		Thread thread = new Thread(() -> runTest(targetName, runtime, data, junitClass));
		thread.setContextClassLoader(cl);
		thread.start();
		thread.join();
    }
	private void runTest(final String targetName, final IRuntime runtime, final RuntimeData data, Class<?> junitClass)
			 {
		JUnitCore junit = new JUnitCore();
        List<String> methodNames = Arrays.asList(junitClass.getDeclaredMethods()).stream().
        		filter(m -> m.getAnnotation(Test.class) != null).
        		map(x->x.getName()).collect(Collectors.toList());
        Request request = Request.method(junitClass, methodNames.get(0));
        Result result = junit.run(request);
        //Result result = junit.run(junitClass);

        System.out.println("Failure count: " + result.getFailureCount());

        // At the end of test execution we collect execution data and shutdown the runtime:
        final ExecutionDataStore executionData = new ExecutionDataStore();
        data.collect(executionData, new SessionInfoStore(), false);
        runtime.shutdown();
        // Together with the original class definition we can calculate coverage information:
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        try {
			analyzer.analyzeClass(getTargetClass(targetName), targetName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // Let's dump some metrics and line coverage information:
        for (final IClassCoverage cc : coverageBuilder.getClasses())
        {
            System.out.printf("Coverage of class %s%n", cc.getName());
            printCounter("instructions", cc.getInstructionCounter());
            printCounter("branches", cc.getBranchCounter());
            printCounter("lines", cc.getLineCounter());
            printCounter("methods", cc.getMethodCounter());
            printCounter("complexity", cc.getComplexityCounter());
            for (IMethodCoverage x : cc.getMethods()) {
            	System.out.println("x:"+x.getName()+"," + x.getLineCounter());
            }
            for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
                System.out.printf("Line %s: %s%n", Integer.valueOf(i), getColor(cc.getLine(i).getStatus()));
            }
        }
	}
    public static void main(final String[] args) throws Exception {
        new CoreTutorialTest().runTutorial();
    }
}