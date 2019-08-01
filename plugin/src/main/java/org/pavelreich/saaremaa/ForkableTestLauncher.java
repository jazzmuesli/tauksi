package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.pavelreich.saaremaa.jagent.MethodInterceptor;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForkableTestLauncher {
	
	private static final Logger CLOG = LoggerFactory.getLogger(ForkableTestLauncher.class);
	private Logger LOG = CLOG;


	private final File jagentPath;
	private final File jacocoPath;
	private final File targetClasses;
	private long timeout = 60;
	private MongoDBClient db;
	private String id;
	private boolean jacocoEnabled;
	private boolean interceptorEnabled;
	
	public ForkableTestLauncher(String id, MongoDBClient db, Logger log, File jagentPath, File jacocoPath, File targetClasses) {
		this.id = id;
		this.db = db;
		this.LOG = log;
		this.jagentPath = jagentPath;
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
		if (!interceptorEnabled) {
			return "";
		}
		return "-javaagent:" + 
			jagentPath + "=" + MethodInterceptor.class.getCanonicalName()+  
			";testClassName=" + testClassName +
			";sessionId=" + sessionId;
	}
	
	private String createJacocoCmd(String fname) {
		if (!jacocoEnabled) {
			return "";
		}
		return "-javaagent:" + jacocoPath+ "=destfile=" + fname;
	}
	
	void launch(TestExecutionCommand testExecutionCommand, Collection<String> classpath)
			throws IOException, InterruptedException {
		String sessionId = UUID.randomUUID().toString();
		long stime = System.currentTimeMillis();
		String fname = stime + ".exec";
		// https://stackoverflow.com/questions/31567532/getting-expecting-a-stackmap-frame-at-branch-target-when-running-maven-integra
		// https://stackoverflow.com/questions/300639/use-of-noverify-when-launching-java-apps
		String cmd = "java -noverify " + 
		createInterceptorJavaAgentCmd(testExecutionCommand.testClassName, sessionId)
		 + " " + 
		createJacocoCmd(fname) 
		 + "  -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator))
				+ " " + ForkableTestExecutor.class.getCanonicalName() + " " + testExecutionCommand.testClassName;
		if (testExecutionCommand.testMethodName !=null) {
			cmd += " " + testExecutionCommand.testMethodName;
		}
		FileWriter fw = new FileWriter("last_command.sh");
		fw.write(cmd);
		fw.close();
		List<String> cmdArgs = Arrays.asList(cmd.split("\\s+"));
		ProcessBuilder pb = new ProcessBuilder(cmdArgs);
		pb.inheritIO();
		Process p;
		p = pb.start();
		boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
		LOG.info("Test  " + testExecutionCommand + " finished:" + finished + ", alive: "+ p.isAlive());
		if (!finished) {
			p.destroy();
			p.destroyForcibly();			
		}
		
		long duration = System.currentTimeMillis() - stime;
		
		int exitValue = p.isAlive() ? -1 : p.exitValue();
		LOG.info("took: " + duration + ", result:" + finished + ", exit: " + exitValue);

		db.insertCollection("testsLaunched", Arrays.asList(
				testExecutionCommand.asDocument().
				append("id", id).
				append("sessionId", sessionId).
				append("jacocoEnabled", jacocoEnabled).
				append("interceptorEnabled", interceptorEnabled).
				append("exitValue", exitValue).
				append("cmd", cmd).
				append("duration", duration).
				append("finished", finished)
				));
		
		if (jacocoEnabled) {
			processCoverageData(testExecutionCommand, fname, sessionId);			
		}
		
	}


	private void processCoverageData(TestExecutionCommand testExecCmd, String fname, String sessionId) throws IOException {
		ExecFileLoader execFileLoader = new ExecFileLoader();
		File file = new File(fname);
		if (file.exists()) {
			execFileLoader.load(file);
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

			analyzer.analyzeAll(targetClasses);

			List<Document> clsCovDocs = new ArrayList<Document>();
			List<Document> metCovDocs = new ArrayList<Document>();
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String prodClassName = cc.getName().replaceAll("/", ".");
				clsCovDocs.add(testExecCmd.asDocument().
						append("prodClassName", prodClassName).
						append("id", id).
						append("sessionId", sessionId).
						append("coveredLines", cc.getLineCounter().getCoveredCount()).
						append("missedLines", cc.getLineCounter().getMissedCount())
						);
				for (IMethodCoverage method : cc.getMethods()) {
					metCovDocs.add(testExecCmd.asDocument().
							append("prodMethodName", method.getName()).
							append("prodClassName", prodClassName).
							append("prodMethodSignature", method.getSignature()).
							append("id", id).
							append("sessionId", sessionId).
							append("coveredLines", method.getLineCounter().getCoveredCount()).
							append("missedLines", method.getLineCounter().getMissedCount())
							);
					
				}
			}
			db.insertCollection("classCoverage", clsCovDocs);
			db.insertCollection("methodCoverage", metCovDocs);
		}
	}


}
