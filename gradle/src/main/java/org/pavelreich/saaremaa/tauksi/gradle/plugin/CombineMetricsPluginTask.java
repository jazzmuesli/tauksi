package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVParser;
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
//			String projectDir = projectDir2.getAbsolutePath();
			List<String> sourceDirFiles = Helper.findFiles(projectDir.toString(),
					p -> p.getName().equals("sourceDirs.csv"));
			List<String> srcDirs = new ArrayList();
			List<String> testSrcDirs = new ArrayList();
			for (String f : sourceDirFiles) {
				System.out.println("reading " + f);
				CSVParser parser = Helper.getParser(new File(f), "processed");
				parser.getRecords().stream().filter(p -> p.get("processed").equals("true")).forEach(entry -> {
					if (entry.get("sourceSetName").contains("test")) {
						testSrcDirs.add(entry.get("dirName"));
					} else {
						srcDirs.add(entry.get("dirName"));
					}
				});
			}
			srcDirs.forEach(x -> System.out.println("src: " + x));
			testSrcDirs.forEach(x -> System.out.println("testSrc: " + x));
			Logger LOG = LoggerFactory.getLogger(CombineMetricsPluginTask.class);
			String targetDir = projectDir + File.separator + "target";
			new File(targetDir).mkdirs();
			MongoDBClient db = new MongoDBClient(CombineMetricsPluginTask.class.getSimpleName());
			String projectId = project.getGroup() + ":" + project.getName();
			LOG.info("projectId: "+ projectId);
			CombineMetricsTask task = new CombineMetricsTask(db, LOG, projectDir, projectId, targetDir, testSrcDirs,
					"false", testSrcDirs);
			task.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
