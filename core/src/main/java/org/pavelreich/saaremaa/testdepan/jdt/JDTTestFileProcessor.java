package org.pavelreich.saaremaa.testdepan.jdt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.pavelreich.saaremaa.testdepan.ITestClass;

public class JDTTestFileProcessor {

	private static String readFile(String fname) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(fname));

		String s = lines.stream().collect(Collectors.joining("\n"));
		return s;
	}

	public static List<ITestClass> analyse(String fname) throws Exception {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		String s = readFile(fname);
		parser.setSource(s.toCharArray());
		parser.setUnitName(fname);

		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
		
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
		String[] encodings = new String[] {"UTF-8"};
		parser.setEnvironment(classpath, null, null, true);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		

		final TestClass tc = new TestClass();
		cu.accept(new TestFileVisitor(cu,tc));
		
		if (cu.getAST().hasBindingsRecovery()) {
			System.out.println("Binding activated.");
		}
		
		cu.accept(new TypeFinderVisitor());
		return Arrays.asList(tc);
	}
	
	static class TypeFinderVisitor extends ASTVisitor{
		 
		public boolean visit(VariableDeclarationStatement node){
			for (Iterator iter = node.fragments().iterator(); iter.hasNext();) {
				System.out.println("------------------");
	 
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				IVariableBinding binding = fragment.resolveBinding();
	 
				System.out.println("binding variable declaration: " +binding.getVariableDeclaration());
				System.out.println("binding: " +binding);
			}
			return true;
		}
	}
}
