package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

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
import org.pavelreich.saaremaa.mongo.MongoDBClient;

@Mojo(name = "combine-metrics", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.NONE)
public class CombineMetricsMojo extends AbstractMojo {
	private static final String LOC_PROD = "loc.prod";
	private static final String PROD_COVERED_LINES = "prod.coveredLines";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;

	public CombineMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
	}

	static class Metrics {
		static Set<String> fields = new LinkedHashSet<>();
		private String prodClassName;

		Map<String, Long> longMetrics = new HashMap<String, Long>();

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
			vals.addAll(fields);
			return vals.toArray(new String[0]);
		}

		public String[] getValues() {
			List<String> vals = new ArrayList();
			vals.add(prodClassName);
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
			parser = getParser(fname, field);
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

	protected CSVParser getParser(String fname, String field) throws IOException {
		CSVParser parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
				CSVFormat.DEFAULT.withFirstRecordAsHeader());
		if (!parser.getHeaderMap().containsKey(field)) {
			parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
					CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader());
		}
		return parser;
	}

	protected Map<String, Map<String, Long>> readCKMetricPairs(String fname, String suffix) {
		if (!new File(fname).exists()) {
			return Collections.emptyMap();
		}
		CSVParser parser;
		try {
			parser = getParser(fname, "class");
			Map<String, Map<String, Long>> ret = parser.getRecords().stream()
					.filter(p-> "class".equals(p.get("type")))
					.collect(Collectors.toMap(x -> x.get("class"), x -> toCKMetrics(suffix, x)));
			return ret;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	private Map<String, Long> toCKMetrics(String suffix, CSVRecord r) {
		return Arrays.asList("cbo", "wmc", "rfc", "loc", "lcom").stream()
				.collect(Collectors.toMap(k -> k + suffix, k -> Long.valueOf(r.get(k))));
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Map<String, Metrics> metricsByProdClass = new HashMap();
			addTMetrics(project.getBasedir().getAbsolutePath(), metricsByProdClass);
			addTNOO(metricsByProdClass);
			addProdCKmetrics(metricsByProdClass);
			addTestCKmetrics(metricsByProdClass);
			String fname = project.getBuild().getDirectory() + File.separator + "metrics.csv";
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
				if (!metrics.longMetrics.containsKey(LOC_PROD)) {
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

	protected void addTestCKmetrics(Map<String, Metrics> metricsByProdClass) throws IOException {
		project.getTestCompileSourceRoots().forEach(dirName -> {
			Map<String, Map<String, Long>> allTestCKMetrics = readCKMetricPairs(dirName + File.separator + "class.csv",
					".test");
			allTestCKMetrics.entrySet().stream().filter(f -> !f.getKey().contains("ESTest_scaffolding")).// ignore
																											// evosuite
			forEach(p -> populateCK(metricsByProdClass, p));
		});
	}

	protected void addProdCKmetrics(Map<String, Metrics> metricsByProdClass) throws IOException {
		project.getCompileSourceRoots().forEach(dirName -> {
			Map<String, Map<String, Long>> prodCKMetrics = readCKMetricPairs(dirName + File.separator + "class.csv",
					".prod");
			prodCKMetrics.entrySet().forEach(p -> populateCK(metricsByProdClass, p));
		});
	}

	protected void addTMetrics(String dir, Map<String, Metrics> metricsByProdClass) throws IOException {
		Path path = new File(dir).toPath();

		List<String> files = java.nio.file.Files.walk(path).filter(p -> p.toFile().getName().endsWith("-tmetrics.csv"))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());

		List<Pair<String, String>> pairs = new ArrayList();
		getLog().info("Found " + files.size() + " files in dir=" + dir);
		files.forEach(file -> pairs.addAll(readTMetricPairs(file, "metricType")));
		files.forEach(file -> addCoverageMetrics(file, metricsByProdClass));
		getLog().info("Generated " + pairs.size() + " from " + files.size() + " files");
		pairs.forEach(p -> populateTMetrics(p, metricsByProdClass));
	}

	protected Map<String, Integer> sumLinesByClass(List<Document> ret, String linesName) {
		Map<String, Integer> map = ret.stream().collect(Collectors.<Document, String, Integer>toMap(
				doc -> doc.getString("prodClassName"), val -> val.getInteger(linesName, 0), (a, b) -> a + b));
		return map;
	}

	//test
	public static void main(String[] args) {
		Map<String, Metrics> metricsByProdClass = new HashMap<>();
		new CombineMetricsMojo().addCoverageMetrics("/Users/preich/Documents/git/tauksi/transformer/b020cd80-e181-4981-a704-b1623eeb352b-tmetrics.csv", metricsByProdClass);
	}
	
	private void addCoverageMetrics(String file, Map<String, Metrics> metricsByProdClass) {
		String sessionId = "730ef9c2-6467-44c3-8b08-2f2f8cdad4b5";
		File f = new File(file);
		sessionId = f.getName().replaceAll("-tmetrics.csv", "");
		getLog().info("Processing sessionId=" + sessionId);
		Bson query = com.mongodb.client.model.Filters.eq("sessionId", sessionId);
		List<Document> testsLaunched = db.find("testsLaunched", query);

		Set<String> testClassNames = testsLaunched.stream().map(x -> x.getString("testClassName"))
				.collect(Collectors.toSet());
		getLog().info("Found " + testClassNames + " testsLaunched for  " + sessionId);
		if (testClassNames.size() != 1) {
			return;
		}

		String testClassName = testClassNames.iterator().next();
		String prodClassName = Helper.getProdClassName(testClassName);
		List<Document> classCoverage = db.find("classCoverage", query);
		List<Document> methodCoverage = db.find("methodCoverage", query);



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
				+ " with " + coverageRatio);

		metricsByProdClass.putIfAbsent(prodClassName, new Metrics(prodClassName));
		Metrics metrics = metricsByProdClass.get(prodClassName);
		metrics.put("prodClassesCovered", prodClassesCovered);
		metrics.put("prod.covratio", coverageRatio);
		Integer coveredLinesByThisTest = coveredLines.getOrDefault(prodClassName, 0);
		long covLines = Math.max(coveredLinesByThisTest, metrics.longMetrics.getOrDefault(PROD_COVERED_LINES, 0L));
		metrics.put(PROD_COVERED_LINES, covLines);
		try {
			calculateQualityIndex(f, methodCoverage, metrics);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	/**
	 * based on toure2018predicting
	 * @param tmetricsFile
	 * @param methodCoverage
	 * @param metrics
	 * @return
	 * @throws IOException
	 */
	protected double calculateQualityIndex(File tmetricsFile, List<Document> methodCoverage, Metrics metrics) throws IOException {
		File absoluteFile = tmetricsFile.getAbsoluteFile();
		Path path = absoluteFile.getParentFile().toPath();
		List<String> files = java.nio.file.Files.walk(path ).filter(p -> p.toFile().getName().endsWith("method.csv"))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
//		getLog().info("from absoluteFile=" + absoluteFile + " found " + files);
		Map<ClassMethodKey, Document> methodCoverageMap = methodCoverage.stream()
				.collect(Collectors.toMap(k -> ClassMethodKey.fromMethodCoverage(k), v -> v));
		Map<ClassMethodKey, CSVRecord> methodMetrics = new HashMap<>();
		for (String f : files) {
			CSVParser parser = getParser(f, "class");
			List<CSVRecord> records = parser.getRecords();
			Map<ClassMethodKey, CSVRecord> map = records.stream()
					.collect(Collectors.toMap(k -> ClassMethodKey.fromMethodMetric(k), v -> v));
//			getLog().info("found "+ records.size() + " for file=" + f);
			methodMetrics.putAll(map);
		}
//		methodCoverageMap.keySet().forEach(x -> getLog().info("methodCoverage: " + x));
//		methodMetrics.keySet().forEach(x -> getLog().info("methodMetrics: " + x));
		Map<String, Long> maxComplexityPerClass = calculateMaxComplexityPerClass(methodMetrics);
		Map<ClassMethodKey, Double> iqai = methodMetrics.keySet().stream().filter(p->methodCoverageMap.containsKey(p)).
				collect(
						Collectors.toMap(k->k, 
						x -> iqai(maxComplexityPerClass, methodCoverageMap.get(x), methodMetrics.get(x))));
		iqai.entrySet().forEach(x -> getLog().info("iqai: " + x));
		BinaryOperator<Double> mergeFunction = (a,b) -> a*b;
		Map<String, Double> classIqai = iqai.entrySet().stream().collect(Collectors.toMap(k->k.getKey().className, 
				v->v.getValue(), mergeFunction));
		metrics.put("iqai", (long) (classIqai.getOrDefault(metrics.prodClassName, (double) -1)*100));
		double qi = 1.0;
		return qi;
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
		project.getTestCompileSourceRoots().forEach(dirName -> {
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
