package Utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.SimpleName;

import smile.math.matrix.Matrix;
import smile.nlp.dictionary.EnglishStopWords;
import smile.nlp.dictionary.SimpleDictionary;
import smile.nlp.normalizer.SimpleNormalizer;
import smile.nlp.stemmer.PorterStemmer;
import smile.nlp.tokenizer.SimpleTokenizer;

class SemanticParser {

	public static String breakCamelCase(String str) {
		String regex = "([a-z])([A-Z]+)";
		String replacement = "$1 $2";
		str = str.replaceAll(regex, replacement);
		return str;
	}

	public static String extractDocText(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
		StringBuilder body = new StringBuilder();
		classOrInterfaceDeclaration.getAllContainedComments().forEach(comment -> {
			body.append(comment.getContent() + " ");
		});
		classOrInterfaceDeclaration.findAll(SimpleName.class).forEach(simpleName -> {
			body.append(simpleName.toString() + " ");
		});
		return body.toString();
	}

	/*
	 * Later to be added 1. Remove N length tokens 2. Camel Case to normal 3. Remove
	 * and number text 4. Convert to lower case 5. Remove all non-alphanumeric
	 * characters
	 */

	public static ArrayList<String> camelCase2Words(ArrayList<String> tokens) {
		ArrayList<String> newTokens = new ArrayList<>();
		for (String token : tokens) {
			token = breakCamelCase(token);
			for (String word : token.split(" "))
				newTokens.add(word);
		}
		return newTokens;
	}

	public static ArrayList<String> purgeTokens(ArrayList<String> tokens) {
		final int DEFAULT_MIN_LEN = 3;
		ArrayList<String> newTokens = new ArrayList<>();
		for (String token : tokens)
			if (token.length() > DEFAULT_MIN_LEN)
				newTokens.add(token.replaceAll("[^a-zA-Z0-9]", "").toLowerCase());
		return newTokens;
	}

	public static ArrayList<String> removeEnglishStopWords(ArrayList<String> tokens) {
		ArrayList<String> newTokens = new ArrayList<>();
		for (String token : tokens)
			if (!EnglishStopWords.DEFAULT.contains(token))
				newTokens.add(token);
		return newTokens;
	}

	public static ArrayList<String> removeJavaKeywords(ArrayList<String> tokens, SimpleDictionary javaKeywords) {
		ArrayList<String> newTokens = new ArrayList<>();
		for (String token : tokens)
			if (!javaKeywords.contains(token))
				newTokens.add(token);
		return newTokens;
	}

	public static ArrayList<String> stem(ArrayList<String> tokens, PorterStemmer stemmer) {
		ArrayList<String> newTokens = new ArrayList<>();
		for (String token : tokens)
			newTokens.add(stemmer.stem(token));
		return newTokens;
	}
}

class WordCounter extends Hashtable<String, Integer> {
	private static final long serialVersionUID = 1L; // Serialization & Deserialization

	public void put(String fqname) {
		if (this.containsKey(fqname)) {
			int value = this.get(fqname);
			this.replace(fqname, value + 1);
		} else {
			this.put(fqname, 1);
		}
	}
}

public class Semantic {

	Hashtable<String, Integer> indexW;
	TreeMap<String, Integer> indexC;
	Hashtable<String, WordCounter> docs;

	double[][] docsMatrix;
	int countOfWordsInDoc[];
	double[] idf;

	int noOfWords;
	int noOfDocs;

	public Semantic(TreeMap<String, Integer> classIndex) {
		indexC = classIndex;
		indexW = new Hashtable<String, Integer>();
		docs = new Hashtable<String, WordCounter>();
	}

	public void parseDocs(TreeMap<String, ClassOrInterfaceDeclaration> javaSrcAST, String javaKeywordsFile) {

		TreeSet<String> uniqueWords = new TreeSet<>(); // for a sorted order of words

		SimpleNormalizer normalizer = SimpleNormalizer.getInstance();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		SimpleDictionary javaKeywords = new SimpleDictionary(javaKeywordsFile);
		PorterStemmer stemmer = new PorterStemmer();

		for (String classname : javaSrcAST.keySet()) {

			WordCounter wordCounter = new WordCounter();

			String docText = SemanticParser.extractDocText(javaSrcAST.get(classname));
			docText = normalizer.normalize(docText);
			String[] tokensArray = tokenizer.split(docText);

			ArrayList<String> tokens = new ArrayList<String>();
			for (String token : tokensArray)
				tokens.add(token);

			tokens = SemanticParser.camelCase2Words(tokens);
			tokens = SemanticParser.purgeTokens(tokens);
			tokens = SemanticParser.removeEnglishStopWords(tokens);
			tokens = SemanticParser.removeJavaKeywords(tokens, javaKeywords);
			tokens = SemanticParser.stem(tokens, stemmer);

			for (String token : tokens) {
				wordCounter.put(token);
				uniqueWords.add(token);
			}

			docs.put(classname, wordCounter);
		}

		int index = 0;
		for (String word : uniqueWords) {
			indexW.put(word, index);
			index++;
		}
	}

	public void buildDocsMatrix() {
		int rows = noOfDocs = indexC.size();
		int columns = noOfWords = indexW.size();

		docsMatrix = new double[rows][columns];
		countOfWordsInDoc = new int[rows];

		for (String classname : docs.keySet()) {
			int row = indexC.get(classname);
			WordCounter wordsInDoc = docs.get(classname);
			int count = 0;
			for (String word : wordsInDoc.keySet()) {
				int column = indexW.get(word);
				if (row != column) {
					int value = wordsInDoc.get(word);
					docsMatrix[row][column] = value;
					count += value;
				}
			}
			countOfWordsInDoc[row] = count;
		}
	}

	public void computeInverseDocFreq() {
		idf = new double[noOfWords];
		for (int i = 0; i < noOfDocs; i++) {
			for (int j = 0; j < noOfWords; j++) {
				if (docsMatrix[i][j] != 0)
					idf[j] += 1;
			}
		}
		for (int j = 0; j < noOfWords; j++)
			idf[j] = Math.log(noOfDocs / (idf[j] + 1));
	}

	public void computeTermFreq() {
		int rows = noOfDocs;
		int columns = noOfWords;
		for (int i = 0; i < rows; i++) {
			int val = countOfWordsInDoc[i];
			for (int j = 0; j < columns; j++)
				docsMatrix[i][j] = docsMatrix[i][j] / val;
		}
	}

	public void computeTF_IDF() {
		int rows = noOfDocs;
		int columns = noOfWords;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++)
				docsMatrix[i][j] = docsMatrix[i][j] * idf[j];
		}
	}

	public void computeSimilarityMatrix(Matrix semantic) {
		int N = semantic.nrows();
		for (int i = 0; i < N; i++) {
			for (int j = i + 1; j < N; j++) {
				double sim = cosineSimil(docsMatrix[i], docsMatrix[j]);
				semantic.set(i, j, sim);
				semantic.set(j, i, sim);
			}
		}
	}

	double cosineSimil(double[] Vi, double[] Vj) {
		int len = Vi.length;
		double res, a, b;
		res = a = b = 0.0;
		for (int i = 0; i < len; i++) {
			res += (Vi[i] * Vj[i]);
			a += Math.pow(Vi[i], 2);
			b += Math.pow(Vj[i], 2);
		}
		res = res / (Math.sqrt(a) * Math.sqrt(b));
		return res;
	}
}