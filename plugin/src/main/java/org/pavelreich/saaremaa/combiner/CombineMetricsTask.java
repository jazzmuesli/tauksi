package org.pavelreich.saaremaa.combiner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.math3.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.pavelreich.saaremaa.CSVReporter;
import org.pavelreich.saaremaa.ForkableTestExecutor;
import org.pavelreich.saaremaa.ForkableTestLauncher;
import org.pavelreich.saaremaa.Helper;
import org.pavelreich.saaremaa.ForkableTestExecutor.TestExecutedMetrics;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import me.tongfei.progressbar.ProgressBar;

public class CombineMetricsTask {
	private static final String LOC_PROD = "loc.prod";
	private static final String PROD_COVERED_LINES = "coveredLines";

	private MongoDBClient db;
	private Logger logger;
	String projectId;
	private String usePomDirectories;
	private CKJMMetrics ckjmMetrics;
	private ProjectDirs projectDirs;

	public CombineMetricsTask(MongoDBClient db, 
			Logger logger, 
			ProjectDirs projDirs, 
			String projectId, 
			String usePomDirectories) {
		this.db = db;
		this.logger = logger;
		this.projectDirs = projDirs;
		this.projectId = projectId;
		this.usePomDirectories = usePomDirectories;
		this.ckjmMetrics = new CKJMMetrics(db,logger,projDirs,projectId,usePomDirectories);

	}

	public Logger getLog() {
		return logger;
	}

	public static void main(String[] args) {

		File basedir = new File("/Users/preich/Documents/git/poi");
		try {
			List<String> sourceDirFiles = Helper.findFiles(basedir.toString(),
					p -> p.getName().equals("sourceDirs.csv"));
			List<String> srcDirs = new ArrayList();
			List<String> testSrcDirs = new ArrayList();
			for (String f : sourceDirFiles) {
				System.out.println("reading " + f);
				CSVParser parser = Helper.getParser(new File(f), "processed");
				parser.getRecords().stream().filter(p -> p.get("processed").equals("true")).forEach(entry -> {
					if (entry.get("sourceSetName").contains("test")) {
						testSrcDirs.add(entry.get("dirName"));
					} else {
						srcDirs.add(entry.get("dirName"));
					}
				});
			}
			srcDirs.forEach(x -> System.out.println("src: " + x));
			testSrcDirs.forEach(x -> System.out.println("testSrc: " + x));
			Logger LOG = LoggerFactory.getLogger(CombineMetricsTask.class);
			String targetDir = basedir + File.separator + "target";
			new File(targetDir).mkdirs();
			MongoDBClient db = new MongoDBClient(CombineMetricsTask.class.getSimpleName());
			ProjectDirs projDirs = new ProjectDirs(basedir, targetDir, srcDirs, testSrcDirs, targetDir, targetDir);
			CombineMetricsTask task = new CombineMetricsTask(db, LOG, projDirs, "root: " + basedir.getName(),  "false");
			task.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void populateCK(MetricsManager metricsManager, Entry<String, Map<String, Long>> p) {
		String testClassName = p.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		Metrics m = metricsManager.provideMetrics(prodClassName);
		Map<String, Long> value = p.getValue();
		if (!testClassName.equals(prodClassName)) {
			String suffix = getSuffix(testClassName);
			value = value.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().replaceAll(".test", "") + suffix, e -> e.getValue()));
		}

		m.merge(value);
	}

	private void populateTMetrics(Pair<String, String> p, MetricsManager metricsManager) {
		String prodClassName = Helper.getProdClassName(p.getFirst());
		Metrics metrics = metricsManager.provideMetrics(prodClassName);
		String metricName = p.getSecond() + getSuffix(p.getFirst());
		metrics.incrementMetric(metricName);
	}

	static String getSuffix(String testClassName) {
		return "." + Helper.classifyTest(testClassName);
	}

	private List<Pair<String, String>> readTMetricPairs(String fname, String field) {
		if (!new File(fname).exists()) {
			return Collections.emptyList();
		}
		CSVParser parser;
		try {
			parser = Helper.getParser(fname, field);
			List<Pair<String, String>> pairs = parser.getRecords().stream().filter(p -> p.isSet(field))
					.map(x -> new Pair<String, String>(x.get("testClassName"), x.get(field)))
					.collect(Collectors.toList());
			parser.close();
			return pairs;
		} catch (Exception e) {
			getLog().error("Can't read " + fname + " due to  " + e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	protected Map<String, Map<String, Long>> readCKMetricPairs(String fname, String suffix) {
		if (!new File(fname).exists()) {
			return Collections.emptyMap();
		}
		CSVParser parser;
		try {
			parser = Helper.getParser(fname, "class");
			List<CSVRecord> records = parser.getRecords();
			getLog().info("found " + records.size() + " in file " + fname);
			Map<String, Map<String, Long>> ret = records.stream()
					.filter(p -> p.isConsistent() && "class".equals(p.get("type")))
					.collect(Collectors.toMap(x -> x.get("class"), x -> toCKMetrics(suffix, x)));
			return ret;
		} catch (Exception e) {
			getLog().error("Failed to readCKMetricPairs from " + fname + " due to " + e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	protected void printNonDataMethods(String dir) throws IOException {
		CSVParser fieldsParser = Helper.getParser(dir + "field.csv", "class");
		Map<String, List<CSVRecord>> fieldsByClass = fieldsParser.getRecords().stream()
				.collect(Collectors.groupingBy(x -> x.get("class")));
		CSVParser methodsParser = Helper.getParser(dir + "method.csv", "class");
		Map<String, List<CSVRecord>> methodsByClass = methodsParser.getRecords().stream()
				.collect(Collectors.groupingBy(x -> x.get("class")));
		// fields.forEach(f -> System.out.println(f));
//		methodsByClass.keySet().forEach(k->System.out.println(k));
		Map<String, Pair<Integer, Integer>> nonDataMethods = methodsByClass.entrySet().stream().collect(Collectors
				.toMap(e -> e.getKey(), e -> calculateNonDataMethodsCount(fieldsByClass, e, new Metrics(e.getKey()))));
		nonDataMethods.entrySet().stream().filter(p -> p.getValue().getSecond() == 0)
				.forEach(e -> System.out.println(e));
	}

	protected void populateCKJM(MetricsManager metricsManager, Entry<String, Map<String, Long>> p) {
		String testClassName = p.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		Metrics m = metricsManager.provideMetrics(prodClassName);
		Map<String, Long> value = p.getValue();
		if (!testClassName.equals(prodClassName)) {
			String suffix = getSuffix(testClassName);
			value = value.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().replaceAll(".test", "") + suffix, e -> e.getValue()));
		}

		m.merge(value);
	}

	
	private static Pair<Integer, Integer> calculateNonDataMethodsCount(Map<String, List<CSVRecord>> fieldsByClass,
			Entry<String, List<CSVRecord>> methodEntry, Metrics m) {
		String className = methodEntry.getKey();
		Set<String> fields = fieldsByClass.getOrDefault(className, Collections.emptyList()).stream()
				.map(x -> x.get("variable").toLowerCase()).collect(Collectors.toSet());
		List<String> methods = methodEntry.getValue().stream().filter(p -> "false".equals(p.get("constructor")))
				.map(x -> x.get("method")).collect(Collectors.toList());
		List<Optional<String>> setters = Helper.getMethods(methods, "^set.*", "(set)");
		List<Optional<String>> getters = Helper.getMethods(methods, "^(get|is).*", "(get|is)");
		Collection<String> dataMethods = Helper.calculateDataMethods(fields, methods, setters, getters);
		String simpleClassName = className.replaceAll(".*?\\.([^\\.]+)$", "$1");
		List<CSVRecord> constructors = methodEntry.getValue().stream()
				.filter(p -> p.isConsistent() && simpleClassName.equals(p.get("method").replaceAll("(.*?)/.*", "$1")))
				.collect(Collectors.toList());
		if (!constructors.isEmpty()) {
			OptionalInt maxConstructorArgs = constructors.stream().mapToInt(x -> Integer.valueOf(x.get("parametersQty")))
					.max();
			if (maxConstructorArgs.isPresent()) {
				m.put("prod.maxConstructorArgs", Long.valueOf(maxConstructorArgs.getAsInt()));
			}

		}
		m.put("prod.dataMethods", Long.valueOf(dataMethods.size()));
		long dataMethodsRatio = 0;
		if (!methods.isEmpty() && !dataMethods.isEmpty()) {
			dataMethodsRatio = Math.round(100 * Double.valueOf(dataMethods.size() / Double.valueOf(methods.size())));
		}

		m.put("prod.dataMethodsRatio", dataMethodsRatio);
		m.put("prod.methods", Long.valueOf(methods.size()));
		m.put("prod.setters", Long.valueOf(setters.stream().filter(p -> p.isPresent()).count()));
		m.put("prod.getters", Long.valueOf(getters.stream().filter(p -> p.isPresent()).count()));
		List<String> nonDataMethods = methods.stream().filter(p -> !dataMethods.contains(p))
				.collect(Collectors.toList());
		return new Pair<>(methods.size(), nonDataMethods.size());
	}

	public static final List<String> CK_METRICS = Arrays.asList("cbo", "wmc", "rfc", "loc", "lcom", "dit",
			"anonymousClassesQty", "subClassesQty", "lambdasQty", "totalFields", "totalMethods");

	private Map<String, Long> toCKMetrics(String suffix, CSVRecord r) {
		Map<String, Long> ret = new HashMap<String, Long>();
		for (String metricName : CK_METRICS) {
			if (r.isConsistent() && r.isSet(metricName)) {
				String val = r.get(metricName);
				if (val != null && !val.trim().isEmpty()) {
					try {
						ret.put(metricName + suffix, Long.valueOf(val));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}

				}

			}
		}
		return ret;
//		return .stream()
//				.filter(p->r.isSet(p) && !"".equals(r.get(p).trim()))
//				.collect(Collectors.toMap(k -> k + suffix, k -> Long.valueOf(r.get(k))));
	}

	public void execute() {
		try {

			Map<String, Metrics> metricsByProdClass = new HashMap<String, Metrics>();
			MetricsManager metricsManager = new MetricsManager(metricsByProdClass);

			addTMetrics(projectDirs.basedir.getAbsolutePath(), metricsManager);
			addTNOO(metricsManager);
			addProdCKmetrics(metricsManager);
			addTestCKmetrics(metricsManager);
			ckjmMetrics.addProdCKJMmetrics(metricsManager);
			ckjmMetrics.addTestCKJMmetrics(metricsManager);
			addTestability(metricsManager);
			TestanResultParser testanResultParser = new TestanResultParser(logger);
			for (String dirName : Helper.extractDirs(projectDirs.testSrcDirs)) {
				File dir = new File(dirName);
				if (dir.isDirectory()) {
					testanResultParser.addMetrics(dir, metricsManager);
				}
			}

			new File(projectDirs.targetDirectory).mkdirs();
			String fname = projectDirs.targetDirectory + File.separator + "metrics.csv";
			List<Document> docs = new ArrayList<Document>();
			metricsByProdClass.values().forEach(metrics -> {
				if (metrics.longMetrics.containsKey(PROD_COVERED_LINES) && metrics.longMetrics.containsKey(LOC_PROD)) {
					long ratio = 100 * metrics.longMetrics.get(PROD_COVERED_LINES) / metrics.longMetrics.get(LOC_PROD);
					metrics.put("prod.maxCovratio", ratio);
				}
			});
			CSVReporter reporter = new CSVReporter(fname, Metrics.getFields());
			for (Entry<String, Metrics> entry : metricsByProdClass.entrySet()) {
				Metrics metrics = entry.getValue();
				if (!metrics.longMetrics.containsKey(LOC_PROD)
						|| metrics.prodClassName.contains("ESTest_scaffolding")) {
					continue;
				}

				Document doc = metrics.toDocument();
				doc.append("project", projectId).append("basedir", projectDirs.basedir.toString());
				docs.add(doc);
				reporter.write(metrics.getValues());
			}
			reporter.close();

			int docsLength = docs == null ? 0 : docs.size();
			getLog().info("Generated " + fname + " and insert " + docsLength + " into mongo");
			if (docsLength > 0) {
				db.insertCollection("combinedMetrics", docs);
				db.waitForOperationsToFinish();
			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	private void addTestability(MetricsManager metricsManager) throws IOException {
		List<String> files = Helper.findFiles(projectDirs.basedir.getAbsolutePath(), p -> p.getName().equals("testability.csv"));
		files.forEach(fileName -> {
			try {
				CSVParser parser = Helper.getParser(fileName, "className");
				parser.getRecords().forEach(record -> {
					String prodClassName = record.get("className");
					Metrics metrics = metricsManager.provideMetrics(prodClassName);
					Long cost = Long.valueOf(record.get("cost"));
					metrics.put("testabilityCost", cost);
				});
			} catch (Exception e) {
				getLog().error("Can't parse " + fileName + " due to  " + e.getMessage(), e);
			}
		});

	}

	protected void addTestCKmetrics(MetricsManager metricsManager) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : Helper.extractDirs(projectDirs.testSrcDirs)) {
				files.addAll(Helper.findFiles(dir, p -> p.getName().equals("class.csv")));
			}
		} else {
			files = Helper.findFiles(projectDirs.basedir.getAbsolutePath(), p -> p.getName().equals("class.csv"));
		}
		files.forEach(fileName -> {
			Map<String, Map<String, Long>> allTestCKMetrics = readCKMetricPairs(fileName, ".test");
			allTestCKMetrics.entrySet().stream()
					.filter(f -> Helper.isTest(f.getKey()) && !f.getKey().contains("ESTest_scaffolding")).// ignore
			// evosuite
			forEach(p -> populateCK(metricsManager, p));
		});
	}

	protected void addProdCKmetrics(MetricsManager metricsManager) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : projectDirs.srcDirs) {
				files.addAll(Helper.findFiles(dir, p -> p.getName().equals("class.csv")));
			}
		} else {
			files = Helper.findFiles(projectDirs.basedir.getAbsolutePath(), p -> p.getName().equals("class.csv"));
		}

		files.forEach(file -> {
			Map<String, Map<String, Long>> prodCKMetrics = readCKMetricPairs(file, ".prod");
			prodCKMetrics.entrySet().stream().filter(p -> !Helper.isTest(p.getKey()))
					.forEach(p -> populateCK(metricsManager, p));

			try {
				String fieldFname = file.replaceAll("class.csv", "field.csv");
				CSVParser fieldsParser = Helper.getParser(fieldFname, "class");
				List<CSVRecord> fieldRecors = fieldsParser.getRecords();
				getLog().info("Read " + fieldRecors.size() + " from  " + fieldFname);
				Map<String, List<CSVRecord>> fieldsByClass = fieldRecors.stream()
						.collect(Collectors.groupingBy(x -> x.get("class")));
				String methodFname = file.replaceAll("class.csv", "method.csv");
				CSVParser methodsParser = Helper.getParser(methodFname, "class");
				List<CSVRecord> methodRecords = methodsParser.getRecords();
				getLog().info("Read " + methodRecords.size() + " from  " + methodFname);
				Map<String, List<CSVRecord>> methodsByClass = methodRecords.stream()
						.collect(Collectors.groupingBy(x -> x.get("class")));
				methodsByClass.entrySet().forEach(methodEntry -> calculateNonDataMethodsCount(fieldsByClass,
						methodEntry, metricsManager.provideMetrics(methodEntry.getKey())));
			} catch (Exception e) {
				getLog().error("Failed to parse " + file + " due to " + e.getMessage(), e);
			}

		});
		// fields.forEach(f -> System.out.println(f));
//		methodsByClass.keySet().forEach(k->System.out.println(k));
//		Map<String, Pair<Integer, Integer>> nonDataMethods = methodsByClass.entrySet().stream()
//				.collect(Collectors.toMap(e -> e.getKey(), e -> calculateNonDataMethodsCount(fieldsByClass, e, new Metrics(e.getKey()))));
//		nonDataMethods.entrySet().stream().filter(p -> p.getValue().getSecond() == 0)
//				.forEach(e -> System.out.println(e));

	}
	


	protected void addTMetrics(String dir, MetricsManager metricsManager) throws IOException {
		List<String> files = Helper.findFiles(dir, (p) -> p.getName().endsWith("-tmetrics.csv"));

		List<Pair<String, String>> pairs = new ArrayList();
		getLog().info("Found " + files.size() + " files in dir=" + dir);
		files.forEach(file -> pairs.addAll(readTMetricPairs(file, "metricType")));
		Set<Path> paths = files.stream().map(file -> new File(file).getAbsoluteFile().getParentFile().toPath())
				.collect(Collectors.toSet());
		Map<ClassMethodKey, CSVRecord> methodMetrics = new HashMap();
		paths.forEach(path -> methodMetrics.putAll(loadMethodMetrics(path)));
		for (String file : ProgressBar.wrap(files, "coverageMetrics")) {
			addCoverageMetrics(methodMetrics, file, metricsManager);
		}

		getLog().info("Generated " + pairs.size() + " from " + files.size() + " files");
		pairs.forEach(p -> populateTMetrics(p, metricsManager));
	}

	protected Map<String, Integer> sumLinesByClass(Collection<Document> ret, String linesName) {
		Map<String, Integer> map = ret.stream().collect(Collectors.<Document, String, Integer>toMap(
				doc -> doc.getString("prodClassName"), val -> val.getInteger(linesName, 0), (a, b) -> a + b));
		return map;
	}

	class DocumentManager {
		final String collectionName;

		public DocumentManager(String colName) {
			this.collectionName = colName;
		}

		Multimap<String, Document> map = ArrayListMultimap.create();

		Collection<Document> findDocumentsBySessionId(String sessionId) {
			Collection<Document> found = map.get(sessionId);
			if (found != null && !found.isEmpty()) {
				return found;
			}
			Bson query = com.mongodb.client.model.Filters.eq("sessionId", sessionId);
			List<Document> docs = db.find(collectionName, query);
			Set<String> ids = docs.stream().map(doc -> doc.getString("id")).filter(p -> p != null)
					.collect(Collectors.toSet());
			fetch(ids);
			return docs;
		}

		protected void fetch(Set<String> ids) {
			if (!ids.isEmpty()) {
				Bson query = com.mongodb.client.model.Filters.in("id", ids);
				List<Document> docs = db.find(collectionName, query);
				getLog().info("Fetching " + collectionName + " using id=" + ids + ", found: " + docs.size());
				for (Document doc : docs) {
					map.put(doc.getString("sessionId"), doc);
				}
			}
		}
	}

	private final DocumentManager testsLaunchedManager = new DocumentManager(
			ForkableTestLauncher.TESTS_LAUNCHED_COL_NAME);
	private final DocumentManager testsExecutedManager = new DocumentManager(
			ForkableTestExecutor.TESTS_EXECUTED_COL_NAME);
	private final DocumentManager classCoverageManager = new DocumentManager(
			ForkableTestLauncher.CLASS_COVERAGE_COL_NAME);
	private final DocumentManager methodCoverageManager = new DocumentManager(
			ForkableTestLauncher.METHOD_COVERAGE_COL_NAME);

	private void addCoverageMetrics(Map<ClassMethodKey, CSVRecord> methodMetrics, String file,
			MetricsManager metricsManager) {
		File f = new File(file);
		String sessionId = f.getName().replaceAll("-tmetrics.csv", "");
//		getLog().info("Processing sessionId=" + sessionId);
		Collection<Document> testsLaunched = testsLaunchedManager.findDocumentsBySessionId(sessionId);

		Map<String, Document> testClassNames = testsLaunched.stream()
				.collect(Collectors.toMap(x -> x.getString("testClassName"), x -> x));
		if (testClassNames.size() != 1) {
			getLog().warn("Found " + testClassNames.keySet() + " testsLaunched for  " + sessionId);
			return;
		}
		Collection<Document> testsExecuted = testsExecutedManager.findDocumentsBySessionId(sessionId);

		Map<String, Document> testsExecutedDocs = testsExecuted.stream()
				.collect(Collectors.toMap(x -> x.getString("testClassName"), x -> x));

		Entry<String, Document> testLaunched = testClassNames.entrySet().iterator().next();
		String testClassName = testLaunched.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		Collection<Document> classCoverage = classCoverageManager.findDocumentsBySessionId(sessionId);
		Collection<Document> methodCoverage = methodCoverageManager.findDocumentsBySessionId(sessionId);
		Metrics metrics = metricsManager.provideMetrics(prodClassName);

		String testCat = Helper.classifyTest(testClassName);

		Document doc = testsExecutedDocs.get(testClassName);
		if (doc != null) {
			for (ForkableTestExecutor.TestExecutedMetrics metric : TestExecutedMetrics.values()) {
				Long val = doc.getLong(metric.name());
				if (val != null) {
					metrics.put(metric.name() + "." + testCat, val);
				}
			}
		}

//		getLog().info("Found " + classCoverage.size() + " classCoverage docs for " + sessionId);
//		getLog().info("Found " + methodCoverage.size() + " methodCoverage docs for " + sessionId);
		Map<String, Integer> coveredLines = sumLinesByClass(classCoverage, "coveredLines");
		Map<String, Integer> missedLines = sumLinesByClass(classCoverage, "missedLines");
		long prodClassesCovered = coveredLines.values().stream().filter(p -> p > 0).count();
		Map<String, Double> covratio = coveredLines.entrySet().stream()
				.collect(Collectors.<Entry<String, Integer>, String, Double>toMap(k -> k.getKey(),
						v -> Double.valueOf(v.getValue())
								/ Double.valueOf(v.getValue() + missedLines.getOrDefault(v.getKey(), 0))));
		long coverageRatio = Math.round(covratio.getOrDefault(prodClassName, 0.0) * 100);
		getLog().info("Test " + testClassName + "  covered " + prodClassesCovered + " prod classes and " + prodClassName
				+ " with " + coverageRatio + " for sessionId= " + sessionId);

		metrics.put("prodClassesCovered." + testCat, prodClassesCovered);
		if ("evo".equals(testCat)) {
			metrics.evoSessionId = sessionId;
		} else {
			metrics.testSessionId = sessionId;
		}

		metrics.put("covratio." + testCat, coverageRatio);
		Integer coveredLinesByThisTest = coveredLines.getOrDefault(prodClassName, 0);
		long covLines = Math.max(coveredLinesByThisTest, metrics.longMetrics.getOrDefault(PROD_COVERED_LINES, 0L));
		metrics.put(PROD_COVERED_LINES + "." + testCat, covLines);
		try {
			calculateQualityIndex(methodMetrics, testCat, methodCoverage, metrics);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	/**
	 * based on toure2018predicting
	 * 
	 * @param tmetricsFile
	 * @param methodCoverage
	 * @param metrics
	 * @return
	 * @throws IOException
	 */
	protected double calculateQualityIndex(Map<ClassMethodKey, CSVRecord> methodMetrics, String testCat,
			Collection<Document> methodCoverage, Metrics metrics) throws IOException {
//		getLog().info("from absoluteFile=" + absoluteFile + " found " + files);
		BinaryOperator<Document> mergeDocuments = (a, b) -> {
			a.putAll(b);
			return a;
		};
		Map<ClassMethodKey, Document> methodCoverageMap = methodCoverage.stream()
				.collect(Collectors.toMap(k -> ClassMethodKey.fromMethodCoverage(k), v -> v, mergeDocuments));
//		methodCoverageMap.keySet().forEach(x -> getLog().info("methodCoverage: " + x));
//		methodMetrics.keySet().forEach(x -> getLog().info("methodMetrics: " + x));
		Map<String, Long> maxComplexityPerClass = calculateMaxComplexityPerClass(methodMetrics);
		Map<ClassMethodKey, Double> iqai = methodMetrics.keySet().stream().filter(p -> methodCoverageMap.containsKey(p))
				.collect(Collectors.toMap(k -> k,
						x -> iqai(maxComplexityPerClass, methodCoverageMap.get(x), methodMetrics.get(x))));
		// iqai.entrySet().forEach(x -> getLog().info("iqai: " + x));
		BinaryOperator<Double> mergeFunction = (a, b) -> a * b;
		Map<String, Double> classIqai = iqai.entrySet().stream()
				.collect(Collectors.toMap(k -> k.getKey().className, v -> v.getValue(), mergeFunction));
		metrics.put("iqai." + testCat, (long) (classIqai.getOrDefault(metrics.prodClassName, (double) -1) * 100));
		double qi = 1.0;
		return qi;
	}

	protected Map<ClassMethodKey, CSVRecord> loadMethodMetrics(Path path) {
		List<String> files;
		try {
			files = java.nio.file.Files.walk(path).filter(p -> p.toFile().getName().endsWith("method.csv"))
					.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
			Map<ClassMethodKey, CSVRecord> methodMetrics = new HashMap<>();
			for (String f : files) {
				CSVParser parser = Helper.getParser(f, "class");
				List<CSVRecord> records = parser.getRecords();
				Map<ClassMethodKey, CSVRecord> map = records.stream()
						.collect(Collectors.toMap(k -> ClassMethodKey.fromMethodMetric(k), v -> v));
//				getLog().info("found "+ records.size() + " for file=" + f);
				methodMetrics.putAll(map);
			}
			return methodMetrics;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	private double iqai(Map<String, Long> maxComplexityPerClass, Document document, CSVRecord csvRecord) {
		double wmc = Double.valueOf(csvRecord.get("wmc"));
		double coverage = Double.valueOf(document.getInteger("coveredLines", 0))
				/ Double.valueOf((document.getInteger("missedLines", 0) + document.getInteger("coveredLines", 0)));
		double maxComplexity = Double.valueOf(maxComplexityPerClass.get(csvRecord.get("class")));
		double qi = (1.0 - coverage) * wmc / maxComplexity;
		return 1.0 - qi;
	}

	protected Map<String, Long> calculateMaxComplexityPerClass(Map<ClassMethodKey, CSVRecord> methodMetrics) {
		List<Pair<String, Long>> classWithWMC = methodMetrics.values().stream()
				.map(v -> new Pair<String, Long>(v.get("class"), Long.valueOf(v.get("wmc"))))
				.collect(Collectors.toList());
		Map<String, List<Long>> wmcByClass = classWithWMC.stream().collect(
				Collectors.groupingBy(k -> k.getKey(), Collectors.mapping(Pair::getValue, Collectors.toList())));
		Map<String, Long> maxWMCByClass = wmcByClass.entrySet().stream()
				.collect(Collectors.toMap(k -> k.getKey(), v -> Collections.max(v.getValue())));
		return maxWMCByClass;
	}

	static class ClassMethodKey {

		private String className;
		private String method;
		private int firstLine;

		static ClassMethodKey fromMethodCoverage(Document d) {
			ClassMethodKey classMethod = new ClassMethodKey(d.getString("prodClassName"), d.getString("prodMethodName"),
					d.getInteger("firstLine", 0));
			return classMethod;
		}

		static ClassMethodKey fromMethodMetric(CSVRecord r) {
			ClassMethodKey classMethod = new ClassMethodKey(r.get("class"), r.get("method").replaceAll("/.*", ""),
					Integer.valueOf(r.get("line")) + 1);
			return classMethod;
		}

		private ClassMethodKey(String className, String method, int firstLine) {
			this.className = className;
			this.method = method;
			this.firstLine = firstLine;
		}

		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		@Override
		public boolean equals(Object that) {
			return EqualsBuilder.reflectionEquals(this, that);
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

	}

	protected void addTNOO(MetricsManager metricsManager) {
		Helper.extractDirs(projectDirs.testSrcDirs).forEach(dirName -> {
			List<Pair<String, String>> testCases = readTMetricPairs(dirName + File.separator + "testcases.csv",
					"testCaseName");
			testCases.stream().filter(p -> !p.getSecond().equals("TOTAL")).forEach(p -> {
				String prodClassName = Helper.getProdClassName(p.getFirst());
				Metrics m = metricsManager.provideMetrics(prodClassName);
				String metricName = "TNOO" + getSuffix(p.getFirst());
				m.incrementMetric(metricName);
			});

		});
	}

}
