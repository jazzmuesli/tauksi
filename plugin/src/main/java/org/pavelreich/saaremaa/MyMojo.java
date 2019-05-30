package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;
    @Parameter( defaultValue="${project}", readonly=true, required=true )
    MavenProject project;
    
    public Function<Number, String> getStringFun() {
    	return x->String.valueOf(x);
    }
    
    public void execute()
        throws MojoExecutionException
    {
    	
    	getLog().info("compileSourceRoots:" + project.getCompileSourceRoots());
    	getLog().info("dependencies: " + project.getDependencies());
    	getLog().info("compileDeps: " + project.getCompileDependencies());
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
}
