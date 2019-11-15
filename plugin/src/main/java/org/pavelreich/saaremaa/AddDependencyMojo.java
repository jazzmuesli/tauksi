package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.io.Files;

/**
 * mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency

 * @author preich
 *
 */
@Mojo(name = "add-dependency", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.NONE)
public class AddDependencyMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;

	@Parameter(property = "dependencies", defaultValue = "")
	private String dependencies;
	@Parameter(property = "overwrite", defaultValue = "false")
	private String overwrite;

	public void execute() throws MojoExecutionException {
		File file = project.getFile();
		getLog().info("pom.xml: " + file);
		try {
			if (dependencies == null || dependencies.trim().isEmpty()) {
				getLog().info(
						"No new dependencies provided. Run with -Ddependencies=\"org.junit:junit:4.12;org.banana:core:3.14\"");
				return;
			}
			addDependency(file, Boolean.valueOf(overwrite), dependencies);
		} catch (Exception e) {
			getLog().error("Can't parse  " + file + " due to " + e.getMessage(), e);
		}

	}

	public static void main(String[] args) throws Exception {
		addDependency(new File("pom.xml"), false, "apple:banana:1.0:test;apple:orange:1.3");
	}

	private static void addDependency(File srcFile, boolean overwrite, String newDependencies) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();
		Document document = db.parse(srcFile);

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		NodeList dependencies = (NodeList) xpath.evaluate("/project/dependencies", document, XPathConstants.NODESET);

		Node depsEl;
		if (dependencies.getLength() == 0) {
			depsEl = document.createElement("dependencies");
			NodeList buildEls = document.getElementsByTagName("project");
			if (buildEls.getLength() == 0) {
				throw new IllegalArgumentException("Can't find project in " + srcFile);
			}
			buildEls.item(0).appendChild(depsEl);
		} else {
			depsEl = dependencies.item(0);
		}

		for (String s : newDependencies.split(";")) {
			Element newDep = document.createElement("dependency");
			String[] vals = s.split(":");
			if (vals.length < 3) {
				throw new IllegalArgumentException("Can't parse " + newDependencies + ", in particular " + s
						+ " expected groupId:artifactId:version");
			}
			String groupId = vals[0];
			String artifactId = vals[1];
			String version = vals[2];
			addElement(document, newDep, "groupId", groupId);
			addElement(document, newDep, "artifactId", artifactId);
			addElement(document, newDep, "version", version);
			if (vals.length >= 4) {
				addElement(document, newDep, "scope", vals[3]);
			}

			depsEl.appendChild(newDep);
		}
		DOMSource source = new DOMSource(document);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		String dstFilename = srcFile.getName();
		if (!overwrite) {
			dstFilename += ".updated";
		} else {
			// leave original name, but make backups
			Files.copy(srcFile, new File(srcFile.getName() + ".bak"));
			String suffix = new SimpleDateFormat("ddMMyy-HHmmss").format(new Date());
			Files.copy(srcFile, new File(srcFile.getName() + "." + suffix + ".bak"));
		}

		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		transformer.transform(source, result);
		// TODO: find a more elegant way to remove xmlns
		String content = sw.toString().replaceAll(" xmlns=\"\"", "");
		FileWriter fw = new FileWriter(dstFilename);
		fw.write(content);
		fw.close();
	}

	protected static void addElement(Document document, org.w3c.dom.Element newDep, String name, String value) {
		Element el = document.createElement(name);
		el.appendChild(document.createTextNode(value));
		newDep.appendChild(el);
	}

}
