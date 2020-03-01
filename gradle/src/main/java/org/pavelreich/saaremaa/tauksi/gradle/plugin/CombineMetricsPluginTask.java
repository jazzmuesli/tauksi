package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.pavelreich.saaremaa.CombineMetricsTask;
import org.pavelreich.saaremaa.Helper;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
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
			CombineMetricsTask task = new CombineMetricsTask(db, LOG, projectDir, projectId, targetDir, testSrcDirs,
					"false", srcDirs);
			task.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
