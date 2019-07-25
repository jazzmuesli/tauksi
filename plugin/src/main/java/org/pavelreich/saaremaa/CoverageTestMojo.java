package org.pavelreich.saaremaa;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
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

	public void execute()
        throws MojoExecutionException
    {
    	
    	getLog().info("pluginArtifactMap: "+ pluginArtifactMap.keySet());
    	File jagentPath = resolveJavaAgent("org.pavelreich.saaremaa:jagent");
    	File jacocoPath = resolveJavaAgent("org.jacoco:org.jacoco.agent");
    	String targetClasses = project.getBuild().getOutputDirectory();
		getLog().info("output: " + targetClasses);
//    	unused();
		Collection<String> classpath = DependencyHelper.prepareClasspath(project, localRepository, repositorySystem, pluginArtifactMap, getLog());
		getLog().info("classpath: " + classpath);
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
    	List<String> junitClassNames = new ArrayList();
    	for(String dirName: project.getTestCompileSourceRoots()) {
        	try {
        		// process test directory
        		getLog().info("Processing "  + dirName);
        		ForkableTestExecutor executor = new ForkableTestExecutor(logger, jagentPath, jacocoPath,new File(targetClasses));
        		
        		
        		if (new File(dirName).exists()) {
					TestFileProcessor processor = TestFileProcessor.run(logger, dirName, null);
    				// extract junit class names
					List<ITestClass> elements = processor.getElements();
    				for (ITestClass element : elements) {
    					junitClassNames.add(element.getClassName());
    				}
    				getLog().info("junitClassNames: " + junitClassNames);

    				for (ITestClass testClass : ProgressBar.wrap(elements,"testClasses")) {
    					executor.launch(testClass.getClassName(), classpath);
    				}
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			} finally {
        		if (new File(dirName).exists()) {
        			//createShellScript(dirName, classpath, junitClassNames);
        		}
			}
    	}
    }

	private void unused() {
		ArtifactResolutionRequest request = new ArtifactResolutionRequest();
    	Artifact art = new DefaultArtifact("org.pavelreich.saaremaa", "jagent", "1.0-SNAPSHOT", null, "pom", null, new DefaultArtifactHandler("pom"));
		request.setArtifact(art);
		ArtifactResolutionResult x = repositorySystem.resolve(request);
		getLog().info("resolved req: " + request + " to " + x);
    	getLog().info("pluginArtifactMap: "+ pluginArtifactMap.values());
	}

	private File resolveJavaAgent(String groupWithArtifact) {
		Artifact jagent = pluginArtifactMap.get(groupWithArtifact);
		File file = jagent.getFile();
		getLog().info("agentPath: " + jagent + ", file: " + file);
		return file;
	}


}
