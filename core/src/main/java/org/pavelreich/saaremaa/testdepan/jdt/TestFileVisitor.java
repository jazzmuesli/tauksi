package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Visit Test class and populate the value object accordingly
 * 
 * @author preich
 *
 */
public class TestFileVisitor extends ASTVisitor {
	private final TestClass tc;

	public TestFileVisitor(TestClass tc) {
		this.tc = tc;
	}

	/**
	 * package declaration found
	 */
	@Override
	public boolean visit(PackageDeclaration node) {
		String fullyQualifiedName = node.getName().getFullyQualifiedName();
		tc.setPackageName(fullyQualifiedName);
		return super.visit(node);
	}

	/**
	 * class declaration found
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		String fullyQualifiedName = node.getName().getFullyQualifiedName();
		tc.setClassName(fullyQualifiedName);
		return super.visit(node);
	}

	/**
	 * @Test annotation found
	 * 
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		Optional x = node.modifiers().stream().filter(p -> p instanceof MarkerAnnotation
				&& ((MarkerAnnotation) p).getTypeName().getFullyQualifiedName().contains("Test")).findFirst();
		if (x.isPresent()) {
			new TestMethod(node.getName().getIdentifier());
		}
		return x.isPresent();
	}
}
