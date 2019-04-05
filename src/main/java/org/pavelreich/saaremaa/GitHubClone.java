package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clone projects
 * 
 * 
 * @author preich
 *
 */
public class GitHubClone {
	static final Logger LOG = LoggerFactory.getLogger(GitHubClone.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			LOG.error("Usage: " + GitHubClone.class.getCanonicalName() + " gh_projects.url");
			System.exit(1);
		}
		String filename = args[0];
		List<String> urls = Files.lines(Paths.get(filename)).filter(p -> p.startsWith("https://github.com/"))
				.collect(Collectors.toList());
		urls.forEach(url -> cloneProject(url));

	}

	private static void cloneProject(String repoUrl) {

		String cloneDirectoryPath = repoUrl.replaceAll("^.*?github.com/", "");
		LOG.info("Cloning " + repoUrl + " to " + cloneDirectoryPath);
		try {
			if (!new File(cloneDirectoryPath).exists()) {
				Git.cloneRepository().setURI(repoUrl).setDirectory(Paths.get(cloneDirectoryPath).toFile()).call();
			} else {
				LOG.info("Already exists: " + cloneDirectoryPath + " for  " + repoUrl);
			}
		} catch (Exception e) {
			LOG.error("Can't clone " + repoUrl + " into " + cloneDirectoryPath);
		}
	}
}
