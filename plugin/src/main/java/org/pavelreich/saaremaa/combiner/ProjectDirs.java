package org.pavelreich.saaremaa.combiner;

import java.io.File;
import java.util.List;

public class ProjectDirs {
	File basedir;
	String targetDirectory;
	List<String> srcDirs;
	List<String> testSrcDirs;
	String mainOutputDir;
	String testOutputDir;
	public ProjectDirs(File basedir, String targetDirectory, List<String> srcDirs, List<String> testSrcDirs,
			String mainOutputDir, String testOutputDir) {
		this.basedir = basedir;
		this.targetDirectory = targetDirectory;
		this.srcDirs = srcDirs;
		this.testSrcDirs = testSrcDirs;
		this.mainOutputDir = mainOutputDir;
		this.testOutputDir = testOutputDir;
	}

}
