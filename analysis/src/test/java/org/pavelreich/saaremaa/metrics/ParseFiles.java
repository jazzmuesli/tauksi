 package org.pavelreich.saaremaa.metrics;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

/**
 * sketch of TDATA parsing
 *
 */
public class ParseFiles {

	
	@Test
	public void parseCSVs() throws Exception {
		

	}

	





	@Ignore
	@Test
	public void test() throws Exception {
		CSVParser parser = CSVParser.parse(new File("files/testsLaunched.csv"), Charset.defaultCharset(),
				CSVFormat.DEFAULT.withFirstRecordAsHeader());
		List<CSVRecord> records = parser.getRecords();
		Map<String, String> testClassNamesBySessionId = records.stream()
				.collect(Collectors.toMap(x -> x.get("sessionId"), x -> x.get("testClassName")));
		assertEquals(343, records.size());
		// BoxAndWhiskerXYToolTipGeneratorTest
		String sessionId = "eee7e63f-801e-4a07-9d72-5c54988cea29";
		String tcn = testClassNamesBySessionId.get(sessionId);
		readTMetrics(sessionId, tcn);
		Map<String, TMetrics> map = testClassNamesBySessionId.entrySet().parallelStream()
				.map(e -> readTMetrics(e.getKey(), e.getValue())).filter(p -> p != null)
				.collect(Collectors.toMap(e -> e.testClassName, e -> e));

		try (CSVPrinter printer = new CSVPrinter(new FileWriter("metrics.csv"), CSVFormat.DEFAULT)) {
			printer.printRecord("testClassName","TASSERT","TDATA","TINVOK");
			for (Entry<String,TMetrics> entry : map.entrySet()) {
				printer.printRecord(entry.getValue().testClassName, entry.getValue().tASserts.size(), entry.getValue().tData.size(), entry.getValue().tInvoks.size());				
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	protected TMetrics readTMetrics(String sessionId, String tcn) {
		String pcn = tcn.replaceAll("Test$", "");
		String fname = "files/" + sessionId + "-result.json";
		File file = new File(fname);
		if (!file.exists()) {
			return null;
		}
		String json;
		try {
			json = new String(Files.readAllBytes(Paths.get(file.toURI())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Document doc = Document.parse(json);
		List<List> results = doc.get("results", List.class);
		assertEquals(1, results.size());
		List<Document> docs = results.get(0);
		List<String> tInvoks = docs.stream().filter(p -> "TInvok".equals(p.get("metricType")))
				.map(x -> toAssertRepresentation(x))
				.filter(p -> p != null && p.startsWith(tcn + "::") && p.contains(pcn + "::"))
				.collect(Collectors.toList());
		List<String> tData = docs.stream().filter(p -> "TData".equals(p.get("metricType"))).map(x -> toTData(x))
				.filter(p -> p != null && p.startsWith(tcn + "::")).collect(Collectors.toList());

		List<String> asserts = docs.stream().filter(p -> "TAssert".equals(p.get("metricType")))
				.map(x -> toAssertRepresentation(x)).filter(p -> p != null).collect(Collectors.toList());
		TMetrics metrics = new TMetrics(tcn, asserts, tData, tInvoks);
		return metrics;
	}

	static class TMetrics {
		List<String> tASserts, tData, tInvoks;
		String testClassName;

		public TMetrics(String testClassName, List<String> tASserts, List<String> tData, List<String> tInvoks) {
			this.testClassName = testClassName;
			this.tASserts = tASserts;
			this.tData = tData;
			this.tInvoks = tInvoks;
		}

	}

	private String toTData(Document x) {
		String sourceLocation = getSourceLocation(x);
		return sourceLocation + " ==> " + x.getString("dataType");
	}

	protected String toAssertRepresentation(Document x) {
		String sourceLocation = getSourceLocation(x);
		if (sourceLocation.contains("junit.framework") || sourceLocation.contains("org.junit")) {
			return null;
		}
		return sourceLocation + " ==> " + getTargetLocation(x);
	}

	protected Object getTargetLocation(Document x) {
		Document doc = (Document) x.get("targetLocation");
		return doc.get("className") + "::" + doc.get("methodName") + ":" + doc.get("descriptor");
	}

	protected String getSourceLocation(Document x) {
		Document doc = (Document) x.get("sourceLocation");
		return doc.get("className") + "::" + doc.get("methodName") + ":" + doc.get("line");
	}
}
