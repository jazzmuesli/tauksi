package org.pavelreich.saaremaa;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "testan", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class AnalyseMojo extends AbstractMojo {

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {

		List<String> testCompileSourceRoots = project.getTestCompileSourceRoots();
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
		AnalyseTask task = new AnalyseTask(logger, testCompileSourceRoots);
		task.execute();
	}

}
