package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
			Collection<Pair<String,String>> commits = gitParser.getRecords().stream()
					.filter(p -> files.contains(p.get("newFilename"))).map(x -> 
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
		String dirName = "/Users/preich/Documents/git/jfreechart/";
		Bson query = Filters.and(Filters.eq("dirName",dirName+"src/main/java"), Filters.exists("runId"), Filters.exists("prefix"));
		List<Document> docs = mdb.find("classMetrics", query);
		LOG.info("Found " + docs.size());
		
		Map<String, List<Document>> docsByClass = docs.stream().collect(Collectors.groupingBy(d->d.getString("class")));
		Map<String, Set<String>> locByClass = docs.stream().collect(Collectors.groupingBy(d->d.getString("class"),
				Collectors.mapping(e->e.getString("loc"), Collectors.toCollection(LinkedHashSet::new))));
		LOG.info("Found " + locByClass.size());
		Map<String, Integer> locsByClass = locByClass.entrySet().stream().collect(Collectors.toMap(k->k.getKey(),  v->v.getValue().size()));
		Map<String, Integer> sortedLocsByClass = locsByClass.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		sortedLocsByClass.entrySet().forEach(x->{
			String className = x.getKey();
			Integer locs = x.getValue();
			LOG.info("class: " +className + ", locs: " + locs+"=" + locByClass.getOrDefault(className, Collections.emptySet()));
			List<Document> relDocs = docsByClass.getOrDefault(className, Collections.emptyList());
			relDocs.forEach(relDoc -> LOG.info("commit: "+ relDoc.getString("prefix") + ", timestamp: "+ relDoc.getString("commitTimestamp")));;
		});

	}

}
