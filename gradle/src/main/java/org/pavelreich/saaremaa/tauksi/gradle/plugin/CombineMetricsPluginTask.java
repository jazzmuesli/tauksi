package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.pavelreich.saaremaa.combiner.CombineMetricsTask;
import org.pavelreich.saaremaa.Helper;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.pavelreich.saaremaa.combiner.ProjectDirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CombineMetricsPluginTask extends DefaultTask {

	private Project project;

	public void setProject(Project project) {
		this.project = project;
	}

	@TaskAction
	void processDirectories() {
		try {

			File projectDir = project.getProjectDir();
			List<String> sourceDirFiles = Helper.findFiles(projectDir.toString(),
					p -> p.getName().equals("sourceDirs.csv"));
			Logger LOG = LoggerFactory.getLogger(CombineMetricsPluginTask.class);
			Pair<List<String>, List<String>> tuple2 = SourceDirExtractor.extract(sourceDirFiles);
			List<String> srcDirs = tuple2.getLeft();
			List<String> testSrcDirs = tuple2.getRight();
			String targetDir = projectDir + File.separator + "target";
			new File(targetDir).mkdirs();
			MongoDBClient db = new MongoDBClient(CombineMetricsPluginTask.class.getSimpleName());
			String projectId = project.getGroup() + ":" + project.getName();
			LOG.info("projectId: "+ projectId);
			ProjectDirs projDirs = new ProjectDirs(projectDir, targetDir, srcDirs, testSrcDirs,project.getBuildDir().getAbsolutePath(), project.getBuildDir().getAbsolutePath());
			CombineMetricsTask task = new CombineMetricsTask(db, LOG, projDirs, projectId, "false");
			task.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
