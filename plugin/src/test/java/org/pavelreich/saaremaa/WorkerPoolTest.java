package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class WorkerPoolTest {
	@Test
	public void testSingleThreaded() throws InterruptedException {
		runTest(1);
	}

	@Test
	public void testMultiple() throws InterruptedException {
		runTest(3);
	}

	protected void runTest(int nThreads) {
		int tasksCount = 100;
		final AtomicInteger counter = new AtomicInteger(0);
		WorkerPool pool = new WorkerPool(nThreads);
		for (int i = 0; i < tasksCount; i++) {
			pool.submit(() -> counter.incrementAndGet());
		}

		assertTrue(pool.await());
		assertEquals(tasksCount, counter.get());
	}
}
