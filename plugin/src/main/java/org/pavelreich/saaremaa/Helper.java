package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.maven.project.MavenProject;

public class Helper {

	public static boolean isRootProject(MavenProject project) {
		MavenProject parent = project.getParent();
		return parent == null || parent.getFile() == null;
	}
	public static String convertListToString(List<String> ls) {
		if (ls.size() < 5) {
			return ls.stream().collect(Collectors.joining(", "));
		} else {
			String head = ls.subList(0, 3).stream().collect(Collectors.joining(", "));
			return head + ", " + (ls.size() - 4 + " more, ") + ls.get(ls.size() - 1);
		}
	}
	
	static String getProdClassName(String testClassName) {
		String prodClassName = testClassName
				.replaceAll("_ESTest$", "")
				.replaceAll("UnitTests$", "")
				.replaceAll("UnitTest$", "")
				.replaceAll("Test$", "")
				.replaceAll("Tests$", "")
				.replaceAll("\\.Test", ".")
				.replaceAll("TestCase$", "");
		return prodClassName;
	}

	static String classifyTest(String tcn) {
		if (tcn.contains("ESTest")) {
			return "evo";
		} else if (isTest(tcn)) {
			return "test";
		}
		return null;
	}
	
	static boolean isTest(String tcn) {
		return tcn.contains(".Test") || 
				tcn.endsWith("Test") || 
				tcn.endsWith("UnitTests") ||
				tcn.endsWith("UnitTest") ||
				tcn.endsWith("TestCase") || 
				tcn.endsWith("Tests") ;
	}

  
	static CSVParser getParser(String fname, String field) throws IOException {
		CSVParser parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
				CSVFormat.DEFAULT.withFirstRecordAsHeader());
		if (!parser.getHeaderMap().containsKey(field)) {
			parser = CSVParser.parse(new File(fname), Charset.defaultCharset(),
					CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader());
		}
		return parser;
	}
	
}
