package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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
import org.pavelreich.saaremaa.ClassMetricsGatherer.CKMetricsCSVReporter;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class CodeMetricsMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;
    @Parameter( defaultValue="${project}", readonly=true, required=true )
    MavenProject project;
    @Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
    private Map<String, Artifact> pluginArtifactMap;
    
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;
    
    @Component
    private RepositorySystem repositorySystem;
    
    public void execute()
        throws MojoExecutionException
    {
    	
    	List<String> dirs = new ArrayList<String>();
    	dirs.addAll(project.getTestCompileSourceRoots());
    	dirs.addAll(project.getCompileSourceRoots());
    	getLog().info("dirs: " + dirs);
    	CKMetricsCSVReporter reporter;
    	try {
			Path path = Paths.get(project.getBasedir().getAbsolutePath(), "class-metrics.csv");
			getLog().info("path: " + path);
			reporter = ClassMetricsGatherer.createReporter(path);
		} catch (IOException e1) {
			throw new IllegalArgumentException(e1);
		}
    	dirs.parallelStream().forEach(dirName -> {
        	try {
        		// process test directory
        		getLog().info("Processing "  + dirName);
        		if (new File(dirName).exists()) {
					ClassMetricsGatherer.processDir(reporter, dirName);        			
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	});

    	reporter.close();
    }
}
