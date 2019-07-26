package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.brutusin.instrumentation.logging.LoggingInterceptor;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.pavelreich.saaremaa.analysis.DataFrame;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute tests in a separate process with two agents attached:
 * jagent: intercept method calls to junit and gather statistics.
 * jacoco: record coverage and then write to CSV
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
	void launch(String testClassName, Collection<String> classpath)
			throws IOException, InterruptedException {
		long stime = System.currentTimeMillis();
		String fname = stime + ".exec";
		String cmd = "java -javaagent:" + jagentPath + 
				"=" + LoggingInterceptor.class.getCanonicalName()+ 
				";testClassName=" + testClassName + " " + 
				"-javaagent:" + jacocoPath+ "=destfile=" + fname + 
				"  -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator))
				+ " " + ForkableTestExecutor.class.getCanonicalName() + " " + testClassName;// + " " + methodName;
//		LOG.info("cmd: " + cmd);
		FileWriter fw = new FileWriter("last_command.sh");
		fw.write(cmd);
		fw.close();
		List<String> cmdArgs = Arrays.asList(cmd.split("\\s+"));
		LOG.info("cmdArgs:"+cmdArgs);
		ProcessBuilder pb = new ProcessBuilder(cmdArgs);
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
		LOG.info("took: " + duration + ", result:" + ret + ", exit: " + exitValue);

		
		ExecFileLoader execFileLoader = new ExecFileLoader();
		File file = new File(fname);
		if (file.exists()) {
			execFileLoader.load(file);
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

			analyzer.analyzeAll(targetClasses);

			MongoDBClient db = new MongoDBClient();
			DataFrame df = new DataFrame();
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String prodClassName = cc.getName().replaceAll("/", ".");
				df=df.append(new DataFrame().
						addColumn("prodClassName", prodClassName).
						addColumn("testClassName", testClassName).
						addColumn("coveredLines", cc.getLineCounter().getCoveredCount()).
						addColumn("missedLines", cc.getLineCounter().getMissedCount())
						);
			}
			db.insertCollection("coverage", df.toDocuments());

		}
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
