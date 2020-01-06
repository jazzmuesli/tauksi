package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
		mojo = new InternalMojo();
	}



	class InternalMojo extends CKMetricsMojo {
		@Override
		protected MetricsWriter createMetricsWriter(String dirName) {
			getLog().info("Analysing dirName=" + dirName);
			MetricsWriter csvWriter = super.createMetricsWriter(dirName);
			if (csvWriter instanceof MetricsCSVWriter) {
				return new MongoMetricsWriter(dirName, (MetricsCSVWriter) csvWriter);
			} else {
				return csvWriter;
			}
		}
		
		@Override
		public void execute() throws MojoExecutionException {
			this.project = CodeMetricsMojo.this.project;
			super.execute();
		}
		
	}
	
	class MongoMetricsWriter implements MetricsWriter {

		private MetricsCSVWriter csvWriter;
		private String dirName;

		public MongoMetricsWriter(String dirName, MetricsCSVWriter csvWriter) {
			this.csvWriter = csvWriter;
			this.dirName = dirName;
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
		

		private Document toDocument(String dirName, Set<String> cols, CSVRecord record) {
			Document doc = new Document().append("project", project.getArtifact().getId()).append("dirName", dirName);
			for (String col : cols) {
				doc = doc.append(col, record.get(col));
			}
			return doc;
		}

	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		mojo.execute();
	}


}
