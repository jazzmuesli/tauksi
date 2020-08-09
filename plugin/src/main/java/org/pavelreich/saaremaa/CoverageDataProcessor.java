package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.bson.Document;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverageDataProcessor {
	private static final Logger CLOG = LoggerFactory.getLogger(ForkableTestLauncher.class);
	private Logger LOG = CLOG;
	private MongoDBClient db;
	private File targetClasses;
	MavenProject project;
	private boolean alwaysIncludeMethodCoverage = false;
	private boolean alwaysIncludeClassCoverage = false;
	private String id;

	public CoverageDataProcessor(String id, MongoDBClient db, Logger log, File targetClasses) {
		this.id = id;
		this.db = db;
		this.LOG = log;
		this.targetClasses = targetClasses;
	}
	
	public static TestExecutionCommand extractTestExecutionCommand(File execFileName) throws IOException {
		File runFile = new File(execFileName.getAbsolutePath().replaceAll(".exec", ".run"));
		TestExecutionCommand cmd = new TestExecutionCommand();
		if (runFile.exists()) {
			List<String> runLines = Files.readAllLines(runFile.toPath());
			for (String line : runLines) {
				int idx = line.indexOf(ForkableTestExecutor.class.getCanonicalName());
				if (idx > 0) {
					String[] parts = line.substring(idx+ForkableTestExecutor.class.getCanonicalName().length()+1).split(" ");
					if (parts.length >= 0) {
						cmd = TestExecutionCommand.forTestClass(parts[0]);
					}
				}
			}
		}
		return cmd;
	}


	public CoverageBuilder processCoverageData(TestExecutionCommand testExecCmd, String fname, String sessionId, long stime) throws IOException {
		ExecFileLoader execFileLoader = new ExecFileLoader();
		File file = new File(fname);
		if (file.exists()) {
			execFileLoader.load(file);
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

			boolean analysed = false;
			if (targetClasses.exists()) {
				analyzer.analyzeAll(targetClasses);
				analysed = true;
			} else {
				LOG.info("targetClasses=" + targetClasses + " doesn't exist");
			}
			if (project != null) {
				List<String> classpathEntries = DependencyHelper.getCoverageClasspath(project);
				for (String s : classpathEntries) {
					LOG.info("Calculating coverage for " + s);
					File f = new File(s);
					if (f.exists()) {
						analyzer.analyzeAll(f);
						analysed = true;
					}
				}
			}
			
			List<Document> clsCovDocs = new ArrayList<>();
			List<Document> metCovDocs = new ArrayList<>();
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String prodClassName = cc.getName().replaceAll("/", ".");
				if (alwaysIncludeClassCoverage || cc.getLineCounter().getCoveredCount() > 0) {
					clsCovDocs.add(testExecCmd.asDocument().
							append("prodClassName", prodClassName).
							append("id", id).
							append("sessionId", sessionId).
							append("startTime", stime).
							append("coveredLines", cc.getLineCounter().getCoveredCount()).
							append("missedLines", cc.getLineCounter().getMissedCount())
							);
				}
				for (IMethodCoverage method : cc.getMethods()) {
					if (alwaysIncludeMethodCoverage || method.getLineCounter().getCoveredCount() > 0) {
						metCovDocs.add(testExecCmd.asDocument().
								append("prodMethodName", method.getName()).
								append("prodClassName", prodClassName).
								append("startTime", stime).
								append("prodMethodSignature", method.getSignature()).
								append("prodMethodDescription", method.getDesc()).
								append("firstLine", method.getFirstLine()).
								append("lastLine", method.getLastLine()).
								append("id", id).
								append("sessionId", sessionId).
								append("coveredLines", method.getLineCounter().getCoveredCount()).
								append("missedLines", method.getLineCounter().getMissedCount())
								);
					}
				}
			}
			LOG.info("Found coverage for " + coverageBuilder.getClasses().size() + " classes, inserting " + clsCovDocs.size() + " class and " + metCovDocs.size() + " method documents");
			db.insertCollection(ForkableTestLauncher.CLASS_COVERAGE_COL_NAME, clsCovDocs);
			db.insertCollection(ForkableTestLauncher.METHOD_COVERAGE_COL_NAME, metCovDocs);
			return coverageBuilder;
		}
		return null;
	}
}
