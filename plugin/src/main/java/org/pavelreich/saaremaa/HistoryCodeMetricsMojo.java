package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.pavelreich.saaremaa.CodeMetricsMojo.InternalMojo;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

public class HistoryCodeMetricsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
	private InternalMojo mojo;

	public HistoryCodeMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
		mojo = new InternalMojo(db, project);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub
		try {
			Git git = Git.open(new File("."));
			//git.checkout().setName(name)
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
