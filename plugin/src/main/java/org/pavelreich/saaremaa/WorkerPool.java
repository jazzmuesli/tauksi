package org.pavelreich.saaremaa;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * simplistic pool to run tasks in parallel
 * 
 *
 */
public class WorkerPool {
	private final ThreadPoolExecutor pool;
	private final Collection<Future<?>> futures = new CopyOnWriteArrayList<>();
	private final int nThreads;
	private final AtomicInteger tasksCount = new AtomicInteger(0);
	private ArrayBlockingQueue<Runnable> workQueue;

	public WorkerPool(int nThreads) {
		this.nThreads = nThreads;
		if (nThreads > 1) {
			this.workQueue = new ArrayBlockingQueue<Runnable>(nThreads, true);
			this.pool = new ThreadPoolExecutor(nThreads, nThreads, 5000L, TimeUnit.MILLISECONDS,
					workQueue, new ThreadPoolExecutor.CallerRunsPolicy());
		} else {
			this.pool = null;
		}
	}

	public void submit(Runnable run) {
		tasksCount.incrementAndGet();
		if (nThreads == 1) {
			run.run();
		} else {
			Future<?> future = pool.submit(run);
			futures.add(future);
		}
	}

	public boolean await() {
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				throw new IllegalArgumentException("Something went wrong with: " + f);
			}
		}
		return true;
	}

	public int getTasksCount() {
		return tasksCount.get();
	}
	
	@Override
	public String toString() {
		return "[enqueued tasks:" + getTasksCount() + ", queueSize: " + workQueue.size() + "]";
	}
}