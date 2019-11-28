package org.pavelreich.saaremaa.mongo;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.pavelreich.saaremaa.analysis.DataFrame;
import org.pavelreich.saaremaa.mongo.MongoDBClient.MySubscriber;

import com.mongodb.reactivestreams.client.FindPublisher;

public class MongoDBClientTest {

	@Ignore
	@Test
	public void test() throws InterruptedException {
		long stime = System.currentTimeMillis();
		MongoDBClient db = new MongoDBClient(getClass().getSimpleName());
		for (int op=0; op<1000; op++) {
			DataFrame df = new DataFrame();
			for (int i=0;i<10;i++) {
				df=df.append(new DataFrame().
						addColumn("startTime", stime).
						addColumn("name", "Banana").
						addColumn("surname", "cake").
						addColumn("dateTime", new Date()));
			}
			
			db.insertCollection("users", df.toDocuments());
		}

		db.waitForOperationsToFinish();
	}
	
	@Test
	public void testSub() throws InterruptedException {
		MongoDBClient client = new MongoDBClient("classCoverage");
		final CountDownLatch latch = new CountDownLatch(1);
		client.database.listCollectionNames().subscribe(new MySubscriber(latch));
		assertTrue(latch.await(20, TimeUnit.SECONDS));
		FindPublisher<Document> ret = client.database.getCollection("classCoverage").find(com.mongodb.client.model.Filters.eq("sessionId","ecd11080-3955-4b13-8ff6-b5009e39f207"));
		CountDownLatch findLatch = new CountDownLatch(1);
		ret.subscribe(new MySubscriber(findLatch));
		assertTrue(findLatch.await(20, TimeUnit.SECONDS));
	}
}
