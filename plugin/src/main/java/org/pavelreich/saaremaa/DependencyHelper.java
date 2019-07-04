package org.pavelreich.saaremaa;

import static org.pavelreich.saaremaa.Helper.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

public class DependencyHelper {

	private static Set<Artifact> getDevArtifacts(ArtifactRepository localRepository, RepositorySystem repositorySystem,
			Map<String, Artifact> pluginArtifactMap) {
		ArtifactResolutionRequest request = new ArtifactResolutionRequest()
				.setArtifact(pluginArtifactMap.get("org.pavelreich.saaremaa:plugin")).setResolveTransitively(true)
				.setLocalRepository(localRepository);
		ArtifactResolutionResult result = repositorySystem.resolve(request);
		return result.getArtifacts();
	}

	/**
	 * TODO: make it less hacky.
	 * 
	 * Here we need classpath of the target project + target/test-clases,
	 * target/classes + dependencies of this plugin. Note that this plugin won't be
	 * added to target project's pom.xml, but rather executed mvn
	 * org.pavelreich.saaremaa:plugin:analyse
	 * 
	 * @param log
	 * @param project
	 * @param pluginArtifactMap
	 * @param repositorySystem
	 * @param localRepository
	 * @return
	 * @throws MojoExecutionException
	 */
	public static LinkedHashSet<String> prepareClasspath(MavenProject project, ArtifactRepository localRepository,
			RepositorySystem repositorySystem, Map<String, Artifact> pluginArtifactMap, Log log)
			throws MojoExecutionException {
		LinkedHashSet<String> classpath = new LinkedHashSet<String>();

		try {
			log.info("artifacts: " + convertListToString(project.getArtifacts()
					.stream().map(x->x.toString())
					.collect(Collectors.toList())));
			classpath.addAll(project.getTestClasspathElements());
			classpath.addAll(project.getCompileClasspathElements());
			// copied from
			// https://github.com/tbroyer/gwt-maven-plugin/blob/54fe4621d1ee5127b14030f6e1462de44bace901/src/main/java/net/ltgt/gwt/maven/CompileMojo.java#L295
			ClassWorld world = new ClassWorld();
			ClassRealm realm;
			try {
				realm = world.newRealm("gwt", null);
				for (String elt : project.getCompileSourceRoots()) {
					URL url = new File(elt).toURI().toURL();
					realm.addURL(url);
					if (log.isDebugEnabled()) {
						log.debug("Source root: " + url);
					}
				}
				for (String elt : project.getCompileClasspathElements()) {
					URL url = new File(elt).toURI().toURL();
					realm.addURL(url);
					if (log.isDebugEnabled()) {
						log.debug("Compile classpath: " + url);
					}
				}
				// gwt-dev and its transitive dependencies
				for (Artifact elt : getDevArtifacts(localRepository, repositorySystem, pluginArtifactMap)) {
					URL url = elt.getFile().toURI().toURL();
					realm.addURL(url);
					if (log.isDebugEnabled()) {
						log.debug("Compile classpath: " + url);
					}
				}
				realm.addURL(pluginArtifactMap.get("org.pavelreich.saaremaa:plugin").getFile().toURI().toURL());
				List<String> urls = Arrays.asList(realm.getURLs()).stream().map(x -> {
					try {
						return new File(x.toURI()).getAbsolutePath();
					} catch (Exception e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
				}).collect(Collectors.toList());
//				log.info("realm: " + urls);
				urls.stream().forEach(x -> classpath.add(x));
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}

		} catch (DependencyResolutionRequiredException e1) {
			log.error(e1.getMessage(), e1);
		}
		return classpath;
	}

}
