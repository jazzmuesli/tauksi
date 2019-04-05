package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * scan pom.xml and build maven projects in parallel
 * 
 * @author preich
 *
 */
public class BuildProjects {

	private static final Logger LOG = LoggerFactory.getLogger(BuildProjects.class);
	private File mavenHome;
	private CSVReporter csvReporter;

	public BuildProjects(CSVReporter csvReporter) {
		this.csvReporter = csvReporter;
		this.mavenHome = new File(System.getProperty("maven.home", "/usr/share/maven"));
		if (!mavenHome.exists()) {
			throw new IllegalArgumentException(
					"please provide maven.home, current " + mavenHome.getAbsolutePath() + " is not present");
		}
	}

	public static void main(String[] args) throws IOException {
		List<File> dirs = getDirectories();
		CSVReporter csvReporter = createCSVReporter();
		BuildProjects buildProjects = new BuildProjects(csvReporter);
		dirs.parallelStream().map(d -> buildProjects.buildProject(d)).collect(Collectors.toList());
		csvReporter.close();
	}

	public static CSVReporter createCSVReporter() throws IOException {
		CSVReporter csvReporter = new CSVReporter(new CSVPrinter(
				Files.newBufferedWriter(Paths.get("build-projects.csv")),
				CSVFormat.DEFAULT
						.withHeader(new String[] { "basedir", "startTime", "endTime", "duration", "result", "error" })
						.withDelimiter(';')));
		return csvReporter;
	}

	public static List<File> getDirectories() throws IOException {
		Integer maxdepth = Integer.valueOf(System.getProperty("maxdepth", "2"));
		List<File> dirs = java.nio.file.Files.walk(java.nio.file.Paths.get("."),maxdepth)
				.filter(p -> p.toFile().getName().contains("pom.xml")).map(f -> f.getParent().toFile())
				.collect(Collectors.toList());
		LOG.info("With maxdepth=" + maxdepth +" found " + dirs.size() + " directories");
		return dirs;
	}

	private final class MyLogger implements InvokerLogger {
		private Logger logger;
		private int threshold;

		public MyLogger(Logger logger) {
			this.logger = logger;
			setThreshold(INFO);
		}

		@Override
		public void warn(String message, Throwable throwable) {
			logger.warn(message, throwable);
		}

		@Override
		public void warn(String message) {
			logger.warn(message);
		}

		@Override
		public void setThreshold(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean isWarnEnabled() {
			return logger.isWarnEnabled();
		}

		@Override
		public boolean isInfoEnabled() {
			return logger.isInfoEnabled();
		}

		@Override
		public boolean isFatalErrorEnabled() {
			return logger.isErrorEnabled();
		}

		@Override
		public boolean isErrorEnabled() {
			return logger.isErrorEnabled();
		}

		@Override
		public boolean isDebugEnabled() {
			return logger.isDebugEnabled();
		}

		@Override
		public void info(String message, Throwable throwable) {
			logger.info(message, throwable);
		}

		@Override
		public void info(String message) {
			logger.info(message);
		}

		@Override
		public int getThreshold() {
			return threshold;
		}

		@Override
		public void fatalError(String message, Throwable throwable) {
			logger.error(message, throwable);
		}

		@Override
		public void fatalError(String message) {
			logger.error(message);
		}

		@Override
		public void error(String message, Throwable throwable) {
			logger.error(message, throwable);
		}

		@Override
		public void error(String message) {
			logger.error(message);
		}

		@Override
		public void debug(String message, Throwable throwable) {
			logger.debug(message, throwable);
		}

		@Override
		public void debug(String message) {
			logger.debug(message);
		}
	}

	static class BuildOutcome {
		private InvocationResult result;
		private File basedir;
		private long startTime;
		private long endTime;
		private MavenInvocationException mavenInvocationException;
		private long duration;

		BuildOutcome(File basedir, long startTime, long endTime, InvocationResult result,
				MavenInvocationException mavenInvocationException) {
			this.basedir = basedir;
			this.startTime = startTime;
			this.endTime = endTime;
			this.duration = endTime - startTime;
			this.result = result;
			this.mavenInvocationException = mavenInvocationException;
		}
	}

	private BuildOutcome buildProject(File basedir) {
		InvocationRequest request = new DefaultInvocationRequest();

		request.setBaseDirectory(basedir);
		request.setPomFile(new File("pom.xml"));
		request.setGoals(Collections.singletonList("test-compile"));

		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(mavenHome);
		InvokerLogger logger = new MyLogger(LoggerFactory.getLogger(basedir.getAbsolutePath()));
		invoker.setLogger(logger);
		long startTime = System.currentTimeMillis();
		BuildOutcome outcome;
		try {
			InvocationResult result = invoker.execute(request);
			outcome = new BuildOutcome(basedir, startTime, System.currentTimeMillis(), result, null);
		} catch (MavenInvocationException e) {
			LOG.error("Can't build " + basedir + " due to " + e.getMessage(), e);
			outcome = new BuildOutcome(basedir, startTime, System.currentTimeMillis(), null, e);
		}
		Exception exception = outcome.mavenInvocationException == null ? outcome.result.getExecutionException() : outcome.mavenInvocationException;
		csvReporter.write(new String[] { outcome.basedir.getAbsolutePath(), String.valueOf(outcome.startTime),
				String.valueOf(outcome.endTime), String.valueOf(outcome.duration),
				outcome.result == null ? null : String.valueOf(outcome.result.getExitCode()),
				exception != null ? exception.getClass().getCanonicalName()+":" +exception.getMessage() : null });
		csvReporter.flush();
		return outcome;
	}
}
