package org.pavelreich.saaremaa.mongo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;

public class MongoDBClient {

	 static final class MySubscriber implements Subscriber {
		private final CountDownLatch latch;

		MySubscriber(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onSubscribe(Subscription s) {
//			LOG.info("onSubscribe: " + s);	
			 s.request(Integer.MAX_VALUE);
		}

		@Override
		public void onNext(Object t) {
//			LOG.info("onNext: " + t);
		}

		@Override
		public void onError(Throwable t) {
//			LOG.error("onError: " + t);				
		}

		@Override
		public void onComplete() {
//			LOG.info("onComplete: ");	
			latch.countDown();
		}
	}
	private MongoClient mongoClient;
	MongoDatabase database;
	private Semaphore semaphore;
	private String name;

	private static final Logger LOG = LoggerFactory.getLogger(MongoDBClient.class);
	private static final int QUEUE_SIZE = 10;

	public List<Document> find(String collectionName, Bson query) {
		FindPublisher<Document> ret = database.getCollection(collectionName).find(query);
		CountDownLatch findLatch = new CountDownLatch(1);
		List<Document> found = new CopyOnWriteArrayList<Document>();
		ret.subscribe(new Subscriber<Document>() {

			@Override
			public void onSubscribe(Subscription s) {
				s.request(Integer.MAX_VALUE);
			}

			@Override
			public void onNext(Document t) {
				found.add(t);
			}

			@Override
			public void onError(Throwable t) {
				LOG.error("onError for collection " + collectionName + ", query " + query + " got exception " + t.getMessage(), t);
				findLatch.countDown();
			}

			@Override
			public void onComplete() {
				LOG.info("onComplete for collection " + collectionName + ", query " + query + " got " + found.size() + " results");
				findLatch.countDown();				
			}
		});
		try {
			findLatch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.error(e.getLocalizedMessage(), e);
		}
		return found;
		
	}
	public void insertCollection(String name, List<Document> documents) {
		if (documents == null || documents.isEmpty()) {
			return;
		}
		try {

			if (LOG.isDebugEnabled()) {
				LOG.debug("inserting.name=" + this.name + ", collection=" + name + ", " + documents.size()
						+ " documents. Semaphore: " + semaphore.availablePermits());
			}
			acquire(1, "insertCollection", 0);

			Publisher<Success> pub = database.getCollection(name).insertMany(documents);
			Subscriber<Success> s = new Subscriber<Success>() {

				@Override
				public void onSubscribe(Subscription s) {

					if (LOG.isDebugEnabled()) {
//						LOG.debug("onSubscribe:" + s);
					}
					s.request(1);
				}

				@Override
				public void onNext(Success t) {
					if (LOG.isDebugEnabled()) {
//						LOG.debug("onNext:" + t);
					}

				}

				@Override
				public void onError(Throwable t) {
					LOG.error("onError: " + t.getMessage(), t);
					semaphore.release();
				}

				@Override
				public void onComplete() {
					if (LOG.isDebugEnabled()) {
//						LOG.info("onComplete");
					}
					semaphore.release();
				}
			};
			pub.subscribe(s);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public MongoDBClient(String name) {
		java.util.logging.Logger mongoLogger = java.util.logging.Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(java.util.logging.Level.SEVERE); // TODO: fix logging
		this.mongoClient = MongoClients.create();
		this.name = name;
		this.database = mongoClient.getDatabase("db");
		this.semaphore = new Semaphore(QUEUE_SIZE);
	}

	public void waitForOperationsToFinish() {
		waitForOperationsToFinish(120000L);
	}

	public void waitForOperationsToFinish(long maxTimeout) {
		acquire(QUEUE_SIZE, "finish", maxTimeout);
		semaphore.release(QUEUE_SIZE);
	}

	public void acquire(int permits, String reason, long maxTimeout) {
		try {
			long stime = System.currentTimeMillis();
			while (!semaphore.tryAcquire(permits, 1000, TimeUnit.MILLISECONDS)) {
				long elapsed = System.currentTimeMillis() - stime;
				LOG.info(name + "." + reason + ": Waiting for " + semaphore.availablePermits()
						+ " operations to finish since " + elapsed);
				if (maxTimeout > 0 && elapsed > maxTimeout) {
					break;
				}
			}
		} catch (InterruptedException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}
