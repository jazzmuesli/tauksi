package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
	public static List<String> findFiles(String dir, Predicate<File> predicate) throws IOException {
		Path path = new File(dir).toPath();
		List<String> files = java.nio.file.Files.walk(path).filter(p -> predicate.test(p.toFile()))
				.map(f -> f.toFile().getAbsolutePath()).collect(Collectors.toList());
		return files;
	}
	public static String getProdClassName(String testClassName) {
		String prodClassName = testClassName
				.replaceAll("_ESTest$", "")
				.replaceAll("EvoSuiteTest$", "")
				.replaceAll("UnitTests$", "")
				.replaceAll("UnitTest$", "")
				.replaceAll("Test$", "")
				.replaceAll("Tests$", "")
				.replaceAll("\\.Test", ".")
				.replaceAll("TestCase$", "");
		return prodClassName;
	}

	public static String classifyTest(String tcn) {
		if (tcn.contains("ESTest") || tcn.contains("EvoSuiteTest")) {
			return "evo";
		} else if (isTest(tcn)) {
			return "test";
		}
		return null;
	}
	
	public static boolean isTest(String tcn) {
		return tcn.contains(".Test") || 
				tcn.endsWith("Test") || 
				tcn.endsWith("UnitTests") ||
				tcn.endsWith("UnitTest") ||
				tcn.endsWith("TestCase") || 
				tcn.endsWith("Tests") ;
	}

	public static CSVParser getParser(String fname, String field) throws IOException {
		return getParser(new File(fname), field);
	}
	
	public static CSVParser getParser(File file, String field) throws IOException {
		CSVParser parser = CSVParser.parse(file, Charset.defaultCharset(),
				CSVFormat.DEFAULT.withFirstRecordAsHeader());
		if (!parser.getHeaderMap().containsKey(field)) {
			parser = CSVParser.parse(file, Charset.defaultCharset(),
					CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader());
		}
		return parser;
	}

	protected static Collection<String> calculateDataMethods(
			final Set<String> fields, 
			final List<String> methods,
			final List<Optional<String>> setters, 
			final List<Optional<String>> getters) {
		Collection<String> dataMethods = IntStream.range(0, methods.size()).mapToObj(i -> {
			String method = methods.get(i);
			boolean isSetter = setters.get(i).isPresent() && fields.contains(setters.get(i).get());
			boolean isGetter = getters.get(i).isPresent() && fields.contains(getters.get(i).get());
			boolean isSpecialMethod = method != null && Arrays.asList("hashCode","equals","toString").contains(getSimpleMethodName(method));
			return (isSetter || isGetter || isSpecialMethod) ? method : null;
		}).filter(p -> p != null).collect(Collectors.toSet());
		return dataMethods;
	}

	public static String getSimpleMethodName(String method) {
		String methodName = method.replaceAll("(.*?)\\/.*", "$1");
		return methodName;
	}

	protected static List<Optional<String>> getMethods(List<String> methods, String startPrefix, String prefix) {
		List<Optional<String>> setters = methods.stream()
				.map(p -> p.matches(startPrefix)
						? extractName(prefix, p)
						: Optional.<String>empty())
				.collect(Collectors.toList());
		return setters;
	}
	static Optional<String> extractName(String prefix, String p) {
		return Optional.of(p.toLowerCase().replaceAll("^" + prefix + "(.*?)\\/.*", "$2").toLowerCase());
	}
}
