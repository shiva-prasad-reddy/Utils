package Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

public class ImportProject {

	private String projectPath;

	private TreeMap<String, ClassOrInterfaceDeclaration> abstractSyntaxTrees; /* Abstract Syntax Trees */
	private TreeMap<String, Integer> index;
	private TreeMap<Integer, String> indexM;

	public ImportProject(String projectPath) throws IOException {
		this.projectPath = projectPath;

		abstractSyntaxTrees = new TreeMap<String, ClassOrInterfaceDeclaration>();

		parseProjectFile();

		int count = 0;
		index = new TreeMap<String, Integer>();
		indexM = new TreeMap<>();
		for (String classname : abstractSyntaxTrees.keySet()) {
			index.put(classname, count);
			indexM.put(count, classname);
			count++;
		}

	}

	public TreeMap<String, ClassOrInterfaceDeclaration> getAbstractSyntaxTrees() {
		return abstractSyntaxTrees;
	}

	public TreeMap<String, Integer> getIndex() {
		return index;
	}

	public TreeMap<Integer, String> getIndexM() {
		return indexM;
	}

	private void parseProjectFile() throws IOException {
		Path root = Paths.get(projectPath);
		ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(root);
		List<SourceRoot> sourceRoots = projectRoot.getSourceRoots();
		for (SourceRoot sourceRoot : sourceRoots) {
			List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
			for (ParseResult<CompilationUnit> parseResult : parseResults) {
				if (parseResult.isSuccessful()) {
					CompilationUnit compilationUnit = parseResult.getResult().get();
					List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = compilationUnit
							.findAll(ClassOrInterfaceDeclaration.class);
					for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations) {
						Stack<String> stack = new Stack<String>();
						// To get the proper naming of inner and nested classes
						this.traverseBack(classOrInterfaceDeclaration, stack);
						String key = "";
						while (true) {
							key += stack.pop();
							if (!stack.empty())
								key += ".";
							else
								break;
						}

//						System.out.println("  [**]  " + key);
						abstractSyntaxTrees.put(key, classOrInterfaceDeclaration);

					}
				}
			}
		}
	}

	/*
	 * To find fully qualified class name in a Abstract Syntax Tree
	 */
	private void traverseBack(Node node, Stack<String> stack) {
		if (node instanceof ClassOrInterfaceDeclaration)
			stack.push(((ClassOrInterfaceDeclaration) node).getNameAsString());
		if (node instanceof CompilationUnit) {
			CompilationUnit compilationUnit = (CompilationUnit) node;
			if (compilationUnit.getPackageDeclaration().isPresent())
				stack.push(compilationUnit.getPackageDeclaration().get().getNameAsString());
			return;
		}
		node = node.getParentNode().get();
		traverseBack(node, stack);
	}

}
