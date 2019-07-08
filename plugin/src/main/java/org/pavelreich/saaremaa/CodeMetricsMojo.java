package org.pavelreich.saaremaa;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.github.mauricioaniche.ck.CSVExporter;


@Mojo( name = "metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, 
requiresDependencyResolution = ResolutionScope.NONE)
public class CodeMetricsMojo
    extends AbstractMojo
{

    @Parameter( defaultValue="${project}", readonly=true, required=true )
    MavenProject project;

    
    public void execute()
        throws MojoExecutionException
    {
    	processDir(project.getCompileSourceRoots());
    	processDir(project.getTestCompileSourceRoots());
    }


	private void processDir(List<String> dirs) {
    	getLog().info("dirs: " + dirs);
    	dirs.stream().forEach(dirName -> {
        	try {
        		getLog().info("Processing "  + dirName);
        		if (new File(dirName).exists()) {
        			CSVExporter.processDirectory(dirName, ";",
        	    			resolveFileName(dirName, CSVExporter.classFileName), 
        	    			resolveFileName(dirName, CSVExporter.methodFileName),
        	    			resolveFileName(dirName, CSVExporter.variableFileName),
        	    			resolveFileName(dirName, CSVExporter.fieldFileName));
        			
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	});
	}


	private String resolveFileName(String dirName, String fname) {
		return Paths.get(dirName, fname).toString();
	}
}
