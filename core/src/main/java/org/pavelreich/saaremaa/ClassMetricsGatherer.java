package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKNumber;
import com.github.mauricioaniche.ck.CKReport;

import gr.spinellis.ckjm.CkjmOutputHandler;
import gr.spinellis.ckjm.ClassMetrics;
import gr.spinellis.ckjm.MetricsFilter;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;

public class ClassMetricsGatherer {
	private static final Logger LOG = LoggerFactory.getLogger(ClassMetricsGatherer.class);

	/**
	 * TODO: I had to use this interface because maven-plugin:helpmojo doesn't like an enum with java8 function.
	 * @author preich
	 */
	interface MetricExtractor {
		Number extract(ClassMetrics cm);
	}

	static class WMCExtrator implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getWmc();
		}
	}
	static class LComExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getLcom();
		}
	}
	static class CAExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getCa();
		}
	}
	static class CBOExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getCbo();
		}
	}

	static class DITExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getDit();
		}
	}
	static class NOCExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getNoc();
		}
	}
	static class NPMExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getNpm();
		}
	}
	static class RFCExtractor implements MetricExtractor {
		@Override
		public Number extract(ClassMetrics cm) {
			return cm.getRfc();
		}
	}
	enum MetricKey {
		ATFD(JavaClassMetricKey.ATFD), NCSS(JavaClassMetricKey.NCSS), LOC(JavaClassMetricKey.LOC),
		NOPA(JavaClassMetricKey.NOPA), NOAM(JavaClassMetricKey.NOAM), WOC(JavaClassMetricKey.WOC),
		TCC(JavaClassMetricKey.TCC),
		WMC(JavaClassMetricKey.WMC, new WMCExtrator()),
		LCOM(new LComExtractor()),
		CA(new CAExtractor()), 
		CBO(new CBOExtractor()), 
		DIT(new DITExtractor()), 
		NOC(new NOCExtractor()), 
		NPM(new NPMExtractor()),
		RFC(new RFCExtractor());/** Not present in PMD, but present in CKJM. */
		private JavaClassMetricKey jcmKey;
		private MetricExtractor ckjmExtractor;

		MetricKey(MetricExtractor ckjmExtractor) {
			this(null, ckjmExtractor);
		}

		MetricKey(JavaClassMetricKey jcmKey) {
			this(jcmKey, null);
		}

		MetricKey(JavaClassMetricKey jcmKey, MetricExtractor ckjmExtractor) {
			this.jcmKey = jcmKey;
			this.ckjmExtractor = ckjmExtractor;
		}
	}

	static final List<MetricKey> metrics = Arrays.asList(MetricKey.values());

	static class MetricsCSVReporter extends CSVReporter {
		public MetricsCSVReporter() throws IOException {
			super(new CSVPrinter(Files.newBufferedWriter(Paths.get("class-metrics.csv")),
					CSVFormat.DEFAULT.withHeader(getHeaders()).withDelimiter(';')));
		}
	}

	static class CKMetricsCSVReporter extends CSVReporter {
		public CKMetricsCSVReporter() throws IOException {
			this("class-metrics.csv");
		}
		public CKMetricsCSVReporter(String fname) throws IOException {
			super(new CSVPrinter(Files.newBufferedWriter(Paths.get(fname)),
					CSVFormat.DEFAULT.withHeader("file,class,type,cbo,wmc,dit,noc,rfc,lcom,nom,nopm,nosm,nof,nopf,nosf,nosi,loc".split(",")).withDelimiter(';')));
		}
	}

	static String[] getHeaders() {
		List<String> headers = new ArrayList(Arrays.asList("fileName", "className", "simpleClassName", "gatherer"));
		headers.addAll(metrics.stream().map(Enum::name).collect(Collectors.toList()));
		return headers.toArray(new String[0]);
	}

	public static void main(String[] args) throws IOException {
		List<String> srcDirs = Files.walk(java.nio.file.Paths.get(".")).filter(p->p.toFile().getAbsolutePath().endsWith("src")).map(x->x.getParent().toFile().getAbsolutePath()).collect(Collectors.toList());
		CSVReporter reporter = new CKMetricsCSVReporter();
		srcDirs.parallelStream().forEach(dirName -> processDir(reporter, dirName));
//		runCKJM(reporter);

		reporter.close();
	}

	public static void processDir(CSVReporter reporter, String dirName) {
		try {
			CKReport report = new CK().calculate(dirName);
			report.all()
					.forEach(result -> reporter.write(result.getFile(), result.getClassName(), result.getType(),
							result.getCbo(), result.getWmc(), result.getDit(), result.getNoc(), result.getRfc(),
							result.getLcom(), result.getNom(), result.getNopm(), result.getNosm(), result.getNof(),
							result.getNopf(), result.getNosf(), result.getNosi(), result.getLoc()));
			LOG.info("report: " + report);
			reporter.flush();
		} catch (Exception e) {
			LOG.error("Can't handle " + dirName + " due to " + e.getMessage(), e);
		}
	}

	public static CKMetricsCSVReporter createReporter(Path path) throws IOException {
		return new CKMetricsCSVReporter(path.toString());
	}

	private static void runCKJM(CSVReporter reporter) throws IOException {
		List<Path> files = Files.walk(Paths.get("."))
				.filter(p -> p.toFile().getName().endsWith(".java") || p.toFile().getName().endsWith(".class"))
				.collect(Collectors.toList());
		files.parallelStream().forEach(f -> reportMetrics(f.toFile().getAbsolutePath(), reporter));
	}

	public static void reportMetrics(String fileName, CSVReporter reporter) {
		Boolean runPMD = Boolean.valueOf(System.getProperty("runPMD", "false"));
		if (fileName.endsWith(".java") && runPMD) {
			PMDConfiguration configuration = new PMDConfiguration();
			configuration.setReportFormat("xml");
			configuration.setInputPaths("src/main/java");
			LanguageVersion version = new JavaLanguageModule().getVersion("1.8");
			configuration.setDefaultLanguageVersion(version);
			File sourceCodeFile = new File(fileName);
			String filename = sourceCodeFile.getAbsolutePath();
			try (InputStream sourceCode = new FileInputStream(sourceCodeFile);
					BufferedReader reader = new BufferedReader(new InputStreamReader(sourceCode))) {
				Parser parser = PMD.parserFor(version, configuration);
				ASTCompilationUnit compilationUnit = (ASTCompilationUnit) parser.parse(filename, reader);

				List<ASTAnyTypeDeclaration> astClassOrInterfaceDeclarations = compilationUnit
						.findDescendantsOfType(ASTAnyTypeDeclaration.class);
				astClassOrInterfaceDeclarations
						.forEach(declaration -> reportJavaMetrics(fileName, reporter, declaration));
			} catch (Exception e) {
				LOG.error("Can't report metrics for " + fileName + " due to " + e.getMessage(), e);
			}
			reporter.flush();
		} else if (fileName.endsWith(".class")) {
			reportClassMetrics(fileName, reporter);
		}
	}

	static final String NA_SYMBOL = String.valueOf(Double.NaN);

	private static void reportJavaMetrics(String fileName, CSVReporter reporter, ASTAnyTypeDeclaration declaration) {
		String className = declaration.getImage();
		List<String> values = new ArrayList(Arrays.asList(fileName, className, getSimpleClassName(className), "PMD"));
		List<String> metricValues = metrics.stream().map(m -> {
			try {
				return m.jcmKey != null ? String.valueOf(JavaMetrics.get(m.jcmKey, declaration)) : NA_SYMBOL;
			} catch (Exception e) {
				LOG.error("Can't get metric " + m + " in declaration " + className + " file " + fileName + " due to "
						+ e.getMessage());
				return String.valueOf(Double.NaN);
			}
		}).collect(Collectors.toList());
		values.addAll(metricValues);
		reporter.write(values.toArray(new String[0]));
	}

	private static void reportClassMetrics(String fileName, CSVReporter reporter) {
		File f = new File(fileName);
		CkjmOutputHandler outputHandler = new CkjmOutputHandler() {
			@Override
			public void handleClass(String name, ClassMetrics c) {
				List<String> values = new ArrayList();
				values.add(fileName);
				values.add(name);
				values.add(getSimpleClassName(name));
				values.add("CKJM");
				metrics.stream()
						.map(x -> x.ckjmExtractor != null ? String.valueOf(x.ckjmExtractor.extract(c)) : NA_SYMBOL)
						.forEach(s -> values.add(s));
				reporter.write(values.toArray(new String[0]));
			}
		};
		try {
			MetricsFilter.runMetrics(new String[] { f.getAbsolutePath() }, outputHandler);			
		} catch (Exception e)  {
			LOG.error("Failed to process " + fileName + " due to " + e.getMessage(), e);
		}
		
		reporter.flush();
	}

	protected static String getSimpleClassName(String name) {
		return name.replaceAll("^.*?\\.([^.]+)$", "$1");
	}
}
