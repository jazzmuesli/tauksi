package org.pavelreich.saaremaa;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

/**
 * taken from https://github.com/tsantalis/RefactoringMiner
 * 
 * @author preich
 *
 */
@Mojo(name = "refminer", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class RefactoringMinerMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {
		File dir = project.getBasedir();
		getLog().info("parent: " + project.getParentArtifact());
		
		if (project.getParent() != null) {
			getLog().info("Ignoring " + dir + " because it has parent  " + project.getParent());
			return;
		}
		getLog().info("Processing " + dir);

		try {
			Git git = Git.open(dir);
			Repository repository = git.getRepository();

			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

			CSVReporter reporter = new CSVReporter("refminer.csv", "commit", "refactoryingType","classesBefore","classesAfter","description");
			miner.detectAll(repository, "master", new RefactoringHandler() {
				@Override
				public void handle(RevCommit commitData, List<Refactoring> refactorings) {
					for (Refactoring ref : refactorings) {
						System.out.println(ref.toString());
						reporter.write(commitData.getId().getName(), 
								ref.getRefactoringType(), 
								ref.getName(),
								ref.getInvolvedClassesBeforeRefactoring().stream().collect(Collectors.joining(",")),
								ref.getInvolvedClassesAfterRefactoring().stream().collect(Collectors.joining(",")),
								ref.toString().replaceAll(";", ",")
						);
					}
				}
			});
			reporter.close();
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}
}
