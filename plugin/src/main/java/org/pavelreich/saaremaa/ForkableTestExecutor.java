package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.pavelreich.saaremaa.analysis.DataFrame;
import org.pavelreich.saaremaa.codecov.ProdClassCoverage;
import org.pavelreich.saaremaa.codecov.TestCoverage;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute tests in a separate process with two agents attached:
 * jagent: intercept method calls to junit and gather statistics.
 * jacoco: record coverage and then write to CSV
 * TODO: report to mongo.
 * 
 * @author preich
 *
 */
public class ForkableTestExecutor {
	private static final Logger CLOG = LoggerFactory.getLogger(ForkableTestExecutor.class);
	private Logger LOG = CLOG;

	private static List<String> getTestMethods(Class clazz) {
		List<String> ret = Arrays.asList(clazz.getMethods()).stream().filter(p -> p.getAnnotation(Test.class) != null)
				.map(m -> m.getName()).collect(Collectors.toList());
		return ret;
	}
	private final File jagentPath;
	private final File jacocoPath;
	private final File targetClasses;
	private long timeout = 15;
	//= new File("/Users/preich/Documents/git/tauksi/core/target/classes");//TODO
	public ForkableTestExecutor(Logger log, File jagentPath, File jacocoPath, File targetClasses) {
		this.LOG = log;
		this.jagentPath = jagentPath;
		this.jacocoPath = jacocoPath;
		this.targetClasses = targetClasses;
	}

	public void run(String jc) {
		Collection<String> classpath = Arrays.asList(System.getProperty("java.class.path").split(":"));
		
		try {
			launch(jc, classpath);
//			List<String> methodNames = getTestMethods(junitClass);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	void launch(String jc, Collection<String> classpath)
			throws IOException, InterruptedException {
		long stime = System.currentTimeMillis();
		String fname = stime + ".exec";
		///Users/preich/Documents/git/tauksi/jagent/target/jagent-1.0-SNAPSHOT-jar-with-dependencies.jar
		///Users/preich/.m2/repository/org/jacoco/org.jacoco.agent/0.8.4/org.jacoco.agent-0.8.4-runtime.jar
		String cmd = "java -javaagent:" + jagentPath + "=org.brutusin.instrumentation.logging.LoggingInterceptor " + 
				"-javaagent:" + jacocoPath+ "=destfile=" + fname + 
				"  -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator))
				+ " " + ForkableTestExecutor.class.getCanonicalName() + " " + jc;// + " " + methodName;
//		LOG.info("cmd: " + cmd);
		FileWriter fw = new FileWriter("last_command.sh");
		fw.write(cmd);
		fw.close();
		ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
		pb.inheritIO();
		Process p;
		p = pb.start();
		boolean ret = p.waitFor(timeout, TimeUnit.SECONDS);
		LOG.info("ret: " + ret + ", alive: "+ p.isAlive());
		if (!ret) {
			p.destroy();
			p.destroyForcibly();			
		}
		
		long duration = System.currentTimeMillis() - stime;
		
		int exitValue = p.isAlive() ? -1 : p.exitValue();
		//TODO: report to mongo
		LOG.info("took: " + duration + ", result:" + ret + ", exit: " + exitValue);
		// from https://www.jacoco.org/jacoco/trunk/doc/examples/java/ReportGenerator.java
		ExecFileLoader execFileLoader = new ExecFileLoader();
		File file = new File(fname);
		if (file.exists()) {
			execFileLoader.load(file);
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

			analyzer.analyzeAll(targetClasses);
			
			Map<String, IClassCoverage> map = new HashMap();
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String className = cc.getName().replaceAll("/", ".");
				map.put(className, cc);
			}
			org.pavelreich.saaremaa.codecov.TestExecutionResults testExecResults = new org.pavelreich.saaremaa.codecov.TestExecutionResults(map, new Result());

			List<ProdClassCoverage> xs = new ArrayList();
			for (Entry<String, IClassCoverage> entry : testExecResults.coverageByProdClass.entrySet()) {
				IClassCoverage coverage = entry.getValue();
				ProdClassCoverage cc = ProdClassCoverage.createProdCoverage(coverage);
				xs.add(cc);
			}
			TestCoverage testCoverage = new TestCoverage(jc, "unknown", xs, testExecResults.result);
			
			CSVReporter reporter = TestCoverage.createReporter("_"+jc);
			TestCoverage.reportCoverages(Arrays.asList(testCoverage), reporter);
			reporter.close();

			MongoDBClient db = new MongoDBClient();
			DataFrame df = new DataFrame();
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String prodClassName = cc.getName().replaceAll("/", ".");
				df=df.append(new DataFrame().
						addColumn("prodClassName", prodClassName).
						addColumn("testClassName", jc).
						addColumn("coveredLines", cc.getLineCounter().getCoveredCount()).
						addColumn("missedLines", cc.getLineCounter().getMissedCount())
						);
				if (cc.getInstructionCounter().getCoveredCount() > 0) {
					LOG.info("Coverage of class " + prodClassName);
					printCounter("instructions", cc.getInstructionCounter());

				}
			}
			db.insertCollection("coverage", df.toDocuments());

		}
	}

	private void printCounter(final String unit, final ICounter counter) {
		final Integer missed = Integer.valueOf(counter.getMissedCount());
		final Integer total = Integer.valueOf(counter.getTotalCount());

		//TODO: report to mongo
		String msg = String.format("%s of %s %s missed%n", missed, total, unit);
		LOG.info(msg);
	}

	public static void main(String[] args) throws Exception {

		MongoDBClient db = new MongoDBClient();
		JUnitCore junit = new JUnitCore();
		String testClassName = args[0];
		Class<?> junitClass = Class.forName(testClassName);
		Request request = Request.aClass(junitClass);
		long stime = System.currentTimeMillis();
		if (args.length > 1) {
			String methodName = args[1];
			request = Request.method(junitClass, methodName);
		}
		CLOG.info("Req: "+ request + "  for " + junitClass);
		Result result = junit.run(request);

		if (result.getFailureCount() > 0) {
			CLOG.warn("Failures for " + junitClass + " : " + result.getFailures());
		}
		DataFrame df = new DataFrame().addColumn("testClassName", testClassName).
				addColumn("failedTests", result.getFailureCount()).
				addColumn("runCount", result.getRunCount()).
				addColumn("ignoreCount", result.getIgnoreCount()).
				addColumn("startTime", stime).
				addColumn("runTime", result.getRunTime());

		db.insertCollection("testExecution", df.toDocuments());
		//TODO: report to mongo
	}
}
