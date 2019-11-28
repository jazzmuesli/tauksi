package org.pavelreich.saaremaa.mongo;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Ignore;
import org.junit.Test;
import org.pavelreich.saaremaa.analysis.DataFrame;
import org.pavelreich.saaremaa.mongo.MongoDBClient.MySubscriber;

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
	
	@Ignore
	@Test
	public void testSub() throws InterruptedException {
		MongoDBClient client = new MongoDBClient("classCoverage");
		final CountDownLatch latch = new CountDownLatch(1);
		client.database.listCollectionNames().subscribe(new MySubscriber(latch));
		assertTrue(latch.await(20, TimeUnit.SECONDS));
		Bson query = com.mongodb.client.model.Filters.eq("sessionId","730ef9c2-6467-44c3-8b08-2f2f8cdad4b5");
		List<Document> ret = client.find("classCoverage", query);
		System.out.println(ret);
		Set<Object> x = ret.stream().map(doc -> doc.get("prodClassName")).collect(Collectors.toSet());
		Map<String, Integer> coveredLines = sumLinesByClass(ret,"coveredLines");
		Map<String, Integer> missedLines = sumLinesByClass(ret,"missedLines");
		long prodClassesCovered = coveredLines.values().stream().filter(p -> p > 0).count();
		Map<String, Double> covratio = coveredLines.entrySet().stream().collect(
				Collectors.<Entry<String,Integer>, String,Double>toMap(k -> k.getKey(), 
						v-> Double.valueOf(v.getValue()) / Double.valueOf(v.getValue() + missedLines.getOrDefault(v.getKey(), 0))
						));
//		
		System.out.println(covratio);
		
	}

	protected Map<String, Integer> sumLinesByClass(List<Document> ret, String linesName) {
		Map<String, Integer> map = ret.stream().collect(Collectors.<Document,String,Integer>toMap(doc -> doc.getString("prodClassName"), 
				val -> val.getInteger(linesName, 0),
				(a,b) -> a+b));
		return map;
	}
}
