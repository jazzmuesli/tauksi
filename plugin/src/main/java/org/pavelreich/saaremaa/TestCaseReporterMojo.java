package org.pavelreich.saaremaa;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.pavelreich.saaremaa.extractors.SpoonTestExtractor;
import org.pavelreich.saaremaa.extractors.SurefireTestExtractor;
import org.slf4j.Logger;

import com.github.mauricioaniche.ck.plugin.CKMetricsMojo;

@Mojo(name = "analyse-testcases", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class TestCaseReporterMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public TestCaseReporterMojo() {
		super();
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Logger logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
			CombinedTestExtractor extractor = new CombinedTestExtractor(new SurefireTestExtractor(logger),
					new SpoonTestExtractor(logger));
			for (String dirName : CKMetricsMojo.extractDirs(project.getTestCompileSourceRoots())) {
				getLog().info("Processing dir=" + dirName);
				if (!new File(dirName).exists()) {
					continue;
				}

				Map<String, Set<String>> testCasesMap = extractor.extractTestCasesByClass(dirName);
				CSVReporter reporter = new CSVReporter(dirName + File.separator + "testcases.csv", "testClassName",
						"testCaseName", "i");
				for (Entry<String, Set<String>> entry : testCasesMap.entrySet()) {
					int i = 0;
					for (String testCase : entry.getValue()) {
						reporter.write(entry.getKey(), testCase, i++);
					}
					reporter.write(entry.getKey(), "TOTAL", i);
				}
				reporter.close();
			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

}
