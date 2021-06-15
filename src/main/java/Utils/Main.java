package Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.TreeMap;

import smile.math.matrix.Matrix;

public class Main {

	public static Matrix scale(Matrix matrix, int a, int b) {
		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		for (int i = 0; i < matrix.nrows(); i++) {
			for (int j = 0; j < matrix.ncols(); j++) {
				min = Math.min(min, matrix.get(i, j));
				max = Math.max(max, matrix.get(i, j));
			}
		}
		for (int i = 0; i < matrix.nrows(); i++) {
			for (int j = 0; j < matrix.ncols(); j++) {
				double res = ((b - a) * (matrix.get(i, j) - min)) / (max - min) + a;
				matrix.update(i, j, res);
			}
		}
		return matrix;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		String ant = "/home/shiva-prasad-reddy/Desktop/3/Tool1";
		String javaKeywordsFile = "/home/shiva-prasad-reddy/Desktop/3/JavaKeywords.txt";

		Matrix references, calls, semantic;

		ImportProject project = new ImportProject(ant);
		Structural _structural = new Structural(project);
		references = _structural.getReferences();
		calls = _structural.getCalls();

		Semantic semanticDependencies = new Semantic(project.getIndex());
		semanticDependencies.parseDocs(project.getAbstractSyntaxTrees(), javaKeywordsFile);
		semanticDependencies.buildDocsMatrix();
		semanticDependencies.computeInverseDocFreq();
		semanticDependencies.computeTermFreq();
		semanticDependencies.computeTF_IDF();
		semantic = new Matrix(project.getAbstractSyntaxTrees().size(), project.getAbstractSyntaxTrees().size());
		semanticDependencies.computeSimilarityMatrix(semantic);

		references.replaceNaN(0.0);
		calls.replaceNaN(0.0);
		semantic.replaceNaN(0.0);

		TreeMap<Integer, String> indexM = project.getIndexM();

		int N = project.getAbstractSyntaxTrees().size();

		Matrix structural = new Matrix(N, N);
		structural.add(references);
		structural.add(calls);
		scale(structural, 0, 1);

		// Functionality Driven Ratio (FDR)
		Matrix avg = new Matrix(N, N);
		avg.add(structural);
		avg.add(semantic);
		avg.div(2);
		double fdr[] = avg.colSums();

		// Cardinality Score (CS)
		structural = new Matrix(N, N);
		structural.add(references);
		structural.add(calls);
		double t[] = structural.colSums();
		double cs[] = new double[N];
		for (int i = 0; i < N; i++)
			cs[i] = 1 - 1 / (1 + t[i]);

		// Utility Traces
		double alpha = 0.85;
		double beta = 0.85;

		HashSet<String> utilityTraces = new HashSet<>();
		for (int i = 0; i < N; i++) {
			if (fdr[i] > alpha && cs[i] > beta)
				utilityTraces.add(indexM.get(i));
		}

		System.out.println("Total Classes : " + N);
		project.getAbstractSyntaxTrees().keySet().forEach(classname -> System.out.println("\t" + classname));

		System.out.println();

		System.out.println("Total Utility Traces : " + utilityTraces.size());
		utilityTraces.forEach(classname -> System.out.println("\t" + classname));
	}

}
