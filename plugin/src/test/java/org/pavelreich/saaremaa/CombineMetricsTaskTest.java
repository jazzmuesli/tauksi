package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.pavelreich.saaremaa.combiner.CombineMetricsTask;
import org.pavelreich.saaremaa.combiner.ProjectDirs;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombineMetricsTaskTest {

	@Test
	public void test() {
		MongoDBClient db = Mockito.mock(MongoDBClient.class);
		Logger logger = LoggerFactory.getLogger(getClass());
		URL resource = getClass().getResource("/metrics-folder/metrics-folder.txt");
		File basedir = new File(resource.getFile()).getParentFile();//TODO: fix it
		basedir = new File("src/test/resources/metrics-folder/");
		String targetDirectory = basedir.toPath().resolve("target").toFile().getAbsolutePath();
		List<String> testSrcDirs = Collections.singletonList(basedir.toPath().resolve("src/test/java").toFile().getAbsolutePath());
		String usePomDirectories = "true";
		List<String> srcDirs= Collections.singletonList(basedir.toPath().resolve("src/main/java").toFile().getAbsolutePath());
		String projectId = "metrics-project";
		ProjectDirs projDirs = new ProjectDirs(basedir, targetDirectory, srcDirs, testSrcDirs, 
				Collections.singleton(targetDirectory + "/classes"), 
				Collections.singleton(targetDirectory+"/test-classes"));
		CombineMetricsTask task = new CombineMetricsTask(db, 
				logger, projDirs, projectId, 
				usePomDirectories);
		task.execute();
		ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
		Mockito.verify(db).insertCollection(Mockito.eq("combinedMetrics"), captor.capture());
		assertEquals(23, captor.getValue().size());
		Map<String, Document> docMap = captor.getValue().stream().collect(Collectors.toMap(k->k.getString("prodClassName"), v->v));
		Document doc = docMap.get("org.pavelreich.saaremaa.Helper");
		Set<String> keys = doc.keySet();
		keys.forEach(kl -> logger.info("key:" + kl));
		assertTrue(keys.contains("RFC"));
		assertTrue(keys.contains("LOC"));
		assertTrue(keys.contains("T_LOC"));
		assertEquals(Long.valueOf(155), doc.getLong("T_LOC"));
		assertEquals(Long.valueOf(41), doc.getLong("loc_test"));
		assertEquals(Long.valueOf(390), doc.getLong("LOC"));
		assertEquals(Long.valueOf(58), doc.getLong("loc_prod"));
	}
}
