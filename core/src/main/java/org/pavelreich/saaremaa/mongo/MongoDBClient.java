package org.pavelreich.saaremaa.mongo;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;


public class MongoDBClient {

	private MongoClient mongoClient;
	private MongoDatabase database;
	private Semaphore semaphore;
	private String name;

	private static final Logger LOG = LoggerFactory.getLogger(MongoDBClient.class);
	private static final int QUEUE_SIZE = 10;

	public void insertCollection(String name, List<Document> documents) {
		try {
//			LOG.info("inserting " + documents.size() + " documents");
			acquire(1,"insertCollection");
			
			Publisher<Success> pub = database.getCollection(name).insertMany(documents);
			Subscriber<Success> s = new Subscriber<Success>() {

				@Override
				public void onSubscribe(Subscription s) {
					s.request(1);
				}

				@Override
				public void onNext(Success t) {
				}

				@Override
				public void onError(Throwable t) {
					LOG.error(t.getMessage() , t);
				}

				@Override
				public void onComplete() {
//					LOG.info("comp");	
					semaphore.release();
				}
			};
			pub.subscribe(s);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	
	public MongoDBClient(String name) {
		java.util.logging.Logger mongoLogger = java.util.logging.Logger.getLogger( "org.mongodb.driver" );
		mongoLogger.setLevel(java.util.logging.Level.SEVERE); //TODO: fix logging
		this.mongoClient = MongoClients.create();
		this.name = name;
		this.database = mongoClient.getDatabase("db");
		this.semaphore = new Semaphore(QUEUE_SIZE);
	}

	public void waitForOperationsToFinish() {
		acquire(QUEUE_SIZE, "finish");
		semaphore.release(QUEUE_SIZE);
	}
	public void acquire(int permits, String reason) {
		try {
			long stime = System.currentTimeMillis();
			while(!semaphore.tryAcquire(permits, 1000, TimeUnit.MILLISECONDS)) {
				LOG.info(name + "." + reason + ": Waiting for " + semaphore.availablePermits() + " operations to finish since " + (System.currentTimeMillis() - stime));
			}
		} catch (InterruptedException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}
