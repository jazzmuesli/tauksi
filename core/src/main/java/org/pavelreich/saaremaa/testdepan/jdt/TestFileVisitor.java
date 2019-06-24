package org.pavelreich.saaremaa.testdepan.jdt;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mauricioaniche.ck.util.JDTUtils;

/**
 * Visit Test class and populate the value object accordingly
 * 
 * @author preich
 *
 */
public class TestFileVisitor extends ASTVisitor {
	protected static final Logger LOG = LoggerFactory.getLogger(TestFileVisitor.class);
	private final TestClass tc;
	private CompilationUnit cu;

	public TestFileVisitor(CompilationUnit cu, TestClass tc) {
		this.tc = tc;
		this.cu = cu;
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

	@Override
	public boolean visit(FieldDeclaration node) {
		Collection<String> annotations = getAnnotations(node);
		if (annotations.contains("Mock")) {
			tc.mockFields.add(new TestField(node.getType().toString()));
		}
		return super.visit(node);

	}

	private Map<Object, String> resolvedNodes = new HashMap();
	/**
	 * @Test annotation found
	 * 
	 */
	private String resolveNode(Object x) {

		AST key = ((ASTNode)x).getAST();
		if (x instanceof NumberLiteral) {
			return "int";
		}
		return resolvedNodes.getOrDefault(x, "unknown");
	}
	@Override
	public boolean visit(MethodDeclaration node) {
		Collection<String> annotations = getAnnotations(node);
		boolean present = annotations.contains("Test");
		String fullName = JDTUtils.getMethodFullName(node);

		if (present) {
		
			LOG.info("returnType: " + node.getReturnType2());
			TestMethod testMethod = new TestMethod(node.getName().getIdentifier());
			testMethod.annotations = annotations;
			node.accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodInvocation node) {
					ITypeBinding type = node.resolveTypeBinding();

					IMethodBinding method = node.resolveMethodBinding();
					Object st = type != null ? type.getQualifiedName() : "unk";
					if (type != null) {
						resolvedNodes.put(node, type.getQualifiedName());
					}
					ITypeBinding dc = method == null ? null : method.getDeclaringClass();
					LOG.info("body: " + node + ", type: " +st + ", method: " + dc);
					return true;
				}
			});
			node.accept(new ASTVisitor() {

				@Override
				public boolean visit(MethodInvocation node) {
					if (node.getName().getIdentifier().contains("assert")) {
						List<String> args = (List<String>) node.arguments().stream().map(x -> resolveNode(x)).collect(Collectors.toList());
						LOG.info("node: " + node + ", args: " +args);
						TestAssert testAssert = new TestAssert(node.getName().getIdentifier());

						int line = node.getStartPosition();
						testAssert.line = cu.getLineNumber(line);
						testAssert.argTypes.addAll(args);
						testMethod.assertions.add(testAssert);
						

					}
					if (node.getName().getIdentifier().contains("mock")) {
						LOG.info("mock.method=" + node);
						Object mockVar = node.arguments().get(0);
//						testMethod.mocks.add(mockVar);TODO
					}
					return super.visit(node);
				}
			});
			tc.testMethods.add(testMethod);
		}
		return present;
	}



	private Collection<String> getAnnotations(BodyDeclaration node) {
		Stream<String> map = node.modifiers().stream().filter(p -> p instanceof MarkerAnnotation).map(x -> ((MarkerAnnotation)x).getTypeName().getFullyQualifiedName());
		Collection<String> annotations =  map.collect(Collectors.toList());
		return annotations;
	}
}
