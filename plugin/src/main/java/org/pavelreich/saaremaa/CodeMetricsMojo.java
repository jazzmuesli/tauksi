package org.pavelreich.saaremaa;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.github.mauricioaniche.ck.CSVExporter;


@Mojo( name = "metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class CodeMetricsMojo
    extends AbstractMojo
{

    @Parameter( defaultValue="${project}", readonly=true, required=true )
    MavenProject project;

    
    public void execute()
        throws MojoExecutionException
    {
    	
    	List<String> dirs = new ArrayList<String>();
    	dirs.addAll(project.getTestCompileSourceRoots());
    	dirs.addAll(project.getCompileSourceRoots());
    	getLog().info("dirs: " + dirs);
    	dirs.parallelStream().forEach(dirName -> {
        	try {
        		getLog().info("Processing "  + dirName);
        		if (new File(dirName).exists()) {
        			CSVExporter.processDirectory(dirName, ";",
        	    			resolveFileName(CSVExporter.classFileName), 
        	    			resolveFileName(CSVExporter.methodFileName),
        	    			resolveFileName(CSVExporter.variableFileName),
        	    			resolveFileName(CSVExporter.fieldFileName));
        			
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	});
    }


	private String resolveFileName(String fname) {
		return Paths.get(project.getBasedir().getAbsolutePath(), fname).toString();
	}
}
