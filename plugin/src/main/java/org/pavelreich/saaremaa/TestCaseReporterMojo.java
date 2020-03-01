package org.pavelreich.saaremaa;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;

@Mojo(name = "analyse-testcases", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class TestCaseReporterMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public TestCaseReporterMojo() {
		super();
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Logger logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
			TestCaseReporterTask task = new TestCaseReporterTask(logger, project.getTestCompileSourceRoots());
			task.execute();
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

}
