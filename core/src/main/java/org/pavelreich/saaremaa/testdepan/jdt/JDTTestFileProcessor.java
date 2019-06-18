package org.pavelreich.saaremaa.testdepan.jdt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.pavelreich.saaremaa.testdepan.ITestClass;

public class JDTTestFileProcessor {

	private static String readFile(String fname) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(fname));

		String s = lines.stream().collect(Collectors.joining("\n"));
		return s;
	}

	public static List<ITestClass> analyse(String fname) throws Exception {
		ASTParser parser = ASTParser.newParser(AST.JLS10);
		String s = readFile(fname);
		parser.setSource(s.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		final TestClass tc = new TestClass();
		cu.accept(new TestFileVisitor(tc));
		return Arrays.asList(tc);
	}
}
