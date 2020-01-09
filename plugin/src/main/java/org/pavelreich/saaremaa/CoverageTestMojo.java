package org.pavelreich.saaremaa;

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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.bson.Document;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

import me.tongfei.progressbar.ProgressBar;

/**
 * Fork a process for every test using ForkableTestExecutor.
 * 
 */
@Mojo(name = "ctest", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class CoverageTestMojo extends AbstractMojo {


	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	@Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
	private Map<String, Artifact> pluginArtifactMap;

	@Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
	private ArtifactRepository localRepository;

	@Component
	private RepositorySystem repositorySystem;
	
	@Parameter( property = "testClassName", defaultValue = "" )
	private String testClassName;

	@Parameter( property = "timeout", defaultValue = "60")
	private String timeout;

	@Parameter( property = "jacocoEnabled", defaultValue = "true")
	private String jacocoEnabled;

	@Parameter( property = "interceptorEnabled", defaultValue = "true")
	private String interceptorEnabled;

	@Parameter( property = "extraClassPath", defaultValue = "false")
	private String extraCp;

	/**
	 * Run jacoco agent and then interceptor
	 */
	@Parameter( property = "seqAgents", defaultValue = "false")
	private String seqAgents;

	/**
	 * run test methods in separate sessions
	 */
	@Parameter( property = "seqTestMethods", defaultValue = "false")
	private String seqTestMethods;
	
	@Parameter( property = "shuffleTests", defaultValue = "true")
	private String shuffleTests;
	
	@Parameter( property = "poolSize", defaultValue = "1")
	private String poolSize;

	@Parameter( property = "testExtractor", defaultValue = "FirstAvailableTestExtractor")
	private String testExtractor;

	public void execute()
        throws MojoExecutionException
    {
		getLog().info("nThreads: " + poolSize + ", seqAgents: " + seqAgents + ", jacocoEnabled: " + jacocoEnabled + ", interceptorEnabled: " + interceptorEnabled +", shuffleTests: " + shuffleTests);
    	File jagentPath = resolveJavaAgent("org.pavelreich.saaremaa:jagent");
    	File jacocoPath = resolveJavaAgent("org.jacoco:org.jacoco.agent");
    	String targetClasses = project.getBuild().getOutputDirectory();
		getLog().info("output: " + targetClasses);
		// ignore log4j
		final Collection<String> classpath = DependencyHelper.prepareClasspath(project, localRepository, repositorySystem, pluginArtifactMap, getLog()).stream().filter(this::filterDependency).collect(Collectors.toList());
		if (this.extraCp != null && !extraCp.trim().isEmpty()) {
			classpath.addAll(Arrays.asList(extraCp.split(":")));
		}
		getLog().info("classpath: " + classpath);
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
    	MongoDBClient db = new MongoDBClient(getClass().getSimpleName());
    	String id = UUID.randomUUID().toString();
    	TestExtractor extractor = createTestExtractor(logger, testClassName);
    	Integer nThreads = Integer.valueOf(poolSize);
		ExecutorService pool = new ThreadPoolExecutor(nThreads, nThreads,
                 5000L, TimeUnit.MILLISECONDS,
                 new ArrayBlockingQueue<Runnable>(nThreads, true), new ThreadPoolExecutor.CallerRunsPolicy());

		for (String dirName : project.getTestCompileSourceRoots()) {
        	try {
        		// process test directory
        		getLog().info("Processing id=" + id + ", dir="  + dirName);
				ForkableTestLauncher launcher = new ForkableTestLauncher(id, db, logger, jagentPath, jacocoPath,
						new File(targetClasses));
				launcher.setProject(project);
				launcher.setTimeout(Long.valueOf(timeout));
        		if (new File(dirName).exists()) {
					Map<String, Set<String>> testClassToMethods = extractor.extractTestCasesByClass(dirName);
					getLog().info("Found " + testClassToMethods.size() + " test classes");
                	List<Document> testsDetectedDocs = testClassToMethods.entrySet().stream().map(x -> 
                	new Document().
                	append("id", id).
                	append("testClassName", x.getKey()).
                	append("testMethods",  x.getValue())).
                			collect(Collectors.toList());
                	Document doc = new Document().
                			append("startTime", System.currentTimeMillis()).
                			append("id", id).
                			append("project", project.getArtifact().getId()).
                			append("basedir", project.getBasedir().toString()).
                			append("testsCount", testClassToMethods.size()).
                			append("dirName", dirName).
                			append("testClasses", testClassToMethods.keySet());
            		
            		db.insertCollection("projects", Arrays.asList(doc));
            		db.insertCollection("testsDetected", testsDetectedDocs);
            		

            		List<TestExecutionCommand> commands = new ArrayList<>(); 
    				for (Entry<String,Set<String>> testClass : testClassToMethods.entrySet()) {
    					if (Boolean.valueOf(seqTestMethods)) {
    						Set<String> testMethods = testClass.getValue();
    						for (String testMethod : testMethods) {
        						TestExecutionCommand cmd = TestExecutionCommand.forTestClassMethod(testClass.getKey(), testMethod);
        						commands.add(cmd);
    						}
    					} else {
    						TestExecutionCommand cmd = TestExecutionCommand.forTestClass(testClass.getKey());
    						commands.add(cmd);
    					}
    				}
    				if (Boolean.valueOf(shuffleTests)) {
    					Collections.shuffle(commands);
    				}
    				if (testClassName != null && !testClassName.isEmpty()) {
    					getLog().info("Filtering by testClassName=" + testClassName);
    					commands = commands.stream().filter(p->p.testClassName.equals(testClassName)).collect(Collectors.toList());
    				}
    				for (TestExecutionCommand cmd : ProgressBar.wrap(commands, "testCases")) {
						if (nThreads == 1) {
							runTest(classpath, launcher, cmd);
						} else {
							pool.submit(() -> runTest(classpath, launcher, cmd));							
						}
    					 
    				}
        		}
    			pool.awaitTermination(1, TimeUnit.HOURS);
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	}
		db.waitForOperationsToFinish();

    }


	private TestExtractor createTestExtractor(MavenLoggerAsSLF4jLoggerAdaptor logger, String testClassName) {
		TestExtractor extractor;
		if (testClassName != null && !testClassName.trim().isEmpty()) {
			return new SingleClassExtractor(testClassName);
		}
    	if (testExtractor.equals(SurefireTestExtractor.class.getSimpleName())) {
    		extractor = new SurefireTestExtractor(logger);
    	} else if (testExtractor.equals(SpoonTestExtractor.class.getSimpleName())) {
    		extractor = new SpoonTestExtractor(logger);
    	} else if (testExtractor.equals(MetricsTestExtractor.class.getSimpleName())) {
    		extractor = new MetricsTestExtractor(logger, project.getTestCompileSourceRoots());
    	} else if (testExtractor.equals(FirstAvailableTestExtractor.class.getSimpleName())) {
    		extractor = new FirstAvailableTestExtractor(new SurefireTestExtractor(logger),
    				new SpoonTestExtractor(logger));
    	} else {
    		extractor = new CombinedTestExtractor(new SurefireTestExtractor(logger),
    				new SpoonTestExtractor(logger));
    	}
		return extractor;
	}


	private boolean filterDependency(String p) {
		if (p.contains("junit-3.")) {
			return false;
		}
		if (p.contains("unit-4") && !p.contains("unit-4.12")) {
			getLog().info("bad junit: " + p);
			return false;
		}
		return !p.contains("slf4j-log4j12");
	}

	private void runTest(Collection<String> classpath, ForkableTestLauncher launcher, TestExecutionCommand cmd) {
		String sessionId = UUID.randomUUID().toString();
		try {

			launcher.setSessionId(sessionId);
			Boolean jacEnabled = Boolean.valueOf(jacocoEnabled);
			Boolean intercepEnabled = Boolean.valueOf(interceptorEnabled);
			launcher.enableJacoco(false);
			launcher.enableInterceptor(false);
			if (Boolean.valueOf(seqAgents)) {
				if (jacEnabled) {
					launcher.enableJacoco(jacEnabled);
					launcher.enableInterceptor(false);
					launcher.launch(cmd, classpath);
				}
				if (intercepEnabled) {
					launcher.enableJacoco(false);
					launcher.enableInterceptor(intercepEnabled);
					launcher.launch(cmd, classpath);
				}
			} else {
				launcher.enableJacoco(jacEnabled);
				launcher.enableInterceptor(intercepEnabled);
				launcher.launch(cmd, classpath);
			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	private File resolveJavaAgent(String groupWithArtifact) {
		Artifact jagent = pluginArtifactMap.get(groupWithArtifact);
		File file = jagent.getFile();
//		getLog().info("agentPath: " + jagent + ", file: " + file);
		return file;
	}


}
