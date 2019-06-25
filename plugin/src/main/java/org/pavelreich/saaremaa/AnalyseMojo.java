package org.pavelreich.saaremaa;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;

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

		for (String dirName : project.getTestCompileSourceRoots()) {
			try {
				// process test directory
				getLog().info("Processing " + dirName);
				if (new File(dirName).exists()) {
					String resultFileName = dirName + File.separator + "result.json";
					String assertsFileName = dirName + File.separator + "asserts.csv";
					String mockitoFileName = dirName + File.separator + "mockito.csv";
					TestFileProcessor processor = TestFileProcessor.run(dirName, resultFileName);
					processor.writeCSVResults(assertsFileName);
					processor.writeMockito(mockitoFileName);
				}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
		}
	}

}
