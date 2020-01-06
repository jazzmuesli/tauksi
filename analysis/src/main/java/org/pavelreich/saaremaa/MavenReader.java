package org.pavelreich.saaremaa;

import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class MavenReader {

	public static void main(String[] args) throws Exception{
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = reader.read(new FileReader("pom.xml"));
		
		System.out.println("model: " + model.getBuild().getSourceDirectory());
	}
}
