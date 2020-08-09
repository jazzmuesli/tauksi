package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * mvn
 * -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test"
 * -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
 * 
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
	@Parameter(property = "depFile", defaultValue = "")
	private String depFile;
	@Parameter(property = "depDir", defaultValue = "")
	private String depDir;

	static class Dependency {
		String groupId, artifactId, version;
		String fileName;
		String scope;

		public Dependency(String groupId, String artifactId, String version, String fileName, String scope) {
			if (groupId == null || artifactId == null || version == null) {
				throw new IllegalArgumentException("Can't parse " + groupId + ":" + artifactId + ":" + version
						+ ",  expected groupId:artifactId:version");
			}

			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.fileName = fileName;
		}

		String asTriple() {
			return groupId + ":" + artifactId + ":" + version;
		}

		static Dependency fromTriple(String s) {
			String[] vals = s.split(":");
			return new Dependency(vals[0], vals[1], vals[2], null, vals[3] != null ? vals[3] : null);
		}

		@Override
		public String toString() {
			return "Dependency [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version
					+ ", fileName=" + fileName + ", scope=" + scope + "]";
		}

		public String createInstallCommand() {
			String cmd = "mvn install:install-file " + "-DgroupId=" + groupId + " " + "-DartifactId=" + artifactId + " "
					+ "-Dversion=" + version + " " + "-Dpackaging=jar " + "-Dfile=" + fileName;
			return cmd;
		}

	}

	public static Collection<Dependency> findDependenciesInDir(Path path) throws Exception {
		List<String> files = java.nio.file.Files.walk(path).filter(p -> p.toFile().getName().endsWith(".jar"))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
		Logger logger = LoggerFactory.getLogger(AddDependencyMojo.class);
		logger.info("Found " + files + " dependencies");
		Collection<Dependency> deps = new LinkedHashSet<Dependency>();
		for (String f : files) {
			deps.addAll(findDependency(f, logger));
		}
		return deps;

	}

	public void execute() throws MojoExecutionException {
		File file = project.getFile();
		getLog().info("Reading from pom.xml: " + file);
		MavenLoggerAsSLF4jLoggerAdaptor logger = new MavenLoggerAsSLF4jLoggerAdaptor(getLog());
		try {
			Collection<Dependency> deps = null;
			if (depDir != null && !depDir.trim().isEmpty()) {
				Path path = new File(depDir).toPath();
				deps = findDependenciesInDir(path);
				//dependencies = deps.stream().map(Dependency::asTriple).collect(Collectors.joining(";"));
				logger.info("Will add dependencies " + dependencies);
			} else if (depFile != null && !depFile.trim().isEmpty()) {
				deps = findDependency(depFile, logger);
				//dependencies = deps.stream().map(Dependency::asTriple).collect(Collectors.joining(";"));
			} else if (dependencies == null || dependencies.trim().isEmpty()) {
				logger.info("No new dependencies provided. Run with -DdepDir=lib/ -Ddependencies=\"org.junit:junit:4.12;org.banana:core:3.14\"");
				return;
			} else if (dependencies != null && !dependencies.trim().isEmpty()){
				deps = Arrays.stream(dependencies.split(";")).map(Dependency::fromTriple).collect(Collectors.toList());
			}
			addDependency(file, Boolean.valueOf(overwrite), deps, logger);
			String installCmds = deps.stream().filter(p->p.fileName!=null).map(Dependency::createInstallCommand).collect(Collectors.joining("\n"));
			if (installCmds != null && !installCmds.trim().isEmpty()) {
				File installFile = new File("install-deps.sh");
				logger.info("Writing install commands to " + installFile.getAbsolutePath());
				Files.write(installCmds, installFile, Charset.defaultCharset());
			}
		} catch (Exception e) {
			getLog().error("Can't parse  " + file + " due to " + e.getMessage(), e);
		}

	}

	/**
	 * example
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Logger logger = LoggerFactory.getLogger(AddDependencyMojo.class);
		Path path = new File("/Users/preich/projects/102_squirrel-sql").toPath();
		Collection<Dependency> deps = findDependenciesInDir(path);
		System.out.println(deps);
//		findDependency("lib/ehcache-1.2.3.jar", logger);
//		addDependency(new File("pom.xml"), false, "apple:banana:1.0:test;apple:orange:1.3", logger);
	}

	private static Collection<Dependency> findDependency(String fname, Logger logger) throws IOException {
		byte[] bytes = java.nio.file.Files.readAllBytes(new File(fname).toPath());
		String sha1code = Hashing.sha1().hashBytes(bytes).toString();
		List<String> deps = new ArrayList<>();
		Collection<Dependency> dependencies = new ArrayList();
		String url = "http://search.maven.org/solrsearch/select?q=1:%22" + sha1code + "%22&rows=20&wt=json";
		logger.info("For file " + fname + " got SHA1: " + sha1code + ", requesting " + url);
		InputStream in = new URL(url).openStream();
		try {
			String json = IOUtils.toString(in);
			JsonWriterSettings writerSettings = new JsonWriterSettings(JsonMode.SHELL, true);
			org.bson.Document parsed = org.bson.Document.parse(json.toString());
			if (parsed != null && parsed.containsKey("response")) {
				org.bson.Document response = parsed.get("response", org.bson.Document.class);
				int found = response.getInteger("numFound", 0);
				if (found > 0) {
					List<org.bson.Document> docs = response.getList("docs", org.bson.Document.class);
					for (org.bson.Document doc : docs) {
						String dep = doc.getString("id");
						if (deps.isEmpty()) {
							deps.add(dep);
							Dependency d = new Dependency(doc.getString("g"), doc.getString("a"), doc.getString("v"),
									fname, null);
							dependencies.add(d);
						} else {
							logger.info("Already have dependency " + deps + ", ignoring " + dep);
						}
					}
				}
				logger.info("For file " + fname + " found " + found + " dependencies: " + deps);
			} else {
				logger.info("Parsed result: " + parsed.toJson(writerSettings));
			}

		} finally {
			IOUtils.closeQuietly(in);
		}
		return dependencies;
	}

	private static void addDependency(File srcFile, boolean overwrite, Collection<Dependency> deps, Logger logger)
			throws Exception {
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

		for (Dependency d : deps) {
			if (d.fileName != null) {
				String cmd = d.createInstallCommand();
				depsEl.appendChild(document.createComment(cmd));
			}
			Element newDep = document.createElement("dependency");
			addElement(document, newDep, "groupId", d.groupId);
			addElement(document, newDep, "artifactId", d.artifactId);
			addElement(document, newDep, "version", d.version);
			if (d.scope != null) {
				addElement(document, newDep, "scope", d.scope);
			}
			depsEl.appendChild(newDep);
		}
		DOMSource source = new DOMSource(document);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		String dstFilename = srcFile.getAbsolutePath();
		if (!overwrite) {
			dstFilename += ".updated";
		} else {
			// leave original name, but make backups
			Files.copy(srcFile, new File(dstFilename + ".bak"));
			String suffix = new SimpleDateFormat("ddMMyy-HHmmss").format(new Date());
			Files.copy(srcFile, new File(dstFilename + "." + suffix + ".bak"));
		}

		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		transformer.transform(source, result);
		// TODO: find a more elegant way to remove xmlns
		String content = sw.toString().replaceAll(" xmlns=\"\"", "");
		logger.info("Writing to pom.xml: " + dstFilename);
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
