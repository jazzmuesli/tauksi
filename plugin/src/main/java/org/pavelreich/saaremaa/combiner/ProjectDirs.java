package org.pavelreich.saaremaa.combiner;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class ProjectDirs {
	File basedir;
	String targetDirectory;
	List<String> srcDirs;
	List<String> testSrcDirs;
	Collection<String> mainOutputDirs;
	Collection<String> testOutputDirs;
	public ProjectDirs(File basedir, String targetDirectory, List<String> srcDirs, List<String> testSrcDirs,
			Collection<String> mainOutputDirs, Collection<String> testOutputDirs) {
		this.basedir = basedir;
		this.targetDirectory = targetDirectory;
		this.srcDirs = srcDirs;
		this.testSrcDirs = testSrcDirs;
		this.mainOutputDirs = mainOutputDirs;
		this.testOutputDirs = testOutputDirs;
	}

}
