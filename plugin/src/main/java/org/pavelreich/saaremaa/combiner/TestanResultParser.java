package org.pavelreich.saaremaa.combiner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.pavelreich.saaremaa.Helper;
import org.slf4j.Logger;

import com.google.gson.Gson;

/**
 * parse result.json, class.csv, method.csv
 * 
 * @author preich
 *
 */
public class TestanResultParser {
	
	private Logger logger;

	public TestanResultParser(Logger logger) {
		this.logger=logger;
	}

	public void doStuff(MetricsManager metricsManager, 
			File resultJsonFname, File classFname, File methodFname) throws IOException {
		String json = Files.readAllLines(resultJsonFname.toPath()).stream().collect(Collectors.joining());
		Gson g = new Gson();
		CSVParser classParser = Helper.getParser(classFname, "class");
		Map<String, Object> testClassMetrics = classParser.getRecords().stream().collect(Collectors.toMap(k->k.get("class"), v->v));
		CSVParser methodParser = Helper.getParser(methodFname, "class");
		Map<String, List<Map<String, String>>> testMethodMetrics = methodParser.getRecords().stream().map(x->x.toMap()).collect(Collectors.groupingBy(k->k.get("class")));
		List<Map> testClasses = g.fromJson(json, List.class);
		logger.info("In " + resultJsonFname + "  found  "+ testClasses.size() + " tests");
		for (Map testClass : testClasses) {
			String tcn = (String) testClass.get("simpleName");
			String testCat = Helper.classifyTest(tcn);
			String pcn = Helper.getProdClassName(tcn);
			Metrics ret = metricsManager.provideMetrics(pcn);
			CSVRecord classMetrics = (CSVRecord) testClassMetrics.get(tcn);
			List<Map<String,String>> methodMetrics = testMethodMetrics.get(tcn);
			ret.put("testClassName."+testCat, tcn);
			Collection<Map<String,Object>> setupMethods = (Collection<Map<String, Object>>) testClass.getOrDefault("setupMethods", Collections.emptyList());
			int setupLOC = getLOC(setupMethods);
			ret.put("setupMethods." + testCat, setupMethods.size());
			ret.put("setupMethods.loc."+testCat, setupLOC);
			Collection<Map<String,Object>> testMethods = (Collection)testClass.getOrDefault("testMethods", Collections.emptyList());
			ret.put("testMethods."+testCat, testMethods.size());
			ret.put("testMethods.loc."+testCat, getLOC(testMethods));
			ret.put("testAnnotations."+testCat, getDelimCollections(testClass, "annotations"));
			ret.put("testInterfaces."+testCat, getDelimCollections(testClass, "interfaces"));
			ret.put("testSuperClasses."+testCat, getDelimCollections(testClass, "superClasses"));
			List mockFields= (List) testClass.getOrDefault("mockFields", List.class);
			ret.put("mockFields."+testCat, mockFields.size());
			Map<String,String> anMap = (Map) testClass.getOrDefault("annotationsMap", Map.class);
			
			ret.put("runWith."+testCat, anMap.entrySet().stream().filter(p->p.getKey().contains("RunWith")).map(x->x.getValue()).collect(Collectors.joining("|")));
			int testConLOC = methodMetrics.stream().filter(p->Boolean.valueOf(p.getOrDefault("constructor", "false"))).mapToInt(x->Integer.valueOf(x.getOrDefault("loc", "0"))).sum();
			ret.put("testConstructors."+testCat, testConLOC);
			Set<String> exclMethods = Stream.concat(setupMethods.stream(), testMethods.stream()).map(x->String.valueOf(x.getOrDefault("simpleName", null))).collect(Collectors.toSet());
			List<Map<String, String>> normalMethods = methodMetrics.stream().filter(p->!exclMethods.contains(Helper.getSimpleMethodName(p.get("method")))).collect(Collectors.toList());
			ret.put("normalMethods."+testCat, normalMethods.size());
			ret.put("normalMethods.loc."+testCat, normalMethods.stream().mapToInt(x->Integer.valueOf(x.get("loc"))).sum());
		}
	}

	protected String getDelimCollections(Map testClass, String name) {
		Collection<String> anns = (Collection)testClass.getOrDefault(name, Collections.emptyList());
		String annots = anns.stream().collect(Collectors.joining("|"));
		return annots;
	}

	protected static int getLOC(Collection<Map<String, Object>> xs) {
		double setupLOC = xs.stream().mapToDouble(x->Double.valueOf(String.valueOf(x.getOrDefault("LOC", 0)))).sum();
		return (int) setupLOC;
	}

	public void addMetrics(File basedir, MetricsManager metricsManager) {
		try {
			File resultJsonFile = new File(basedir, "result.json");
			File classMetricsFile = new File(basedir, "class.csv");
			File methodMetricsFile = new File(basedir, "method.csv");
			if (!resultJsonFile.exists() || !classMetricsFile.exists()) {
				logger.info("File " + resultJsonFile + " found: " + resultJsonFile.exists());
				logger.info("File " + classMetricsFile + " found: " + classMetricsFile.exists());
				return;
			}
			doStuff(metricsManager, 
					resultJsonFile,
					classMetricsFile,
					methodMetricsFile
					);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}
