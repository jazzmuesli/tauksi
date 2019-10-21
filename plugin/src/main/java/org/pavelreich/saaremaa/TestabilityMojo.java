package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.json.JSONObject;
import org.json.XML;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


@Mojo(name = "parse-testability", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class TestabilityMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
//	private TestabilityExplorerMojo mojo;

	public TestabilityMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
//		mojo = new TestabilityExplorerMojo();
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
//		try {
//			mojo.execute();
//		} catch (Exception e) {
//			getLog().error(e.getMessage(), e);
//		}
		try {
			String fname = project.getBuild().getDirectory() + File.separator + "testability.xml";
			List<org.bson.Document> docs = parseTestabilityXml(fname);
			getLog().info("Parse " + fname + " and insert " + docs.size() + " into mongo");
			if (!docs.isEmpty()) {
				db.insertCollection("testabilityExplorer", docs);
				db.waitForOperationsToFinish();
				
			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}

	}

	static List<org.bson.Document> parseTestabilityXml(String fname) {
		File file = new File(fname);
		if (!file.exists()) {
			return Collections.emptyList();
		}
		try {
			JSONObject xmlJSONObj = XML.toJSONObject(new FileReader(file));
			org.bson.Document parsed = org.bson.Document.parse(xmlJSONObj.toString());
			org.bson.Document testabilityDoc = (org.bson.Document) parsed.get("testability");
			List<org.bson.Document> classDocs = (List<org.bson.Document>) testabilityDoc.get("class");
			return classDocs;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	private static List<org.bson.Document> parseXml(File file)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document xml = db.parse(file);
		// Get XPath
		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		NodeList nodes = (NodeList) xpath.evaluate("//class", xml, XPathConstants.NODESET);
		System.out.println("nodes: " + nodes);

		List<org.bson.Document> result = new ArrayList();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			String cn = node.getAttributes().getNamedItem("class").getNodeValue();
			String cost = node.getAttributes().getNamedItem("cost").getNodeValue();
			result.add(new org.bson.Document("class", cn).append("cost", cost));
		}
		return result;
	}

}
