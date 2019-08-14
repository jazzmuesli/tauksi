package org.pavelreich.zoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

public class JupiterTest {

	@Test
	public void test() {
		assertEquals(4, 2 + 2);
	}

	public static void main(String[] args) throws ClassNotFoundException {
		runTest(CalculatorTest.class);
		runTest(JupiterTest.class);
	}

	private static void runTest(Class<?> junitClass) {
		final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectClass(junitClass)).build();

		System.out.println("req: " + request);
		final Launcher launcher = LauncherFactory.create();
		final SummaryGeneratingListener listener = new SummaryGeneratingListener();

		launcher.registerTestExecutionListeners(listener);
		TestPlan testPlan = launcher.discover(request);
		Long found = testPlan.getRoots().stream().map(root -> 
		testPlan.getDescendants(root).size()).collect(Collectors.counting());
		System.out.println("found: "+found);

		testPlan.getRoots().forEach(root -> 
		System.out.println("root: " + root.getDisplayName() + ", planned:"+testPlan.getDescendants(root).stream().filter(p->p.getType()==Type.TEST).map(x->x.getUniqueId()+":"+x.getDisplayName()).collect(Collectors.toList())));
		launcher.execute(request);

		TestExecutionSummary summary = listener.getSummary();
		long testFoundCount = summary.getTestsFoundCount();
		System.out.println("found: " + testFoundCount);
		List<Failure> failures = summary.getFailures();
		System.out.println("getTestsSucceededCount() - " + summary.getTestsSucceededCount());
		failures.forEach(failure -> System.out.println("failure - " + failure.getException()));
	}
}
