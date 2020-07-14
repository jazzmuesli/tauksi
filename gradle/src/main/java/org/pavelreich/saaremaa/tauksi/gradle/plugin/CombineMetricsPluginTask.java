package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
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
			getLogger().info("sourceDirFiles: " + sourceDirFiles);
			Logger LOG = LoggerFactory.getLogger(CombineMetricsPluginTask.class);
			Pair<List<String>, List<String>> tuple2 = SourceDirExtractor.extract(sourceDirFiles);
			List<String> srcDirs = tuple2.getLeft();
			getLogger().info("srcDirs: " + srcDirs);
			List<String> testSrcDirs = tuple2.getRight();
			String targetDir = projectDir + File.separator + "target";
			new File(targetDir).mkdirs();
			MongoDBClient db = new MongoDBClient(CombineMetricsPluginTask.class.getSimpleName());
			String projectId = project.getGroup() + ":" + project.getName();
			LOG.info("projectId: "+ projectId);
			ProjectDirs projDirs = new ProjectDirs(projectDir, targetDir, srcDirs, testSrcDirs,
					getOutput("compileJava"), 
					getOutput("compileTestJava"));
			getLogger().info("projDirs: " + projDirs);
			CombineMetricsTask task = new CombineMetricsTask(db, LOG, projDirs, projectId, "true");
			task.execute();
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}
	}
	
	protected Set<String> getOutput(String task) {
		try {
			TaskContainer tasks = getProject().getTasks();
			Task gtasl = tasks.getByName(task);
			Set<String> outputs = gtasl.getOutputs().getFiles().getFiles()
					.stream().filter(p->p.exists()).map(x->x.getAbsolutePath()).collect(Collectors.toSet());
			return outputs;
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

}
