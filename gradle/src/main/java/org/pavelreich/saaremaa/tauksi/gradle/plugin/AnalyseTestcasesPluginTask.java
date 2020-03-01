package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.tasks.TaskAction;
import org.pavelreich.saaremaa.AnalyseTask;
import org.pavelreich.saaremaa.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyseTestcasesPluginTask extends DefaultTask {

	private Project project;

	public void setProject(Project project) {
		this.project = project;
	}

	@TaskAction
	void processDirectories() {
		try {
			Logger LOG = LoggerFactory.getLogger(AnalyseTestcasesPluginTask.class);

			File projectDir = project.getProjectDir();
			List<String> sourceDirFiles = Helper.findFiles(projectDir.toString(),
					p -> p.getName().equals("sourceDirs.csv"));
			Pair<List<String>, List<String>> tuple2 = SourceDirExtractor.extract(sourceDirFiles);
			List<String> srcDirs = tuple2.getLeft();
			List<String> testSrcDirs = tuple2.getRight();
			AnalyseTask task = new  AnalyseTask(LOG, testSrcDirs);
			task.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
