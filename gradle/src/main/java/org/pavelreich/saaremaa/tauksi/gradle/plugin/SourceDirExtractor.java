package org.pavelreich.saaremaa.tauksi.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.tuple.Pair;
import org.pavelreich.saaremaa.Helper;


public class SourceDirExtractor {

	public static Pair<List<String>, List<String>> extract(List<String> sourceDirFiles) throws IOException {
		List<String> srcDirs = new ArrayList<String>();
		List<String> testSrcDirs = new ArrayList<String>();
		for (String f : sourceDirFiles) {
			System.out.println("reading " + f);
			CSVParser parser = Helper.getParser(new File(f), "processed");
			parser.getRecords().stream().filter(p -> p.get("processed").equals("true")).forEach(entry -> {
				if (entry.get("sourceSetName").contains("test")) {
					testSrcDirs.add(entry.get("dirName"));
				} else {
					srcDirs.add(entry.get("dirName"));
				}
			});
		}
		srcDirs.forEach(x -> System.out.println("src: " + x));
		testSrcDirs.forEach(x -> System.out.println("testSrc: " + x));

		return Pair.of(srcDirs, testSrcDirs);
	}
}
