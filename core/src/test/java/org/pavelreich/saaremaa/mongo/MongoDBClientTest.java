package org.pavelreich.saaremaa.mongo;

import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Ignore;
import org.junit.Test;
import org.pavelreich.saaremaa.analysis.DataFrame;

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
		Bson query = com.mongodb.client.model.Filters.eq("sessionId","730ef9c2-6467-44c3-8b08-2f2f8cdad4b5");
		List<Document> ret = client.find("classCoverage", query);
		
	}
}
