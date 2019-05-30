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
import org.pavelreich.saaremaa.testdepan.TestFileProcessor;
import org.pavelreich.saaremaa.testdepan.TestFileProcessor.MyClass;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "analyse", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
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
    @Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
    private Map<String, Artifact> pluginArtifactMap;
    
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;
    
    @Component
    private RepositorySystem repositorySystem;
    
    private Set<Artifact> getDevArtifacts() {
          ArtifactResolutionRequest request = new ArtifactResolutionRequest()
              .setArtifact(pluginArtifactMap.get("org.pavelreich.saaremaa:plugin"))
              .setResolveTransitively(true)
              .setLocalRepository(localRepository);
          ArtifactResolutionResult result = repositorySystem.resolve(request);
          return result.getArtifacts();
      }

    public void execute()
        throws MojoExecutionException
    {
    	
		LinkedHashSet<String> classpath = prepareClasspath();
    	List<String> junitClassNames = new ArrayList();
    	for(String dirName: project.getTestCompileSourceRoots()) {
        	try {
        		// process test directory
        		getLog().info("Processing "  + dirName);
        		if (new File(dirName).exists()) {
    				TestFileProcessor processor = TestFileProcessor.run(dirName, dirName+File.separator+"result.json");
    				// extract junit class names
    				List<MyClass> elements = processor.getElements();
    				for (MyClass element : elements) {
    					junitClassNames.add(element.getClassName());
    				}
        		}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
    	}
		getLog().info("junitClassNames: " + junitClassNames);

		// generate a shell script that we can execute separately
		createShellScript(classpath, junitClassNames);
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

	private void createShellScript(LinkedHashSet<String> classpath, List<String> junitClassNames) {
		String cmd = "java -classpath " + classpath.stream().collect(Collectors.joining(File.pathSeparator)) 
				+ " " + MeasureCodeCoverageByTestAndProdMethod.class.getCanonicalName() + " " + junitClassNames.stream().collect(Collectors.joining(" "));
		getLog().info("cmd: "+ cmd);
		try {
			FileWriter fw = new FileWriter("measure.sh");
			fw.write(cmd);
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * TODO: make it less hacky.
	 * 
	 * Here we need classpath of the target project + target/test-clases, target/classes + 
	 * dependencies of this plugin.
	 * Note that this plugin won't be added to target project's pom.xml, but rather executed 
	 * mvn org.pavelreich.saaremaa:plugin:analyse
	 * @return
	 * @throws MojoExecutionException
	 */
	private LinkedHashSet<String> prepareClasspath() throws MojoExecutionException {
		LinkedHashSet<String> classpath = new LinkedHashSet();
    	
    	try {
    		getLog().info("artifacts: " + project.getArtifacts());
			classpath.addAll(project.getTestClasspathElements());
			classpath.addAll(project.getCompileClasspathElements());
			// copied from https://github.com/tbroyer/gwt-maven-plugin/blob/54fe4621d1ee5127b14030f6e1462de44bace901/src/main/java/net/ltgt/gwt/maven/CompileMojo.java#L295
			ClassWorld world = new ClassWorld();
		    ClassRealm realm;
		    try {
		      realm = world.newRealm("gwt", null);
		      for (String elt : project.getCompileSourceRoots()) {
		        URL url = new File(elt).toURI().toURL();
		        realm.addURL(url);
		        if (getLog().isDebugEnabled()) {
		          getLog().debug("Source root: " + url);
		        }
		      }
		      for (String elt : project.getCompileClasspathElements()) {
		        URL url = new File(elt).toURI().toURL();
		        realm.addURL(url);
		        if (getLog().isDebugEnabled()) {
		          getLog().debug("Compile classpath: " + url);
		        }
		      }
		      // gwt-dev and its transitive dependencies
		      for (Artifact elt : getDevArtifacts()) {
		        URL url = elt.getFile().toURI().toURL();
		        realm.addURL(url);
		        if (getLog().isDebugEnabled()) {
		          getLog().debug("Compile classpath: " + url);
		        }
		      }
		      realm.addURL(pluginArtifactMap.get("org.pavelreich.saaremaa:plugin").getFile().toURI().toURL());
		      List<String> urls = Arrays.asList(realm.getURLs()).stream().map(x->{
				try {
					return new File(x.toURI()).getAbsolutePath();
				} catch (Exception e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
			}).collect(Collectors.toList());
			getLog().info("realm: " + urls);
			urls.stream().forEach(x->classpath.add(x));
		    } catch (Exception e) {
		      throw new MojoExecutionException(e.getMessage(), e);
		    }

		} catch (DependencyResolutionRequiredException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return classpath;
	}
}
