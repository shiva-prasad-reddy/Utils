package Utils;

import java.util.Set;
import java.util.TreeMap;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import smile.math.matrix.Matrix;

public class Structural {

	private TreeMap<String, ClassOrInterfaceDeclaration> abstractSyntaxTrees;
	private Set<String> classes;
	private TreeMap<String, Integer> index;

	private Matrix R3, R6, R7;

	public Structural(ImportProject project) {
		abstractSyntaxTrees = project.getAbstractSyntaxTrees();
		classes = abstractSyntaxTrees.keySet();

		index = project.getIndex();

		int N = index.size();

		R3 = new Matrix(N, N);
		R6 = new Matrix(N, N);
		R7 = new Matrix(N, N);
		
		this.parse();
	}

	public void parse() {
		for (String classname : abstractSyntaxTrees.keySet()) {
			parse(classname, abstractSyntaxTrees.get(classname));
		}
	}

	private void error(String classname, String msg) {
		if (true)
			System.err.println(classname + " >> " + msg);
	}

	private void parse(String classname, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

		int row = index.get(classname);

		// ~~~~~ Calls R3 ~~~~~
		classOrInterfaceDeclaration.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
			try {
				ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();
				String fqname = resolvedMethodDeclaration.getPackageName() + "."
						+ resolvedMethodDeclaration.getClassName();
				if (classes.contains(fqname)) {
					int column = index.get(fqname);
					if (row != column) {
						R3.add(row, column, 1);
					}
				}
			} catch (Exception exception) {
				error(classname, exception.getMessage());
			}
		});

		// ~~~~~ IsOfType|Contains R6 ~~~~~
		classOrInterfaceDeclaration.getFields().forEach(fieldDeclaration -> {
			try {
				ResolvedType resolvedType = fieldDeclaration.getElementType().resolve();
				parseResolvedType(resolvedType, row, R6);
			} catch (Exception exception) {
				error(classname, exception.getMessage());
			}
		});
		classOrInterfaceDeclaration.findAll(VariableDeclarationExpr.class).forEach(variableDeclarationExpr -> {
			try {
				ResolvedType resolvedType = variableDeclarationExpr.calculateResolvedType();
				parseResolvedType(resolvedType, row, R6);
			} catch (Exception exception) {
				error(classname, exception.getMessage());
			}
		});

		// ~~~~~ References R7 ~~~~~
		classOrInterfaceDeclaration.findAll(FieldAccessExpr.class).forEach(fieldAccessExpr -> {
			try {
				ResolvedType resolvedType = fieldAccessExpr.calculateResolvedType();
				parseResolvedType(resolvedType, row, R7);
			} catch (Exception exception) {
				error(classname, exception.getMessage());
			}
		});

	}

	private void parseResolvedType(ResolvedType resolvedType, int row, Matrix R) {
		if (resolvedType.isArray()) {
			ResolvedType resolvedType2 = getResolvedTypeFromArrayType(resolvedType.asArrayType());
			if (resolvedType2.isReferenceType()) {
				String fqname = resolvedType2.asReferenceType().getTypeDeclaration().getQualifiedName();
				if (classes.contains(fqname)) {
					int column = index.get(fqname);
					if (row != column) {
						R.add(row, column, 1);
					}
				}
			}
		}
		if (resolvedType.isReferenceType()) {
			ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
			String fqname1 = resolvedReferenceType.getQualifiedName();
			if (classes.contains(fqname1)) {
				int column = index.get(fqname1);
				if (row != column) {
					R.add(row, column, 1);
				}
			}

			// To get the type information from fields like List<Type>
			resolvedReferenceType.getTypeParametersMap().forEach(pair -> {
				if (pair.b.isReferenceType()) {
					ResolvedReferenceType tempResolvedReferenceType = pair.b.asReferenceType();
					String fqname2 = tempResolvedReferenceType.getQualifiedName();
					if (classes.contains(fqname2)) {
						int column = index.get(fqname2);
						if (row != column) {
							R.add(row, column, 1);
						}
					}
				}
			});
		}
	}

	private ResolvedType getResolvedTypeFromArrayType(ResolvedArrayType resolvedArrayType) {
		ResolvedType resolvedType = resolvedArrayType.getComponentType();
		if (resolvedType.isArray())
			return getResolvedTypeFromArrayType(resolvedType.asArrayType());
		return resolvedType;
	}

	public Matrix getCalls() {
		return this.R3;
	}

	public Matrix getReferences() {
		return this.R7;
	}

}
