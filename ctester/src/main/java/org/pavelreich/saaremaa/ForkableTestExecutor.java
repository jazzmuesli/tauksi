package org.pavelreich.saaremaa;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
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
		String testClassName = args[0];
		String testMethodName = null;
		if (args.length > 1) {
			testMethodName = args[1];
		}
		//Document df = runJunit4(testClassName, testMethodName);
		Document df = runJunit5(testClassName, testMethodName);

		db.insertCollection("testsExecuted", Arrays.asList(df));
		db.waitForOperationsToFinish();
	}

	private static Document runJunit5(String testClassName, String testMethodName) throws ClassNotFoundException {
		Class<?> junitClass = Class.forName(testClassName);
		final LauncherDiscoveryRequest  request;
		if (testMethodName != null && !testMethodName.trim().isEmpty()) {
			request = LauncherDiscoveryRequestBuilder.request()
					.selectors(selectMethod(junitClass, testMethodName)).build();
		} else {
			request = LauncherDiscoveryRequestBuilder.request()
					.selectors(selectClass(junitClass)).build();
		}

		final Launcher launcher = LauncherFactory.create();
		final SummaryGeneratingListener listener = new SummaryGeneratingListener();

		TestPlan testPlan = launcher.discover(request);
		long found = testPlan.getRoots().stream().mapToLong(root -> 
		testPlan.getDescendants(root).stream().filter(p->p.getType()==Type.TEST).count()).sum();
		CLOG.info("Running test "+ request + " for " + junitClass + ", method=" + testMethodName + " planned "+ found + " tests");

		launcher.registerTestExecutionListeners(listener);
		launcher.execute(request);

		TestExecutionSummary summary = listener.getSummary();

		long stime = System.currentTimeMillis();
		

		if (summary.getTotalFailureCount() > 0) {
			CLOG.warn("Failures for " + junitClass + " : " + summary.getFailures().stream().map(x->x.getException()).collect(Collectors.toList()));
		}
		List<String> failures = summary.getFailures().stream().map(x -> x.toString()).collect(Collectors.toList());
		CLOG.info("Finished " + summary.getTestsSucceededCount()+"/"+summary.getTestsFailedCount() +" tests for " + junitClass + ", method=" + testMethodName + " with failures=" + failures);
		Document df = new Document().
				append("testClassName", testClassName).
				append("testMethodName", testMethodName).
				append("failedTests", summary.getTotalFailureCount()).
				append("runCount", summary.getTestsSucceededCount()).
				append("ignoreCount", summary.getTestsSkippedCount()).
				append("startTime", stime).
				append("runTime", summary.getTimeFinished() - summary.getTimeStarted()).
				append("failures", failures);
		return df;
	}


	private static Document runJunit4(String testClassName, String testMethodName) throws ClassNotFoundException {
		Class<?> junitClass = Class.forName(testClassName);
		Request request = Request.aClass(junitClass);
		long stime = System.currentTimeMillis();
		if (testMethodName != null) {
			request = Request.method(junitClass, testMethodName);
		}
		
		CLOG.info("Running test "+ request.getClass().getSimpleName() + "  for " + junitClass + ", method=" + testMethodName);
		JUnitCore junit = new JUnitCore();
		Result result = junit.run(request);

		if (result.getFailureCount() > 0) {
			CLOG.warn("Failures for " + junitClass + " : " + result.getFailures());
		}
		List<String> failures = result.getFailures().stream().map(x -> x.toString()).collect(Collectors.toList());
		Document df = new Document().
				append("testClassName", testClassName).
				append("testMethodName", testMethodName).
				append("failedTests", result.getFailureCount()).
				append("runCount", result.getRunCount()).
				append("ignoreCount", result.getIgnoreCount()).
				append("startTime", stime).
				append("runTime", result.getRunTime()).
				append("failures", failures);
		return df;
	}
}
