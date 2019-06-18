package org.pavelreich.saaremaa.testdepan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class JDTTestFileProcessor {

	public void x() throws IOException {

		ASTParser parser = ASTParser.newParser(AST.JLS10);
		String s = readFile();
		parser.setSource(s.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		cu.accept(new ASTVisitor() {

			Set names = new HashSet();

			public boolean visit(VariableDeclarationFragment node) {
				SimpleName name = node.getName();
				this.names.add(name.getIdentifier());
				System.out.println("Declaration of '" + name + "' at line" + cu.getLineNumber(name.getStartPosition()));
				return false; // do not continue to avoid usage info
			}

			public boolean visit(SimpleName node) {
				if (this.names.contains(node.getIdentifier())) {
					System.out.println("Usage of '" + node + "' at line " + cu.getLineNumber(node.getStartPosition()));
				}
				return true;
			}

		});
	}

	private String readFile() throws IOException {
		List<String> lines = Files
				.readAllLines(Paths.get("src/main/java/org/pavelreich/saaremaa/jdt/JDTUsageExample.java"));

		String s = lines.stream().collect(Collectors.joining("\n"));
		return s;
	}
}
