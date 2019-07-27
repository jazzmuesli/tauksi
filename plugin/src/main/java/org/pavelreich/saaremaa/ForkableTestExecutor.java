package org.pavelreich.saaremaa;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
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


	public static void main(String[] args) throws Exception {

		MongoDBClient db = new MongoDBClient("ForkableTestExecutor");
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
		List<String> failures = result.getFailures().stream().map(x -> x.toString()).collect(Collectors.toList());
		Document df = new Document().
				append("testClassName", testClassName).
				append("failedTests", result.getFailureCount()).
				append("runCount", result.getRunCount()).
				append("ignoreCount", result.getIgnoreCount()).
				append("startTime", stime).
				append("runTime", result.getRunTime()).
				append("failures", failures);

		db.insertCollection("testExecution", Arrays.asList(df));
		db.waitForOperationsToFinish();
	}
}
