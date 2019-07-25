package org.pavelreich.saaremaa.mongo;

import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.pavelreich.saaremaa.analysis.DataFrame;
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

	private static final Logger LOG = LoggerFactory.getLogger(MongoDBClient.class);

	public void insertCollection(String name, List<Document> documents) {
		try {
			Publisher<Success> pub = database.getCollection(name).insertMany(documents);
			pub.subscribe(new Subscriber<Success>() {

				@Override
				public void onSubscribe(Subscription s) {
//					LOG.info("sub: " + s);
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
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public MongoDBClient() {
		java.util.logging.Logger mongoLogger = java.util.logging.Logger.getLogger( "org.mongodb.driver" );
		mongoLogger.setLevel(java.util.logging.Level.SEVERE); //TODO: fix logging
		this.mongoClient = MongoClients.create();
		this.database = mongoClient.getDatabase("db");
	}

	public static void main(String[] args) throws InterruptedException {
		MongoDBClient db = new MongoDBClient();
		DataFrame df = new DataFrame();
		for (int i=0;i<100;i++) {
			df=df.append(new DataFrame().addColumn("name", "Banana").addColumn("surname", "cake").addColumn("dateTime", new Date()));
		}
		
		db.insertCollection("users", df.toDocuments());
	}
}
