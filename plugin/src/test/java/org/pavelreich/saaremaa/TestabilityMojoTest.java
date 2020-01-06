package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

public class TestabilityMojoTest {

	@Ignore
	@Test
	public void test() {
		File file = new File("/Users/preich/Documents/github/jfreechart/target/testability.xml");
		if (file.exists()) {
			List<Document> doc = TestabilityMojo.parseXMLToJSON(file);
			assertEquals(20, doc.size());
		}
	}
}
