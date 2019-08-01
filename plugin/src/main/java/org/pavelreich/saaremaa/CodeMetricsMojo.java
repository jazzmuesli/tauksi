package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bson.Document;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

import com.github.mauricioaniche.ck.CSVExporter;

@Mojo(name = "metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class CodeMetricsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
	private static final String delim = ";";

	public CodeMetricsMojo() {
		db = new MongoDBClient(getClass().getSimpleName());
	}
	public void execute() throws MojoExecutionException {
		processDir(project.getCompileSourceRoots());
		processDir(project.getTestCompileSourceRoots());
		db.waitForOperationsToFinish();
	}

	private void processDir(List<String> dirs) {
		getLog().info("dirs: " + dirs);
		dirs.stream().forEach(dirName -> {
			try {
				getLog().info("Processing " + dirName);
				if (new File(dirName).exists()) {
					String classFname = resolveFileName(dirName, CSVExporter.classFileName);
					String methodFname = resolveFileName(dirName, CSVExporter.methodFileName);
					String varFname = resolveFileName(dirName, CSVExporter.variableFileName);
					String fieldFname = resolveFileName(dirName, CSVExporter.fieldFileName);

					CSVExporter.processDirectory(dirName, delim, classFname, methodFname, varFname, fieldFname);
					exportToMongo(dirName, "class", classFname);
					exportToMongo(dirName, "method", methodFname);
					exportToMongo(dirName, "variable", varFname);
					exportToMongo(dirName, "field", fieldFname);
				}
			} catch (Exception e) {
				getLog().error(e.getMessage(), e);
			}
		});
	}
	
	private void exportToMongo(String dirName, String docName, String fname) throws IOException {
        CSVParser parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
                CSVFormat.newFormat(';').withFirstRecordAsHeader());

		Map<String, Integer> headerMap = parser.getHeaderMap();
		getLog().info("dirName: " + dirName + ", fname: " + fname +", headers: " + headerMap);
		if (headerMap != null) {
			List<Document> documents = parser.getRecords().stream().map(record -> toDocument(dirName, headerMap.keySet(), record)).collect(Collectors.toList());
			getLog().info("From " + fname + " read " + documents.size() + " documents");
			if (!documents.isEmpty()) {
				db.insertCollection(docName + "Metrics", documents);				
			}
			
		}
	}

	private Document toDocument(String dirName, Set<String> cols, CSVRecord record) {
		Document doc = new Document().append("project", project.getArtifact().getId()).append("dirName", dirName);
		for (String col : cols) { 
			doc = doc.append(col, record.get(col));
		}
		return doc;
	}

	private String resolveFileName(String dirName, String fname) {
		return Paths.get(dirName, fname).toString();
	}
	
}
