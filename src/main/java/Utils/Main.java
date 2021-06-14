package Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.TreeMap;

import smile.math.matrix.Matrix;

public class Main {

	public static void serialize(Matrix matrix, String filename) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(filename);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		objectOutputStream.writeObject(matrix);
		objectOutputStream.close();
		fileOutputStream.close();
	}

	public static Matrix deserialize(String filename) throws ClassNotFoundException, IOException {
		FileInputStream fileInputStream = new FileInputStream(filename);
		ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		Object object = objectInputStream.readObject();
		objectInputStream.close();
		fileInputStream.close();
		return (Matrix) object;
	}

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

		String ant = "/home/shiva-prasad-reddy/Desktop/3/apache-ant-1.9.15";
		String javaKeywordsFile = "/home/shiva-prasad-reddy/Desktop/3/JavaKeywords.txt";

		String referencesFN = "/home/shiva-prasad-reddy/Desktop/3/references";
		String callsFN = "/home/shiva-prasad-reddy/Desktop/3/calls";
		String semanticFN = "/home/shiva-prasad-reddy/Desktop/3/semantic";

		Matrix references, calls, semantic;

		try {
			references = deserialize(referencesFN);
			calls = deserialize(callsFN);
			semantic = deserialize(semanticFN);
		} catch (FileNotFoundException exception) {
			ImportProject project = new ImportProject(ant);
			Structural structural = new Structural(project);
			references = structural.getReferences();
			serialize(references, referencesFN);
			calls = structural.getCalls();
			serialize(calls, callsFN);

			Semantic semanticDependencies = new Semantic(project.getIndex());
			semanticDependencies.parseDocs(project.getAbstractSyntaxTrees(), javaKeywordsFile);
			semanticDependencies.buildDocsMatrix();
			semanticDependencies.computeInverseDocFreq();
			semanticDependencies.computeTermFreq();
			semanticDependencies.computeTF_IDF();
			semantic = new Matrix(project.getAbstractSyntaxTrees().size(), project.getAbstractSyntaxTrees().size());
			semanticDependencies.computeSimilarityMatrix(semantic);
			serialize(semantic, semanticFN);
		}

		references.replaceNaN(0.0);
		calls.replaceNaN(0.0);
		semantic.replaceNaN(0.0);

		ImportProject project = new ImportProject(ant);
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

		for (int i = 0; i < N; i++)
			System.out.print(fdr[i] + ",");
		System.out.println();

		for (int i = 0; i < N; i++)
			System.out.print(cs[i] + ",");
		System.out.println();

		// Utility Traces
		double alpha = 0.95;
		double beta = 0.95;

		HashSet<String> utilityTraces = new HashSet<>();
		for (int i = 0; i < N; i++) {
			if (fdr[i] > alpha && cs[i] > beta)
				utilityTraces.add(indexM.get(i));
		}
		
		System.out.println("Total Classes : " + N);
		System.out.println("Total Utility Traces : " + utilityTraces.size());
//		utilityTraces.forEach(classname -> System.out.println(classname));
	}

}
