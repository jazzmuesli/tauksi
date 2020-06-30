package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

import gr.spinellis.ckjm.MetricsFilter;
import gr.spinellis.ckjm.PrintPlainResults;

@Mojo(name = "ckjm", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.NONE)
public class CkJmMojo extends AbstractMojo {

	// private MongoDBClient db;
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

	public CkJmMojo() {
		super();
		// db = new MongoDBClient(getClass().getSimpleName());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String targetDir = project.getBuild().getDirectory();
		String fname = targetDir + File.separator + "ckjm.csv";

		try {
			List<String> files;
			files = Helper.findFiles(targetDir, (s) -> s.getName().endsWith(".class"));
			LinkedHashSet<String> classpath = DependencyHelper.prepareClasspath(project, localRepository,
					repositorySystem, pluginArtifactMap, getLog());
			getLog().info("Found " + files.size() + " files in " + targetDir);
			PrintPlainResults outputPlain = new PrintPlainResults(new PrintStream(fname));

			Collection<String> allFiles = new LinkedHashSet<String>(files);
//			allFiles.addAll(classpath);
			String sClasspath = classpath.stream().collect(Collectors.joining(":"));
			getLog().info("Found classpath: " + sClasspath);
			System.setProperty("java.class.path", sClasspath);
			MetricsFilter.runMetrics(allFiles.toArray(new String[0]), outputPlain);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

}
