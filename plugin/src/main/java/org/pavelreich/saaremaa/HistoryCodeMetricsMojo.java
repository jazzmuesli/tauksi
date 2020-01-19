package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jgit.api.Git;
import org.pavelreich.saaremaa.CodeMetricsMojo.InternalMojo;
import org.pavelreich.saaremaa.CodeMetricsMojo.MongoMetricsWriter;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

import me.tongfei.progressbar.ProgressBar;

@Mojo(name = "history-metrics", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class HistoryCodeMetricsMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;
	@Parameter( property = "classesOnly", defaultValue = "false")
	private String classesOnly;
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	private MongoDBClient db;

	public HistoryCodeMetricsMojo() {
		super();
		db = new MongoDBClient(getClass().getSimpleName());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Git git = Git.open(new File("."));
			final String dirName = project.getBasedir().getAbsolutePath() + File.separator;
			getLog().info("Using dirName: " + dirName);
			Set<String> files = new LinkedHashSet<>();
			InternalMojo xmojo = new InternalMojo(Optional.empty(), db, project) {
				protected void processSourceDirectory(String xdirName) {
					try {
						getLog().info("processSourceDirectory:" + xdirName);
						CSVParser classParser = Helper.getParser(xdirName + "/class.csv", "file");
						Set<String> cfiles = classParser.getRecords().stream().map(x->x.get("file").replaceAll(dirName, "")).collect(Collectors.toSet());
						files.addAll(cfiles);
					} catch (Exception e) {
						getLog().error(e.getMessage(), e);
					}
				};
			};
			xmojo.execute();
			String runId = UUID.randomUUID().toString();
			getLog().info("using runId=" + runId);
			getLog().info("Found " + files.size() + " files, first: " + files.stream().findFirst());
			CSVParser gitParser = Helper.getParser(dirName + "git-history.csv", "commit");
			Function<String, Boolean> isFilenameRelevant;
			if (Boolean.valueOf(classesOnly)) {
				isFilenameRelevant = (p) -> files.contains(p);
			} else {
				isFilenameRelevant = (p) -> p.endsWith(".java");
			}
			 
			Collection<Pair<String,String>> commits = gitParser.getRecords().stream()
					.filter(p -> isFilenameRelevant.apply(p.get("newFilename"))).map(x -> 
					Pair.of(x.get("commit"), x.get("timestamp")))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			getLog().info("Found " + commits.size() + " commits");
			for (Pair<String,String> commitWithTimestamp : ProgressBar.wrap(commits, "commits")) {
				getLog().info("checking out commit=" + commitWithTimestamp);
				String commit = commitWithTimestamp.getLeft();
				git.checkout().setName(commit).call();
				InternalMojo mojo = new InternalMojo(Optional.of(commit), db, project) {
					protected org.pavelreich.saaremaa.CodeMetricsMojo.MongoMetricsWriter createMongoMetricsWriter(
							String dirName, com.github.mauricioaniche.ck.plugin.MetricsCSVWriter csvWriter) {
						return new MongoMetricsWriter(prefix, db, project, getLog(), dirName, csvWriter) {
							protected Document toDocument(String dirName, java.util.Set<String> cols,
									org.apache.commons.csv.CSVRecord record) {
								Document doc = super.toDocument(dirName, cols, record);
								doc = doc.append("runId", runId).append("commitTimestamp", commitWithTimestamp.getRight()).append("commit", commit);
								return doc;
							};
						};
					};
				};
				mojo.execute();
			}
			Bson query = Filters.and(Filters.eq("runId",runId), Filters.exists("runId"), Filters.exists("prefix"));
			getLog().info("Loading " + query);
			List<Document> docs = db.find("classMetrics", query);
			CSVReporter reporter = new CSVReporter(outputDirectory.getAbsolutePath()+File.separator+"commits.csv", "class","commit","timestamp","loc");
			getLog().info("Found " + docs.size() + " Documents");
			for (Document doc : docs) {
				reporter.write(doc.getString("class"), doc.getString("commit"), doc.getString("commitTimestamp"), doc.getString("loc"));
			}
			reporter.close();
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}
	
	static class CommitDetails {
		String commit, file;
		long time;
		public CommitDetails(String commit, String file, long time) {
			super();
			this.commit = commit;
			this.file = file;
			this.time = time;
		}
		
	}
	public static void main(String[] args) throws IOException {
		Logger LOG = LoggerFactory.getLogger(HistoryCodeMetricsMojo.class);
		MongoDBClient mdb = new MongoDBClient("main");
		String runId = "ae4737e3-d93a-4551-8c14-1e1f7d530f07";//jfreechart
		runId="53823490-7102-434a-b4c3-c1fcc3a76e8b";//commons-math
//		runId="e582dd29-6c37-4439-b2eb-12fe1819fde8";//ck
//		runId="535b72d3-d213-4fe7-91f9-81ae6a665d7d";//ant
//		runId="636ff432-c70f-4051-b543-d90fc3291806";//commons-collections
		Bson query = Filters.and(Filters.eq("runId",runId), Filters.exists("runId"), Filters.exists("prefix"));
		LOG.info("Loading " + query);
		List<Document> docs = mdb.find("classMetrics", query);
		CSVReporter reporter = new CSVReporter("/tmp/commits.csv", "class","commit","timestamp","loc");
		LOG.info("Found " + docs.size() + " Documents");
		for (Document doc : docs) {
			reporter.write(doc.getString("class"), doc.getString("commit"), doc.getString("commitTimestamp"), doc.getString("loc"));
		}
		Map<String, List<Document>> docsByClass = docs.stream().collect(Collectors.groupingBy(d->d.getString("class")));
		Map<String, Set<String>> locByClass = docs.stream().collect(Collectors.groupingBy(d->d.getString("class"),
				Collectors.mapping(e->e.getString(metric), Collectors.toCollection(LinkedHashSet::new))));
		LOG.info("Found " + locByClass.size());
		Map<String, Integer> locsByClass = locByClass.entrySet().stream().collect(Collectors.toMap(k->k.getKey(),  v->v.getValue().size()));
		Map<String, Integer> sortedLocsByClass = locsByClass.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(15)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		sortedLocsByClass.entrySet().forEach(x->{
			String className = x.getKey();
			Integer locs = x.getValue();
			LOG.info("class: " +className + ", locs: " + locs+"=" + locByClass.getOrDefault(className, Collections.emptySet()));
			List<Document> relDocs = docsByClass.getOrDefault(className, Collections.emptyList());
			Map<String, String> xs = relDocs.stream().collect(Collectors.toMap(d->d.getString(metric), relDoc -> getDoc(relDoc),
					(oldValue, newValue) -> oldValue, LinkedHashMap::new));
			xs.entrySet().forEach(s -> LOG.info(s.getValue()));;
		});

	}
	static String metric = "loc";

	protected static String getDoc(Document relDoc) {
		return "commit: "+ relDoc.getString("prefix") + ", " + metric + ": " + relDoc.getString(metric) + " timestamp: "+ new Date(1000L*Long.valueOf(relDoc.getString("commitTimestamp")));
	}

}
