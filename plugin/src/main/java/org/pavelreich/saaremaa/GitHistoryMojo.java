package org.pavelreich.saaremaa;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "githistory", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class GitHistoryMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {
		File dir = project.getBasedir();
		if (!Helper.isRootProject(project)) {
			getLog().info("Ignoring " + dir + " because it has parent  " + project.getParent());
			return;
		}
		getLog().info("Processing " + dir);
		try {
			new GitHistory(new MavenLoggerAsSLF4jLoggerAdaptor(getLog())).run(dir);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

}
