package org.pavelreich.saaremaa;

import static org.pavelreich.saaremaa.Helper.convertListToString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.github.mauricioaniche.ck.plugin.CKMetricsMojo;

import core.Architecture;
import dependencies.Dependency;

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
			getLog().info("classpath: " + convertListToString(new ArrayList(classpath)));
			List<String> sourcepath = new ArrayList(project.getCompileSourceRoots());
			getLog().info("compileSourceRoots: " + convertListToString(project.getCompileSourceRoots()));
			getLog().info("testCompileSourceRoots: " + convertListToString(project.getTestCompileSourceRoots()));
			sourcepath.addAll(CKMetricsMojo.extractDirs(project.getTestCompileSourceRoots()));
			getLog().info("sourcepath: " + convertListToString(sourcepath));
			List<String> sourceFiles = sourcepath.stream().map(srcDir -> getSourceFiles(srcDir)).flatMap(List::stream)
					.collect(Collectors.toList());

			getLog().info("sourceFiles: " +  convertListToString(sourceFiles));
			architecture = new Architecture(new MavenLoggerAsSLF4jLoggerAdaptor(getLog()));
			architecture.extractDependencies(classpath.toArray(new String[0]), sourcepath.toArray(new String[0]),
					sourceFiles);
			Collection<Dependency> deps = architecture.getClassDependencies();
			Comparator<Dependency> comp = Comparator.comparing(p -> p.getClassNameA());
			comp = comp.thenComparing(Comparator.comparing(p -> p.getLineNumber()));
			deps = deps.stream().sorted(comp).collect(Collectors.toList());
			reportClasses(projectPath, architecture.getITypeBindings());
			reportDependencies(projectPath, deps);

//			TxtFileWriter.writeTxtFile(deps.stream().map(Architecture::toCSV).collect(Collectors.toList()), projectPath);
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	private void reportClasses(String projectPath, List<ITypeBinding> bindings) {
		String fname = projectPath+File.separator+"classes.csv";
		if (!bindings.isEmpty()) {
			try (CSVReporter reporter = new CSVReporter(fname, "classA","modifiers", "superclasses","interfaces")) {
				for (ITypeBinding binding : bindings) {
					if (binding == null) {
						continue;
					}
					String qualifiedName = binding.getQualifiedName();
					String parentClassName = getParents(binding);
					String interfaceNames = binding.getInterfaces() != null ? Arrays.stream(binding.getInterfaces()).map(x->x.getQualifiedName()).collect(Collectors.joining(",")) : "";
					reporter.write(qualifiedName, binding.getModifiers(), parentClassName, interfaceNames);
				}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}

		}
	}

	private String getParents(ITypeBinding binding) {
		if (binding == null || binding.getSuperclass() == null) {
			return "";
		} else {
			String parentName = binding.getSuperclass().getQualifiedName();
			String grandParentName = getParents(binding.getSuperclass());
			if (!grandParentName.equals("")) {
				return parentName + "," + grandParentName;
			}
			return parentName;
		}
	}


	private void reportDependencies(String projectPath, Collection<Dependency> deps) {
		String fname = projectPath+File.separator+"dependencies.csv";
		if (!deps.isEmpty()) {
			try (CSVReporter reporter = new CSVReporter(fname, "classA","dependencyType","classB","classAline")) {
				for (Dependency dep : deps) {
					reporter.write(Architecture.toCSV(dep).split(","));
				}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}

		}
	}


	private List<String> getSourceFiles(String srcDir) {
		try {
			Path path = java.nio.file.Paths.get(srcDir);
			if (!path.toFile().exists()) {
				getLog().info("Path " + srcDir + " doesn't exist");
				return Collections.emptyList();
			}
			List<String> files = java.nio.file.Files.walk(path)
					.filter(p -> p.toFile().getName().endsWith(".java"))
					.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
			return files;
		} catch (IOException e) {
			getLog().error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}
}
