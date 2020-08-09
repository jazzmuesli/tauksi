package org.pavelreich.saaremaa.combiner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.pavelreich.saaremaa.Helper;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;

//TODO: extract CKMetrics too and generalise.
public class CKJMMetrics {

	private static final String CLASS_FIELD = "CLASS";
	//ckjm
	private Logger logger;
	String projectId;
	private String usePomDirectories;
	private ProjectDirs projectDirs;
	private Collection<String> srcDirs;
	private Collection<String>testSrcDirs;

	public CKJMMetrics(MongoDBClient db, Logger logger, ProjectDirs projectDirs, String projectId,
			String usePomDirectories) {
		this.logger = logger;
		this.projectDirs=projectDirs;
		this.projectId = projectId;
		this.srcDirs = projectDirs.mainOutputDirs;
		this.testSrcDirs = projectDirs.testOutputDirs;
		this.usePomDirectories = usePomDirectories;
	}

	public Logger getLog() {
		return logger;
	}

	protected Map<String, Map<String, Long>> readCKJMMetricPairs(String fname, String prefix) {
		if (!new File(fname).exists()) {
			return Collections.emptyMap();
		}
		CSVParser parser;
		try {
			parser = Helper.getParser(fname, CLASS_FIELD);
			List<CSVRecord> records = parser.getRecords();
			getLog().info("found " + records.size() + " in file " + fname);
			Map<String, Map<String, Long>> ret = records.stream()
					.filter(p -> p.isConsistent())// && "class".equals(p.get("type")))
					.collect(Collectors.toMap(x -> x.get(CLASS_FIELD), x -> toCKJMMetrics(prefix, x)));
			return ret;
		} catch (Exception e) {
			getLog().error("Failed to readCKMetricPairs from " + fname + " due to " + e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	
	private void populateCKJM(MetricsManager metricsManager, Entry<String, Map<String, Long>> p) {
		String testClassName = p.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		Metrics m = metricsManager.provideMetrics(prodClassName);
		Map<String, Long> value = p.getValue();
		if (!testClassName.equals(prodClassName)) {
			String suffix = CombineMetricsTask.getSuffix(testClassName);
			value = value.entrySet().stream()
					.collect(Collectors.toMap(e -> "T." + e.getKey().replaceAll("^T.", ""), e -> e.getValue()));
		}

		m.merge(value);
	}
	protected void addTestCKJMmetrics(MetricsManager metricsManager) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : Helper.extractDirs(testSrcDirs)) {
				files.addAll(Helper.findFiles(dir, p -> p.getName().equals("ckjm.csv")));
			}
		} else {
			files = Helper.findFiles(projectDirs.basedir.getAbsolutePath(), p -> p.getName().equals("ckjm.csv"));
		}
		getLog().info("Found ckjm test files: " + files);
		files.forEach(fileName -> {
			Map<String, Map<String, Long>> allTestCKMetrics = readCKJMMetricPairs(fileName, "T.");
			allTestCKMetrics.entrySet().stream()
					.filter(f -> Helper.isTest(f.getKey()) && "test".equals(Helper.classifyTest(f.getKey())) && !f.getKey().contains("ESTest_scaffolding")).// ignore
			// evosuite
			forEach(p -> populateCKJM(metricsManager, p));
		});
	}

	
	protected void addProdCKJMmetrics(MetricsManager metricsManager) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : srcDirs) {
				files.addAll(Helper.findFiles(dir, p -> p.getName().equals("ckjm.csv")));
			}
		} else {
			files = Helper.findFiles(projectDirs.basedir.getAbsolutePath(), p -> p.getName().equals("ckjm.csv"));
		}

		getLog().info("Found ckjm prod files: " + files);
		files.forEach(file -> {
			Map<String, Map<String, Long>> prodCKMetrics = readCKJMMetricPairs(file, "");
			prodCKMetrics.entrySet().stream().filter(p -> !Helper.isTest(p.getKey()))
					.forEach(p -> populateCKJM(metricsManager, p));
		});
	}
	
	public static final List<String> CK_METRICS = Arrays.asList("AMC;CA;CAM;CBM;CBO;CE;DAM;DIT;IC;LCOM;LCOM3;LOC;MFA;MOA;NOC;NPM;RFC;WMC".split(";"));

	private Map<String, Long> toCKJMMetrics(String prefix, CSVRecord r) {
		Map<String, Long> ret = new HashMap<String, Long>();
		for (String metricName : CK_METRICS) {
			if (r.isConsistent() && r.isSet(metricName)) {
				String val = r.get(metricName);
				if (val != null && !val.trim().isEmpty()) {
					try {
						ret.put(prefix + metricName, Double.valueOf(val).longValue());
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}

				}

			}
		}
		return ret;
	}


}
