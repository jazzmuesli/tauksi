package org.pavelreich.saaremaa.mongo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Ignore;
import org.junit.Test;

public class MongoDBClientTest {

	@Ignore
	@Test
	public void test() throws InterruptedException {
		long stime = System.currentTimeMillis();
		MongoDBClient db = new MongoDBClient(getClass().getSimpleName());
		for (int op=0; op<1000; op++) {
			List<Document> dfs = new ArrayList();
			for (int i=0;i<10;i++) {
				Document df = new Document();
				df=new Document().
						append("startTime", stime).
						append("name", "Banana").
						append("surname", "cake").
						append("dateTime", new Date());
				dfs.add(df);
			}
			
			db.insertCollection("users", dfs);
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
