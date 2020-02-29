package org.pavelreich.saaremaa;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
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

	public CombineMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
	}




	public static void main(String[] args) {
		CombineMetricsMojo mojo = new CombineMetricsMojo();
		try {
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (Boolean.valueOf(ignoreChildProjects) && project.getParent() != null) {
			getLog().info("Ignoring child project " + project);
			return;
		}
		getLog().info("modules: " + project.getModules());
		File basedir = project.getBasedir();
		String projectId = project.getArtifact().getId();
		String targetDirectory = project.getBuild().getDirectory();
		List<String> testSrcDirs = project.getTestCompileSourceRoots();

		CombineMetricsTask task = new CombineMetricsTask(db, getLog(), basedir, projectId, targetDirectory, testSrcDirs, usePomDirectories, project.getCompileSourceRoots());

		task.execute();
	}



}
