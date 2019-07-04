package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

import core.Architecture;
import dependencies.Dependency;
import util.TxtFileWriter;

/**
 * taken from https://github.com/tsantalis/RefactoringMiner
 * 
 * @author preich
 *
 */
@Mojo(name = "javadepextractor", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class JavaDepExtractorMojo extends AbstractMojo {

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

	public void execute() throws MojoExecutionException {
		File dir = project.getBasedir();
		getLog().info("parent: " + project.getParentArtifact());

		getLog().info("Processing " + dir);

		String projectPath = dir.getAbsolutePath();
		Architecture architecture;
		try {
			LinkedHashSet<String> classpath = DependencyHelper.prepareClasspath(project, localRepository,
					repositorySystem, pluginArtifactMap, getLog());
			getLog().info("classpath: " + classpath);
			List<String> sourcepath = project.getCompileSourceRoots();
			getLog().info("sourcepath: " + sourcepath);
			List<String> sourceFiles = sourcepath.stream().map(srcDir -> getFiles(srcDir)).flatMap(List::stream)
					.collect(Collectors.toList());

			getLog().info("sourceFiles: " +  sourceFiles);
			architecture = new Architecture(new MavenLoggerAsSLF4jLoggerAdaptor(getLog()));
			architecture.extractDependencies(classpath.toArray(new String[0]), sourcepath.toArray(new String[0]),
					sourceFiles);
			Collection<Dependency> deps = architecture.getClassDependencies();
			Comparator<Dependency> comp = Comparator.comparing(p -> p.getClassNameA());
			comp = comp.thenComparing(Comparator.comparing(p -> p.getLineNumber()));
			deps = deps.stream().sorted(comp).collect(Collectors.toList());
			TxtFileWriter.writeTxtFile(deps.stream().map(Architecture::toCSV).collect(Collectors.toList()), projectPath);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	private List<String> getFiles(String srcDir) {
		try {
			Path path = java.nio.file.Paths.get(srcDir);
			if (!path.toFile().exists()) {
				getLog().info("Path " + srcDir + " doesn't exist");
				return Collections.emptyList();
			}
			List<String> dirs = java.nio.file.Files.walk(path)
					.filter(p -> p.toFile().getName().endsWith(".java"))
					.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
			return dirs;
		} catch (IOException e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}
}
