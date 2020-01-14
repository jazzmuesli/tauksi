package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.print.Doc;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.math3.util.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.pavelreich.saaremaa.ForkableTestExecutor.TestExecutedMetrics;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

import com.github.mauricioaniche.ck.plugin.CKMetricsMojo;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import me.tongfei.progressbar.ProgressBar;

@Mojo(name = "combine-metrics", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.NONE)
public class CombineMetricsMojo extends AbstractMojo {
	private static final String LOC_PROD = "loc.prod";
	private static final String PROD_COVERED_LINES = "coveredLines";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
	
	@Parameter( property = "ignoreChildProjects", defaultValue = "true")
	private String ignoreChildProjects;
	
	@Parameter( property = "usePomDirectories", defaultValue = "false")
	private String usePomDirectories;

	public CombineMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
	}

	static class Metrics {
		static Set<String> fields = new LinkedHashSet<>();
		private String prodClassName;

		Map<String, Long> longMetrics = new HashMap<String, Long>();
		String testSessionId = "";
		String evoSessionId = "";

		Metrics(String prodClassName) {
			this.prodClassName = prodClassName;
		}

		public void incrementMetric(String metricName) {
			fields.add(metricName);
			longMetrics.putIfAbsent(metricName, 0L);
			longMetrics.put(metricName, longMetrics.get(metricName) + 1);
		}

		void merge(Map<String, Long> m) {
			fields.addAll(m.keySet());
			longMetrics.putAll(m);
		}

		void put(String metricName, Long n) {
			fields.add(metricName);
			longMetrics.put(metricName, n);
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

		public static String[] getFields() {
			List<String> vals = new ArrayList();
			vals.add("prodClassName");
			vals.add("testSessionId");
			vals.add("evoSessionId");
			vals.addAll(fields);
			return vals.toArray(new String[0]);
		}

		public String[] getValues() {
			List<String> vals = new ArrayList();
			vals.add(prodClassName);
			vals.add(testSessionId);
			vals.add(evoSessionId);
			vals.addAll(fields.stream().map(f -> longMetrics.containsKey(f) ? String.valueOf(longMetrics.get(f)) : "")
					.collect(Collectors.toList()));
			return vals.toArray(new String[0]);
		}

		public Document toDocument() {
			Document doc = new Document("prodClassName", prodClassName);
			longMetrics.entrySet().forEach(e -> doc.append(e.getKey().replace('.', '_'), e.getValue()));
			return doc;
		}

	}

	protected void populateCK(Map<String, Metrics> metricsByProdClass, Entry<String, Map<String, Long>> p) {
		String testClassName = p.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
		Metrics m = metricsByProdClass.get(prodClassName);
		Map<String, Long> value = p.getValue();
		if (!testClassName.equals(prodClassName)) {
			String suffix = getSuffix(testClassName);
			value = value.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().replaceAll(".test", "") + suffix, e -> e.getValue()));
		}

		m.merge(value);
	}

	private void populateTMetrics(Pair<String, String> p, Map<String, Metrics> metricsByProdClass) {
		String prodClassName = Helper.getProdClassName(p.getFirst());
		metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
		String metricName = p.getSecond() + getSuffix(p.getFirst());
		metricsByProdClass.get(prodClassName).incrementMetric(metricName);
	}

	private String getSuffix(String testClassName) {
		String suffix = testClassName.endsWith("_ESTest") ? ".evo" : ".test";
		return suffix;
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
			Map<String, Map<String, Long>> ret = parser.getRecords().stream()
					.filter(p-> "class".equals(p.get("type")))
					.collect(Collectors.toMap(x -> x.get("class"), x -> toCKMetrics(suffix, x)));
			return ret;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	public static void main(String[] args) {
		CombineMetricsMojo mojo = new CombineMetricsMojo();
		try {
			List<String> files = mojo.findFiles("/Users/preich/Documents/github/jfreechart", 
					p->p.getName().contains("field.csv")).stream().map(x->x.replaceAll("field.csv","")).
					collect(Collectors.toList());
			for (String dir : files) {
				mojo.printNonDataMethods(dir);				
			}
//			String dir = "/Users/preich/Documents/github/jfreechart/src/main/java/";
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		Map<String, Pair<Integer, Integer>> nonDataMethods = methodsByClass.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> calculateNonDataMethodsCount(fieldsByClass, e, new Metrics(e.getKey()))));
		nonDataMethods.entrySet().stream().filter(p -> p.getValue().getSecond() == 0)
				.forEach(e -> System.out.println(e));
	}

	private static Pair<Integer, Integer> calculateNonDataMethodsCount(Map<String, List<CSVRecord>> fieldsByClass,
			Entry<String, List<CSVRecord>> methodEntry, Metrics m) {
		String className = methodEntry.getKey();
		Set<String> fields = fieldsByClass.getOrDefault(className, Collections.emptyList()).stream()
				.map(x -> x.get("variable").toLowerCase()).collect(Collectors.toSet());
		List<String> methods = methodEntry.getValue().stream().filter(p->"false".equals(p.get("constructor"))).map(x -> x.get("method")).collect(Collectors.toList());
		List<Optional<String>> setters = Helper.getMethods(methods, "^set.*", "(set)");
		List<Optional<String>> getters = Helper.getMethods(methods, "^(get|is).*", "(get|is)");
		Collection<String> dataMethods = Helper.calculateDataMethods(fields, methods, setters, getters);
		String simpleClassName = className.replaceAll(".*?\\.([^\\.]+)$", "$1");
		List<CSVRecord> constructors = methodEntry.getValue().stream().filter(p->simpleClassName.equals(p.get("method").replaceAll("(.*?)/.*", "$1"))).collect(Collectors.toList());
		if (!constructors.isEmpty()) {
			OptionalInt maxConstructorArgs = constructors.stream().mapToInt(x -> Integer.valueOf(x.get("parameters"))).max();
			if (maxConstructorArgs.isPresent()) {
				m.put("prod.maxConstructorArgs", Long.valueOf(maxConstructorArgs.getAsInt()));				
			}
			
		}
		m.put("prod.dataMethods", Long.valueOf(dataMethods.size()));
		long dataMethodsRatio = 0;
		if (!methods.isEmpty() && !dataMethods.isEmpty()) {
			dataMethodsRatio = Math.round(100*Double.valueOf(dataMethods.size()/Double.valueOf(methods.size())));
		}
		
		m.put("prod.dataMethodsRatio", dataMethodsRatio);
		m.put("prod.methods", Long.valueOf(methods.size()));
		m.put("prod.setters", Long.valueOf(setters.stream().filter(p->p.isPresent()).count()));
		m.put("prod.getters", Long.valueOf(getters.stream().filter(p->p.isPresent()).count()));
		List<String> nonDataMethods = methods.stream().filter(p -> !dataMethods.contains(p)).collect(Collectors.toList());
		return new Pair<>(methods.size(), nonDataMethods.size());
	}


	
	private Map<String, Long> toCKMetrics(String suffix, CSVRecord r) {
		return Arrays.asList("cbo", "wmc", "rfc", "loc", "lcom","dit").stream()
				.collect(Collectors.toMap(k -> k + suffix, k -> Long.valueOf(r.get(k))));
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (Boolean.valueOf(ignoreChildProjects) && project.getParent() != null) {
				getLog().info("Ignoring child project " + project);
				return;
			}
			getLog().info("modules: " + project.getModules());
			Map<String, Metrics> metricsByProdClass = new HashMap<String, Metrics>();
			addTMetrics(project.getBasedir().getAbsolutePath(), metricsByProdClass);
			addTNOO(metricsByProdClass);
			addProdCKmetrics(metricsByProdClass);
			addTestCKmetrics(metricsByProdClass);
			addTestability(metricsByProdClass);

			String directory = project.getBuild().getDirectory();
			new File(directory).mkdirs();
			String fname = directory + File.separator + "metrics.csv";
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
				if (!metrics.longMetrics.containsKey(LOC_PROD) || metrics.prodClassName.contains("ESTest_scaffolding")) {
					continue;
				}

				Document doc = metrics.toDocument();
				doc.append("project", project.getArtifact().getId()).append("basedir", project.getBasedir().toString());
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

	private void addTestability(Map<String, Metrics> metricsByProdClass) throws IOException {
		List<String> files = findFiles(project.getBasedir().getAbsolutePath(),
				p -> p.getName().equals("testability.csv"));
		files.forEach(fileName -> {
			try {
				CSVParser parser = Helper.getParser(fileName, "className");
				parser.getRecords().forEach(record -> {
					String prodClassName = record.get("className");
					Metrics metrics = getMetrics(metricsByProdClass, prodClassName);
					Long cost = Long.valueOf(record.get("cost"));
					metrics.put("testabilityCost", cost);
				});
			} catch (Exception e) {
				getLog().error("Can't parse " + fileName + " due to  " + e.getMessage(), e);
			}
		});

	}

	protected void addTestCKmetrics(Map<String, Metrics> metricsByProdClass) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : CKMetricsMojo.extractDirs(project.getTestCompileSourceRoots())) {
				files.addAll(findFiles(dir, p -> p.getName().equals("class.csv")));
			}
		} else {
			files = findFiles(project.getBasedir().getAbsolutePath(), p -> p.getName().equals("class.csv"));
		}
		files.forEach(fileName -> {
			Map<String, Map<String, Long>> allTestCKMetrics = readCKMetricPairs(fileName,
					".test");
			allTestCKMetrics.entrySet().stream().filter(f -> Helper.isTest(f.getKey()) && !f.getKey().contains("ESTest_scaffolding")).// ignore
																											// evosuite
			forEach(p -> populateCK(metricsByProdClass, p));
		});
	}

	protected void addProdCKmetrics(Map<String, Metrics> metricsByProdClass) throws IOException {
		List<String> files = new ArrayList<String>();
		if (Boolean.valueOf(usePomDirectories)) {
			for (String dir : project.getCompileSourceRoots()) {
				files.addAll(findFiles(dir, p -> p.getName().equals("class.csv")));
			}
		} else {
			files = findFiles(project.getBasedir().getAbsolutePath(), p -> p.getName().equals("class.csv"));
		}

		files.forEach(file -> {
			Map<String, Map<String, Long>> prodCKMetrics = readCKMetricPairs(file, ".prod");
			prodCKMetrics.entrySet().stream().filter(p-> !Helper.isTest(p.getKey())).
			forEach(p -> populateCK(metricsByProdClass, p));
			
			try {
				CSVParser fieldsParser = Helper.getParser(file.replaceAll("class.csv", "field.csv"), "class");
				Map<String, List<CSVRecord>> fieldsByClass = fieldsParser.getRecords().stream()
						.collect(Collectors.groupingBy(x -> x.get("class")));
				CSVParser methodsParser = Helper.getParser(file.replaceAll("class.csv", "method.csv"), "class");
				Map<String, List<CSVRecord>> methodsByClass = methodsParser.getRecords().stream()
						.collect(Collectors.groupingBy(x -> x.get("class")));
				methodsByClass.entrySet().forEach(methodEntry -> 
				calculateNonDataMethodsCount(fieldsByClass, methodEntry, metricsByProdClass.computeIfAbsent(methodEntry.getKey(), (cn) -> new Metrics(cn)))
				);
			} catch (IOException e) {
				getLog().error(e.getMessage(), e);
			}

		});
		// fields.forEach(f -> System.out.println(f));
//		methodsByClass.keySet().forEach(k->System.out.println(k));
//		Map<String, Pair<Integer, Integer>> nonDataMethods = methodsByClass.entrySet().stream()
//				.collect(Collectors.toMap(e -> e.getKey(), e -> calculateNonDataMethodsCount(fieldsByClass, e, new Metrics(e.getKey()))));
//		nonDataMethods.entrySet().stream().filter(p -> p.getValue().getSecond() == 0)
//				.forEach(e -> System.out.println(e));
		
		
	}

	protected void addTMetrics(String dir, Map<String, Metrics> metricsByProdClass) throws IOException {
		List<String> files = findFiles(dir, (p) -> p.getName().endsWith("-tmetrics.csv"));

		List<Pair<String, String>> pairs = new ArrayList();
		getLog().info("Found " + files.size() + " files in dir=" + dir);
		files.forEach(file -> pairs.addAll(readTMetricPairs(file, "metricType")));
		Set<Path> paths = files.stream().map(file -> new File(file).getAbsoluteFile().getParentFile().toPath()).collect(Collectors.toSet());
		Map<ClassMethodKey, CSVRecord> methodMetrics = new HashMap();
		paths.forEach(path -> methodMetrics.putAll(loadMethodMetrics(path)));
		for (String file : ProgressBar.wrap(files, "coverageMetrics")) {
			addCoverageMetrics(methodMetrics, file, metricsByProdClass);
		}


		getLog().info("Generated " + pairs.size() + " from " + files.size() + " files");
		pairs.forEach(p -> populateTMetrics(p, metricsByProdClass));
	}

	protected List<String> findFiles(String dir, Predicate<File> predicate) throws IOException {
		Path path = new File(dir).toPath();
		List<String> files = java.nio.file.Files.walk(path).filter(p -> predicate.test(p.toFile()))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
		return files;
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
			Set<String> ids = docs.stream().map(doc -> doc.getString("id")).filter(p->p!=null).collect(Collectors.toSet());
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
	
	private final DocumentManager testsLaunchedManager = new DocumentManager(ForkableTestLauncher.TESTS_LAUNCHED_COL_NAME);
	private final DocumentManager testsExecutedManager = new DocumentManager(ForkableTestExecutor.TESTS_EXECUTED_COL_NAME);
	private final DocumentManager classCoverageManager = new DocumentManager(ForkableTestLauncher.CLASS_COVERAGE_COL_NAME);
	private final DocumentManager methodCoverageManager = new DocumentManager(ForkableTestLauncher.METHOD_COVERAGE_COL_NAME);
	private void addCoverageMetrics(Map<ClassMethodKey, CSVRecord> methodMetrics, String file, Map<String, Metrics> metricsByProdClass) {
		File f = new File(file);
		String sessionId = f.getName().replaceAll("-tmetrics.csv", "");
//		getLog().info("Processing sessionId=" + sessionId);
		Collection<Document> testsLaunched = testsLaunchedManager.findDocumentsBySessionId(sessionId);

		Map<String,Document> testClassNames = testsLaunched.stream().collect(Collectors.toMap(x -> x.getString("testClassName"), x->x));
		if (testClassNames.size() != 1) {
			getLog().warn("Found " + testClassNames.keySet() + " testsLaunched for  " + sessionId);
			return;
		}
		Collection<Document> testsExecuted = testsExecutedManager.findDocumentsBySessionId(sessionId);

		Map<String,Document> testsExecutedDocs = testsExecuted.stream().collect(Collectors.toMap(x -> x.getString("testClassName"), x->x));

		Entry<String, Document> testLaunched = testClassNames.entrySet().iterator().next();
		String testClassName = testLaunched.getKey();
		String prodClassName = Helper.getProdClassName(testClassName);
		Collection<Document> classCoverage = classCoverageManager.findDocumentsBySessionId(sessionId);
		Collection<Document> methodCoverage = methodCoverageManager.findDocumentsBySessionId(sessionId);
		Metrics metrics = getMetrics(metricsByProdClass, prodClassName);

		String testCat = Helper.classifyTest(testClassName);
		
		Document doc = testsExecutedDocs.get(testClassName);
		if (doc != null) {
			for (ForkableTestExecutor.TestExecutedMetrics metric : TestExecutedMetrics.values()) {
				Long val = doc.getLong(metric.name());
				if (val != null) {
					metrics.put(metric.name()+"." + testCat, val);
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
		metrics.put(PROD_COVERED_LINES+"." + testCat, covLines);
		try {
			calculateQualityIndex(methodMetrics, testCat, methodCoverage, metrics);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	protected Metrics getMetrics(Map<String, Metrics> metricsByProdClass, String prodClassName) {
		metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
		Metrics metrics = metricsByProdClass.get(prodClassName);
		return metrics;
	}

	/**
	 * based on toure2018predicting
	 * @param tmetricsFile
	 * @param methodCoverage
	 * @param metrics
	 * @return
	 * @throws IOException
	 */
	protected double calculateQualityIndex(Map<ClassMethodKey, CSVRecord> methodMetrics, String testCat, Collection<Document> methodCoverage, Metrics metrics) throws IOException {
//		getLog().info("from absoluteFile=" + absoluteFile + " found " + files);
		BinaryOperator<Document> mergeDocuments = (a,b) -> { a.putAll(b); return a; };
		Map<ClassMethodKey, Document> methodCoverageMap = methodCoverage.stream()
				.collect(Collectors.toMap(k -> ClassMethodKey.fromMethodCoverage(k), v -> v,
						mergeDocuments ));
//		methodCoverageMap.keySet().forEach(x -> getLog().info("methodCoverage: " + x));
//		methodMetrics.keySet().forEach(x -> getLog().info("methodMetrics: " + x));
		Map<String, Long> maxComplexityPerClass = calculateMaxComplexityPerClass(methodMetrics);
		Map<ClassMethodKey, Double> iqai = methodMetrics.keySet().stream().filter(p->methodCoverageMap.containsKey(p)).
				collect(
						Collectors.toMap(k->k, 
						x -> iqai(maxComplexityPerClass, methodCoverageMap.get(x), methodMetrics.get(x))));
		//iqai.entrySet().forEach(x -> getLog().info("iqai: " + x));
		BinaryOperator<Double> mergeFunction = (a,b) -> a*b;
		Map<String, Double> classIqai = iqai.entrySet().stream().collect(Collectors.toMap(k->k.getKey().className, 
				v->v.getValue(), mergeFunction));
		metrics.put("iqai." + testCat, (long) (classIqai.getOrDefault(metrics.prodClassName, (double) -1)*100));
		double qi = 1.0;
		return qi;
	}

	protected Map<ClassMethodKey, CSVRecord> loadMethodMetrics(Path path) {
		List<String> files;
		try {
			files = java.nio.file.Files.walk(path ).filter(p -> p.toFile().getName().endsWith("method.csv"))
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
		} catch (IOException e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	private double iqai(Map<String, Long> maxComplexityPerClass, Document document, CSVRecord csvRecord) {
		double wmc = Double.valueOf(csvRecord.get("wmc"));
		double coverage = Double.valueOf(document.getInteger("coveredLines", 0))
				/ 
				Double.valueOf((document.getInteger("missedLines", 0)+ document.getInteger("coveredLines",0)));
		double maxComplexity = Double.valueOf(maxComplexityPerClass.get(csvRecord.get("class")));
		double qi = (1.0-coverage)*wmc/maxComplexity;
		return 1.0-qi;
	}

	protected Map<String, Long> calculateMaxComplexityPerClass(Map<ClassMethodKey, CSVRecord> methodMetrics) {
		List<Pair<String, Long>> classWithWMC = methodMetrics.values().stream().map(
				v->new Pair<String,Long>(v.get("class"), Long.valueOf(v.get("wmc")))).collect(Collectors.toList());
		Map<String, List<Long>> wmcByClass = classWithWMC.stream().collect(Collectors.groupingBy(k -> k.getKey(), 
				Collectors.mapping(Pair::getValue, Collectors.toList())));
		Map<String, Long> maxWMCByClass = wmcByClass.entrySet().stream().collect(Collectors.toMap(k -> k.getKey(), v->Collections.max(v.getValue())));
		return maxWMCByClass;
	}

	static class ClassMethodKey {

		private String className;
		private String method;
		private int firstLine;

		static ClassMethodKey fromMethodCoverage(Document d) {
			ClassMethodKey classMethod = new ClassMethodKey(d.getString("prodClassName"), d.getString("prodMethodName"), d.getInteger("firstLine", 0));
			return classMethod;
		}
		static ClassMethodKey fromMethodMetric(CSVRecord r) {
			ClassMethodKey classMethod = new ClassMethodKey(r.get("class"), r.get("method").replaceAll("/.*",""), 
					Integer.valueOf(r.get("startLine"))+1);
			return classMethod;
		}
		private ClassMethodKey(String className, String method, int firstLine) {
			this.className=className;
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
	protected void addTNOO(Map<String, Metrics> metricsByProdClass) {
		CKMetricsMojo.extractDirs(project.getTestCompileSourceRoots()).forEach(dirName -> {
			List<Pair<String, String>> testCases = readTMetricPairs(dirName + File.separator + "testcases.csv",
					"testCaseName");
			testCases.stream().filter(p -> !p.getSecond().equals("TOTAL")).forEach(p -> {
				String prodClassName = Helper.getProdClassName(p.getFirst());
				metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
				Metrics m = metricsByProdClass.get(prodClassName);
				String metricName = "TNOO" + getSuffix(p.getFirst());
				m.incrementMetric(metricName);
			});

		});
	}

}
