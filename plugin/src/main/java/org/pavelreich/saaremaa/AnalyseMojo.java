package org.pavelreich.saaremaa;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "testan", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class AnalyseMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	public void execute() throws MojoExecutionException {

		for (String dirName : project.getTestCompileSourceRoots()) {
			try {
				// process test directory
				getLog().info("Processing " + dirName);
				if (new File(dirName).exists()) {
					String resultFileName = dirName + File.separator + "result.json";
					String assertsFileName = dirName + File.separator + "asserts.csv";
					String mockitoFileName = dirName + File.separator + "mockito.csv";
					Logger logger = new Logger() {

						@Override
						public void debug(String arg0) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(String arg0, Object arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(String arg0, Object... arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(String arg0, Throwable arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(Marker arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(String arg0, Object arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(Marker arg0, String arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(Marker arg0, String arg1, Object... arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(Marker arg0, String arg1, Throwable arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(String arg0) {
							getLog().error(arg0);
						}

						@Override
						public void error(String arg0, Object arg1) {
							getLog().error(arg0);							
						}

						@Override
						public void error(String arg0, Object... arg1) {
							getLog().error(arg0);							
						}

						@Override
						public void error(String arg0, Throwable arg1) {
							getLog().error(arg0,arg1);							
						}

						@Override
						public void error(Marker arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(String arg0, Object arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(Marker arg0, String arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(Marker arg0, String arg1, Object... arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(Marker arg0, String arg1, Throwable arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public String getName() {
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public void info(String arg0) {
							// TODO Auto-generated method stub
							getLog().info(arg0);
						}

						@Override
						public void info(String arg0, Object arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(String arg0, Object... arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(String arg0, Throwable arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(Marker arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(String arg0, Object arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(Marker arg0, String arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(Marker arg0, String arg1, Object... arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(Marker arg0, String arg1, Throwable arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public boolean isDebugEnabled() {
							// TODO Auto-generated method stub
							return false;
						}

						@Override
						public boolean isDebugEnabled(Marker arg0) {
							// TODO Auto-generated method stub
							return false;
						}

						@Override
						public boolean isErrorEnabled() {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public boolean isErrorEnabled(Marker arg0) {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public boolean isInfoEnabled() {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public boolean isInfoEnabled(Marker arg0) {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public boolean isTraceEnabled() {
							// TODO Auto-generated method stub
							return false;
						}

						@Override
						public boolean isTraceEnabled(Marker arg0) {
							// TODO Auto-generated method stub
							return false;
						}

						@Override
						public boolean isWarnEnabled() {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public boolean isWarnEnabled(Marker arg0) {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public void trace(String arg0) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(String arg0, Object arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(String arg0, Object... arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(String arg0, Throwable arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(Marker arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(String arg0, Object arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(Marker arg0, String arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(Marker arg0, String arg1, Object... arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(Marker arg0, String arg1, Throwable arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(String arg0) {
							getLog().warn(arg0);
						}

						@Override
						public void warn(String arg0, Object arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(String arg0, Object... arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(String arg0, Throwable arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(Marker arg0, String arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(String arg0, Object arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(Marker arg0, String arg1, Object arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(Marker arg0, String arg1, Object... arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(Marker arg0, String arg1, Throwable arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
							// TODO Auto-generated method stub
							
						}
						
					};
					TestFileProcessor processor = TestFileProcessor.run(logger, dirName, resultFileName);
					getLog().info("Creating files: " + assertsFileName + ", " + resultFileName + ", " + mockitoFileName);
					processor.writeCSVResults(assertsFileName);
					processor.writeMockito(mockitoFileName);
				}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
		}
	}

}
