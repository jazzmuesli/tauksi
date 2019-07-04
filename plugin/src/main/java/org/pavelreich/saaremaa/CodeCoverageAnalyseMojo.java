package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.pavelreich.saaremaa.codecov.MeasureCodeCoverageByTestAndProdMethod;
import org.pavelreich.saaremaa.testdepan.ITestClass;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;
import org.slf4j.Logger;


/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "coverage", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class CodeCoverageAnalyseMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;
    @Parameter( defaultValue="${project}", readonly=true, required=true )
    MavenProject project;
    @Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
    private Map<String, Artifact> pluginArtifactMap;
    
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;
    
    @Component
    private RepositorySystem repositorySystem;
    


    public void execute()
        throws MojoExecutionException
    {
    	
		LinkedHashSet<String> classpath = DependencyHelper.prepareClasspath(project, localRepository, repositorySystem, pluginArtifactMap, getLog());
		Logger logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
    	List<String> junitClassNames = new ArrayList();
    	for(String dirName: project.getTestCompileSourceRoots()) {
        	try {
        		// process test directory
        		getLog().info("Processing "  + dirName);
        		if (new File(dirName).exists()) {
					TestFileProcessor processor = TestFileProcessor.run(logger, dirName, null);
    				// extract junit class names
					List<ITestClass> elements = processor.getElements();
    				for (ITestClass element : elements) {
    					junitClassNames.add(element.getClassName());
    				}
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			} finally {
        		if (new File(dirName).exists()) {
        			createShellScript(dirName, classpath, junitClassNames);
        		}
			}
    	}
		getLog().info("junitClassNames: " + junitClassNames);

		// generate a shell script that we can execute separately
        touchFile();
    }

	private void touchFile() throws MojoExecutionException {
		File f = outputDirectory;
        if ( !f.exists() )
        {
            f.mkdirs();
        }

        File touch = new File( f, "touch.txt" );

        FileWriter w = null;
        try
        {
            w = new FileWriter( touch );

            w.write( "touch.txt" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file " + touch, e );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
	}

	private void createShellScript(String dir, LinkedHashSet<String> classpath, List<String> junitClassNames) {
		if (junitClassNames.isEmpty()) {
			getLog().info("No junits in " + dir);
			return;
		}
		String cmd = "java -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator)) 
				+ " " + MeasureCodeCoverageByTestAndProdMethod.class.getCanonicalName() + " " + junitClassNames.stream().collect(Collectors.joining(" "));
		getLog().info("cmd: "+ cmd);
		try {
			FileWriter fw = new FileWriter(dir+File.separator+"measure.sh");
			fw.write(cmd);
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
