package org.pavelreich.saaremaa;



import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;

import static org.mockito.Mockito.mock; 

public class MyMojoTest
{
    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() throws Throwable 
        {
        }

        @Override
        protected void after()
        {
        }
    };

    /**
     * @throws Exception if any
     */
    @Test
    public void testSomething()
            throws Exception
    {
        File pom = new File( "./" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        AnalyseMojo myMojo = ( AnalyseMojo ) rule.lookupConfiguredMojo( pom, "testan" );
        assertNotNull( myMojo );
        myMojo.execute();

        File outputDirectory = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        File touch = new File("src/test/java/asserts.csv");
        assertTrue( touch.exists() );

    }

    /** Do not need the MojoRule. */
    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn()
    {
    	File f = mock(File.class);
        assertTrue( true );
    }

}

