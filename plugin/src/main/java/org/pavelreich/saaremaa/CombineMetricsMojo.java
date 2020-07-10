package org.pavelreich.saaremaa;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.pavelreich.saaremaa.combiner.CombineMetricsTask;
import org.pavelreich.saaremaa.combiner.ProjectDirs;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

@Mojo(name = "combine-metrics", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.NONE)
public class CombineMetricsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
	
	@Parameter( property = "ignoreChildProjects", defaultValue = "true")
	private String ignoreChildProjects;
	
	@Parameter( property = "usePomDirectories", defaultValue = "false")
	private String usePomDirectories;

	  /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		boolean offline = session != null && session.isOffline();
		getLog().info("offline: " + offline);
		db = new MongoDBClient(getClass().getSimpleName(), offline);
		if (Boolean.valueOf(ignoreChildProjects) && project.getParent() != null) {
			getLog().info("Ignoring child project " + project);
			return;
		}
		getLog().info("modules: " + project.getModules());
		File basedir = project.getBasedir();
		String projectId = project.getArtifact().getId();
		String targetDirectory = project.getBuild().getDirectory();
		List<String> srcDirs = project.getCompileSourceRoots();
		List<String> testSrcDirs = project.getTestCompileSourceRoots();
		String mainOutputDir = project.getBuild().getOutputDirectory();
		String testOutputDir = project.getBuild().getTestOutputDirectory();

		ProjectDirs projDirs = new ProjectDirs(basedir, 
				targetDirectory, 
				srcDirs, 
				testSrcDirs, 
				Collections.singleton(mainOutputDir), 
				Collections.singleton(testOutputDir));
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
		CombineMetricsTask task = new CombineMetricsTask(db, 
				logger, 
				projDirs,
				projectId, 
				usePomDirectories);

		task.execute();
	}



}
