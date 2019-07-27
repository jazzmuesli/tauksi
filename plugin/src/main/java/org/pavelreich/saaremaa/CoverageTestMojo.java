package org.pavelreich.saaremaa;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;

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

	public void execute()
        throws MojoExecutionException
    {
    	
    	getLog().info("pluginArtifactMap: "+ pluginArtifactMap.keySet());
    	File jagentPath = resolveJavaAgent("org.pavelreich.saaremaa:jagent");
    	File jacocoPath = resolveJavaAgent("org.jacoco:org.jacoco.agent");
    	String targetClasses = project.getBuild().getOutputDirectory();
		getLog().info("output: " + targetClasses);
		Collection<String> classpath = DependencyHelper.prepareClasspath(project, localRepository, repositorySystem, pluginArtifactMap, getLog());
		getLog().info("classpath: " + classpath);
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
    	List<String> junitClassNames = new ArrayList<String>();
    	MongoDBClient db = new MongoDBClient(getClass().getSimpleName());
    	String id = UUID.randomUUID().toString();
    	for(String dirName: project.getTestCompileSourceRoots()) {
        	try {
        		// process test directory
        		getLog().info("Processing id=" + id + ", dir="  + dirName);
        		ForkableTestLauncher executor = new ForkableTestLauncher(id, db, logger, jagentPath, jacocoPath,new File(targetClasses));
        		executor.setTimeout(Long.valueOf(timeout));
        		if (new File(dirName).exists()) {
					TestFileProcessor processor = TestFileProcessor.run(logger, dirName, null);
    				// extract junit class names
					List<ITestClass> elements = processor.getElements();
					if (testClassName != null && !testClassName.trim().isEmpty()) {
						elements = elements.stream().filter(p->p.getClassName().contains(testClassName)).collect(Collectors.toList());
					}
					Map<String,List<String>> testClassToMethods = new HashMap<String, List<String>>();
    				for (ITestClass element : elements) {
    					junitClassNames.add(element.getClassName());
    					List<String> testMethods = element.getTestMethods().stream().map(x->x.getName()).collect(Collectors.toList());
						testClassToMethods.put(element.getClassName(), testMethods);
    				}
    				getLog().info("junitClassNames: " + junitClassNames.size());
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
                			append("testsCount", junitClassNames.size()).
                			append("dirName", dirName).
                			append("testClasses", junitClassNames);
            		
            		db.insertCollection("projects", Arrays.asList(doc));
            		db.insertCollection("testsDetected", testsDetectedDocs);
            		

    				for (ITestClass testClass : ProgressBar.wrap(elements,"testClasses")) {
    					executor.launch(testClass.getClassName(), classpath);
    				}
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	}
		db.waitForOperationsToFinish();

    }

	private File resolveJavaAgent(String groupWithArtifact) {
		Artifact jagent = pluginArtifactMap.get(groupWithArtifact);
		File file = jagent.getFile();
		getLog().info("agentPath: " + jagent + ", file: " + file);
		return file;
	}


}
