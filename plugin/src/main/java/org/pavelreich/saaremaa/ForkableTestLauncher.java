package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

import com.google.common.io.Files;

public class ForkableTestLauncher {
	
	public static final String TESTS_LAUNCHED_COL_NAME = "testsLaunched";
	public static final String METHOD_COVERAGE_COL_NAME = "methodCoverage";
	public static final String CLASS_COVERAGE_COL_NAME = "classCoverage";
	private static final Logger CLOG = LoggerFactory.getLogger(ForkableTestLauncher.class);
	private Logger LOG = CLOG;


	private final File jacocoPath;
	private final File targetClasses;
	private long timeout = 60;
	private MongoDBClient db;
	private String id;
	private boolean jacocoEnabled;
	private boolean interceptorEnabled;
	private MavenProject project;
	private boolean alwaysIncludeMethodCoverage = false;
	private boolean alwaysIncludeClassCoverage = false;
	
	public ForkableTestLauncher(String id, MongoDBClient db, Logger log, File jagentPath, File jacocoPath, File targetClasses) {
		this.id = id;
		this.db = db;
		this.LOG = log;
		this.jacocoPath = jacocoPath;
		this.targetClasses = targetClasses;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}


	
	public void enableJacoco(boolean b) {
		this.jacocoEnabled = b;
	}
	
	public void enableInterceptor(boolean b) {
		this.interceptorEnabled = b;
	}
	private String createInterceptorJavaAgentCmd(String testClassName, String sessionId) {
		return "";
//		if (!interceptorEnabled) {
//			return "";
//		}
//		return "-javaagent:" + 
//			jagentPath + "=" + MethodInterceptor.class.getCanonicalName()+  
//			";testClassName=" + testClassName +
//			";sessionId=" + sessionId;
	}
	
	private String createJacocoCmd(String fname) {
		if (!jacocoEnabled) {
			return "";
		}
		return "-javaagent:" + jacocoPath+ "=tmetrics=" + interceptorEnabled + ",destfile=" + fname;
	}
	
	void launch(String sessionId, TestExecutionCommand testExecutionCommand, Collection<String> classpath)
			throws IOException, InterruptedException {
		long stime = System.currentTimeMillis();
		String fname = sessionId + ".exec";
		// https://stackoverflow.com/questions/31567532/getting-expecting-a-stackmap-frame-at-branch-target-when-running-maven-integra
		// https://stackoverflow.com/questions/300639/use-of-noverify-when-launching-java-apps
		String cmd = "java -Did=" + id + " -DsessionId=" + sessionId + " -Dsandbox_mode=OFF -noverify " + 
		createInterceptorJavaAgentCmd(testExecutionCommand.testClassName, sessionId)
		 + " " + 
		createJacocoCmd(fname) 
		 + "  -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator))
				+ " " + ForkableTestExecutor.class.getCanonicalName() + " " + testExecutionCommand.testClassName;
		if (testExecutionCommand.testMethodName !=null) {
			cmd += " " + testExecutionCommand.testMethodName;
		}
		File lastCmdFile = new File("last_command.sh");
		File file = new File(sessionId+".run");
		FileWriter fw = new FileWriter(file);
		fw.write(cmd);
		fw.close();
		Files.copy(file, lastCmdFile);
		List<String> cmdArgs = Arrays.asList(cmd.split("\\s+"));
		ProcessBuilder pb = new ProcessBuilder(cmdArgs);
		pb.inheritIO();
		Process p = pb.start();
		boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
		if (!finished) {
			p.destroy();
			p.destroyForcibly();			
		}
		
		long duration = System.currentTimeMillis() - stime;
		
		int exitValue = p.isAlive() ? -1 : p.exitValue();
		LOG.info("Test  " + testExecutionCommand + " finished:" + finished + " in " + duration + " msec, exitCode: "+ exitValue);

		db.insertCollection(TESTS_LAUNCHED_COL_NAME, Arrays.asList(
				testExecutionCommand.asDocument().
				append("id", id).
				append("sessionId", sessionId).
				append("jacocoEnabled", jacocoEnabled).
				append("interceptorEnabled", interceptorEnabled).
				append("exitValue", exitValue).
				append("startTime", stime).
				append("cmd", cmd).
				append("duration", duration).
				append("finished", finished)
				));
		
		if (jacocoEnabled) {
			try {
				processCoverageData(testExecutionCommand, fname, sessionId, stime);
			} catch (Exception e) {
				LOG.error("Failed to analsye result of " + fname + " from command " + testExecutionCommand + " due to "
						+ e.getMessage(), e);
			}
		}
		
	}


	private void processCoverageData(TestExecutionCommand testExecCmd, String fname, String sessionId, long stime) throws IOException {
		ExecFileLoader execFileLoader = new ExecFileLoader();
		File file = new File(fname);
		if (file.exists()) {
			execFileLoader.load(file);
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

			if (targetClasses.exists()) {
				analyzer.analyzeAll(targetClasses);
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
					}
				}
			}
			
			

			List<Document> clsCovDocs = new ArrayList<Document>();
			List<Document> metCovDocs = new ArrayList<Document>();
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
			db.insertCollection(CLASS_COVERAGE_COL_NAME, clsCovDocs);
			db.insertCollection(METHOD_COVERAGE_COL_NAME, metCovDocs);
		}
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}


}
