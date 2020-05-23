package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bson.Document;
import org.pavelreich.saaremaa.mongo.MongoDBClient;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.plugin.CKMetricsMojo;
import com.github.mauricioaniche.ck.plugin.MetricsCSVWriter;
import com.github.mauricioaniche.ck.plugin.MetricsWriter;


@Mojo(name = "metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class CodeMetricsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	private MongoDBClient db;
	private InternalMojo mojo;

	public CodeMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
		mojo = new InternalMojo(Optional.empty(), db, project);
	}



	static class InternalMojo extends CKMetricsMojo {
		private MongoDBClient db;
		protected Optional<String> prefix;

		public InternalMojo(Optional<String> prefix, MongoDBClient db, MavenProject project) {
			this.prefix = prefix;
			this.db = db;
			this.project = project;
		}

		@Override
		protected MetricsWriter createMetricsWriter(String dirName) {
			getLog().info("Analysing dirName=" + dirName);
			MetricsWriter csvWriter = super.createMetricsWriter(dirName);
			if (csvWriter instanceof MetricsCSVWriter) {
				return createMongoMetricsWriter(dirName, (MetricsCSVWriter) csvWriter);
			} else {
				return csvWriter;
			}
		}
		
		public void setProject(MavenProject project) {
			this.project = project;
		}

		protected MongoMetricsWriter createMongoMetricsWriter(String dirName, MetricsCSVWriter csvWriter) {
			return new MongoMetricsWriter(prefix, db, project, getLog(), dirName, csvWriter);
		}

		@Override
		public void execute() throws MojoExecutionException {
			super.execute();
		}

	}

	static class MongoMetricsWriter implements MetricsWriter {

		private MetricsCSVWriter csvWriter;
		private String dirName;
		private MongoDBClient db;
		private MavenProject project;
		private Log log;
		private Optional<String> prefix;

		public MongoMetricsWriter(Optional<String> prefix, MongoDBClient db, MavenProject project, Log log, String dirName,
				MetricsCSVWriter csvWriter) {
			this.prefix = prefix;
			this.db = db;
			this.project = project;
			this.log = log;
			this.csvWriter = csvWriter;
			this.dirName = dirName;
		}

		public Log getLog() {
			return log;
		}
		
		@Override
		public void notify(CKClassResult result) {
			csvWriter.notify(result);
		}

		@Override
		public void finish() {
			csvWriter.finish();
			exportToMongo(dirName, "class", csvWriter.getClassFileName());
			exportToMongo(dirName, "method", csvWriter.getMethodFileName());
			exportToMongo(dirName, "variable", csvWriter.getVariableFileName());
			exportToMongo(dirName, "field", csvWriter.getFieldFileName());
			db.waitForOperationsToFinish();
		}

		private void exportToMongo(String dirName, String docName, String fname) {
			CSVParser parser;
			try {
				parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
						CSVFormat.DEFAULT.withFirstRecordAsHeader());
				Map<String, Integer> headerMap = parser.getHeaderMap();
				getLog().info("dirName: " + dirName + ", fname: " + fname + ", headers: " + headerMap);
				if (headerMap != null) {
					List<Document> documents = parser.getRecords().stream()
							.map(record -> toDocument(dirName, headerMap.keySet(), record)).collect(Collectors.toList());
					getLog().info("From " + fname + " read " + documents.size() + " documents");
					if (!documents.isEmpty()) {
						db.insertCollection(docName + "Metrics", documents);
					}

				}
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

		}
		

		protected Document toDocument(String dirName, Set<String> cols, CSVRecord record) {
			Document doc = new Document().append("project", project.getArtifact().getId()).append("dirName", dirName);
			if (prefix.isPresent()) {
				doc = doc.append("prefix", prefix.get());
			}
			for (String col : cols) {
				doc = doc.append(col, record.get(col));
			}
			return doc;
		}

	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			mojo.setProject(project);
			mojo.execute();
		} catch (Exception e) {
			e.printStackTrace();
			getLog().error(e.getMessage(), e);
		}
	}


}
