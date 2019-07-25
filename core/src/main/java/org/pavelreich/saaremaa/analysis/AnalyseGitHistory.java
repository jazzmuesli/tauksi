package org.pavelreich.saaremaa.analysis;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyseGitHistory {

	private static final Logger LOG = LoggerFactory.getLogger(AnalyseGitHistory.class);

	static class ProdTestPair {
		GitResource prod;
		GitResource test;

		public ProdTestPair(GitResource prod, GitResource test) {
			this.prod = prod;
			this.test = test;
		}

	}

	
	public static class GitResource {
		String fname;
		String commit;
		long timestamp;
		private String basename;
		private boolean isTest;
		private Long changedLines;

		public GitResource(String fname, String firstCommit, long firstTimestamp, Long changedLines) {
			this.fname = fname;
			this.commit = firstCommit;
			this.timestamp = firstTimestamp;
			this.basename = fname
					.replaceAll("^.*?/src","")
					.replaceAll("^/", "")
					.replaceAll("^src/", "")
					.replaceAll("^(test|main)/", "")
					.replaceAll("^java/", "")
					.replaceAll(".java$", "")
					.replaceAll("Tests?$", "")
					.replaceAll("/", ".");
			this.isTest = fname.endsWith("Test.java");
			this.changedLines = changedLines;
		}

		public String getBaseName() {
			// TODO Auto-generated method stub
			return basename;
		}

	}

	static class Session {

		Map<String, GitResource> addedFiles = new HashMap();
		Map<String, List<GitResource>> resourcesByCommit = new HashMap();
		Set<String> authors = new HashSet();

		public Object processGitHistory(CSVRecord record) {
			String changeType = record.get("changeType");
			String fname = record.get("newFilename");
			if (!fname.endsWith(".java")) {
				return null;
			}
			String commit = record.get("commit");
			Long timestamp = Long.valueOf(record.get("timestamp"));
			Long changedLines = Long.valueOf(record.get("newLines"))-Long.valueOf(record.get("oldLines"));
			GitResource res = new GitResource(fname, commit, timestamp, changedLines);
			if ("ADD".equals(changeType)) {
				addedFiles.put(res.fname, res);
			}
			resourcesByCommit.putIfAbsent(commit, new ArrayList());
			resourcesByCommit.get(commit).add(res);
			return null;
		}

		public long getFilesByCommit(String commit, boolean isTest) {
			return getResourcesByCommit(commit, isTest).count();
		}

		public long getChangedLinesByCommit(String commit, boolean isTest) {
			return getResourcesByCommit(commit, isTest).mapToLong(x->x.changedLines).sum();
		}
		private Stream<GitResource> getResourcesByCommit(String commit, boolean isTest) {
			return resourcesByCommit.get(commit).stream().filter(p->p.isTest == isTest);
		}

		Map<String,Set<String>> refactoredFilesByCommit = new HashMap<>();
		public Object processRefMiner(CSVRecord xrecord) {
			Map<String, String> record = xrecord.toMap();
			String commit = record.get("commit");
			refactoredFilesByCommit.putIfAbsent(commit, new HashSet());
			refactoredFilesByCommit.get(commit).add(record.get("filePath"));
			return null;
		}

		public long getRefactoredFilesCount(String basename, String commit, boolean isTest) {
			Set<String> refactoredFiles = refactoredFilesByCommit.getOrDefault(commit, new HashSet());
			List<GitResource> files = resourcesByCommit.getOrDefault(commit, new ArrayList<GitResource>());
			long count = files.stream().filter(p->refactoredFiles.contains(p.fname) && p.isTest == isTest).count();
			return count;
		}

	}

	public static void main(String[] args) {
		String dir = "/Users/preich/Documents/github/cayenne";
		String fname = dir + "/git-history.csv";

		LOG.info("Loading " + fname);
		try {
			CSVParser ghParser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
					CSVFormat.newFormat(';').withFirstRecordAsHeader());
			CSVParser rfParser = CSVParser.parse(new File(dir+"/refminer-coderange.csv"), Charset.defaultCharset(),
					CSVFormat.newFormat(';').withFirstRecordAsHeader());
			Session ses = new Session();
			ghParser.forEach(x -> ses.processGitHistory(x));
			rfParser.forEach(x -> ses.processRefMiner(x));
			Map<String, GitResource> testMap = ses.addedFiles.values().stream().filter(p -> p.isTest)
					.collect(Collectors.toMap(x -> x.basename, x -> x, (v1, v2) -> v1));
			Map<String, GitResource> prodMap = ses.addedFiles.values().stream().filter(p -> !p.isTest)
					.collect(Collectors.toMap(x -> x.basename, x -> x, (v1, v2) -> v1));

			LOG.info("testMap: " + testMap.size() + ", prodMap: "+ prodMap.size());
			Set<String> baseNames = Stream.concat(prodMap.entrySet().stream(), testMap.entrySet().stream())
					.filter(e -> testMap.containsKey(e.getKey()) && prodMap.containsKey(e.getKey()))
					.map(e -> e.getKey()).collect(Collectors.toSet());
			Map<String, ProdTestPair> prodTestPairs = baseNames.stream()
					.collect(Collectors.toMap(e -> e, e -> new ProdTestPair(prodMap.get(e), testMap.get(e))));
			LOG.info("prodTestPairs: " + prodTestPairs.size());

			Map<String, String> firstTestCommits = prodTestPairs.entrySet().stream()
					.filter(e -> e.getValue().test.timestamp > e.getValue().prod.timestamp)
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().test.commit));
			LOG.info("firstTestCommits: " + firstTestCommits.size());
			DataFrame df = firstTestCommits.entrySet().stream()
			.map(e -> new DataFrame("0")
					.addColumn("testFileName", e.getKey())
					.addColumn("testFilesCount", ses.getFilesByCommit(e.getValue(), true))
					.addColumn("prodFilesCount", ses.getFilesByCommit(e.getValue(), false))
					.addColumn("testFilesLines", ses.getChangedLinesByCommit(e.getValue(), true))
					.addColumn("prodFilesLines", ses.getChangedLinesByCommit(e.getValue(), false))
					.addColumn("refactoredProdFilesCount", ses.getRefactoredFilesCount(e.getKey(), e.getValue(), false))
					)
			.collect(DataFrame.combine());
			df.toCSV("df.csv");
			LOG.info("Saved");
			LOG.info("refactored: " + df.nameToValue.get("refactoredProdFilesCount").stream().filter(p->Long.valueOf(String.valueOf(p)) > 0).count());
//			 Git git = Git.open(new File(dir));
//			 LOG.info("result: " + git.checkout().setName(commit).call());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
}
