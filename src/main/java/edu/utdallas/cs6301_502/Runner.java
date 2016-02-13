package edu.utdallas.cs6301_502;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import seers.methspl.MethodDoc;
import seers.methspl.MethodSplitter;

class Runner {

	private static final String[] JAVA_KEYWORDS = new String[] {
			"abstract", "continue", "for", "new", "switch",
			"assert", "default", "goto", "synchronized",
			"boolean", "do", "if", "private", "this",
			"break", "double", "implements", "protected", "throw",
			"byte", "else", "public", "throws",
			"case", "enum", "instanceof", "return", "transient",
			"catch", "extends", "int", "short", "try",
			"char", "final", "interface", "static", "void",
			"class", "finally", "long", "strictfp", "volatile",
			"const", "float", "native", "super", "while", "null", "true", "false" };
	
	private static MethodSplitter splitter;
	
	private static Set<String> keywordSet = new HashSet<String>();
	
	private boolean debug = false;
	
	private static boolean create = true;
	
	public static void main(String... args) throws IOException {
		// SETUP

		keywordSet.addAll(Arrays.asList(JAVA_KEYWORDS));
		
		// the root folder where the code is located
		String baseFolder = args[0];
		// create the instance of the method splitter
		splitter = new MethodSplitter(baseFolder);

		String indexPath = args[1];
		System.out.println("Indexing to directory '" + indexPath + "'...");

		Directory dir = FSDirectory.open(Paths.get(indexPath));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (create) {
			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
		} else {
			// Add new documents to an existing index:
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}		

		IndexWriter writer = new IndexWriter(dir, iwc);

		
		Runner r = new Runner();
		r.walkFolder(Paths.get(baseFolder), writer);
	}
	
	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}
	
	private void walkFolder(Path path, final IndexWriter writer) throws IOException
	{
		// Based on code from Lucene demo (https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html)
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) throws IOException {
				try {
					if (file.endsWith(".java"))
					{
						System.out.println("processing " + file.toString());
					
						File f = file.toFile();
						List<MethodDoc> methods = splitter.splitIntoMethods(f);
					
						for (MethodDoc m : methods)
						{
							indexMethod(writer, m, file);
						}
					}
				}
				catch (IOException ignore)
				{
					System.out.println("Could not index: " + file);
				}
				return FileVisitResult.CONTINUE;
			}
			
		});
	}
	
	private void indexMethod(IndexWriter writer, MethodDoc m, Path path) throws IOException
	{
		Document doc = new Document();
		Field nameField = new StringField("name", m.getName(), Field.Store.YES);
		doc.add(nameField);
		
		Field pathField = new StringField("path", path.toString(), Field.Store.YES);
		doc.add(pathField);
		
		List<String> txtElements = m.getTxtElements();
		
		System.out.println("Adding text(s) to body:");
		
		for (String t : txtElements)
		{
			System.out.println(t);
			String token = parse(t);
			if (!token.isEmpty())
			{
				doc.add(new TextField("body", t, Field.Store.YES));
			}
		}
		
		if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
			// New index, so we just add the document (no old document can be there):
			System.out.println("adding " + m.getName());
			writer.addDocument(doc);
		} else {
			// FIXME: Is the method name enough to uniquely ID a document?
			System.out.println("updating " + m.getName());
			writer.updateDocument(new Term("name", m.getName()), doc);
		}
		
	}
	
	// FIXME: Still need to remove stopwords
	private String parse(String text) throws FileNotFoundException, IOException {
		StringBuilder builder = new StringBuilder();

		boolean inDoc = false;
		boolean hasProcessedPackage = false;


		text = text.trim();

		if (text.isEmpty()) {
			return text;
		}

		// package statement must be the first non-comment line in java
		if (!hasProcessedPackage && text.startsWith("package ")) {
			hasProcessedPackage = true;

			// Remove package part and process remainder of line (could be a comment)
			text = text.replaceFirst("package .+;", " ").trim();
		}

		// import statements must follow the package statement, if present, and come before the rest
		// comments are allowed in the imports
		if (text.startsWith("import ")) {
			// Remove import part and process remainder of line (could be a comment)
			text = text.replaceFirst("import .+;", "");
		}

		// check for line with only // comments
		if (text.startsWith("//")) {
			text = text.substring(2).trim();
			//				debug(line);
		}

		if (inDoc) {
			if (text.contains("*/")) {
				inDoc = false;
				text = text.replace("*/", " ").trim();
			} else if (text.startsWith("*")) {
				text = text.substring(1).trim();
			}
			//				debug(line);
		}

		// check for javadoc style comments
		if (text.startsWith("/**")) {
			inDoc = true;
			text = text.substring(3).trim();
			//				debug(line);
		}

		// check for c style comments
		if (text.startsWith("/*")) {
			inDoc = true;
			text = text.substring(2).trim();
			//				debug(line);
		}

		// explode punctuation to a space
		text = text.replaceAll("[\\{|\\}|\\(|\\)|;|,|=|+|\\-|*|\"|'|/|\\?|:|<|\\[|\\]|!|\\>|\\^|\\$|\\&\\&|\\|\\||\\.|`|#|~|_]", " ").trim();
		text = text.replaceAll("\\\\t", " ").trim();
		text = text.replaceAll("\\\\r", " ").trim();
		text = text.replaceAll("\\\\n", " ").trim();
		text = text.replaceAll("\\\\", " ").trim();
		text = text.replaceAll("(^\\s)[0-9]+\\.[0-9]+", " ").trim(); // decimal numbers
		text = text.replaceAll("(^\\s)[0-9]+f", " ").trim(); // integer numbers as a float
		text = text.replaceAll("(^\\s)[0-9]+d", " ").trim(); // integer numbers as a double
		text = text.replaceAll("(^\\s)0[x|X][0-9a-fA-F]+", " ").trim(); // integer numbers as hex
		text = text.replaceAll("(^\\s)[0-9]+", " ").trim(); // integer numbers
		text = text.replaceAll("\\s+", " ");

		// Split CamelCase
		if (!text.isEmpty()) {
			StringBuilder lineBuilder = new StringBuilder();

			for (String word : text.split("\\s+")) {
				if (word.length() > 2) {
					if (!keywordSet.contains(word)) {
						lineBuilder.append(word);
						lineBuilder.append(" ");

						String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

						if (explodedWord.length > 1) {
							for (String w : explodedWord) {
								if (w.length() > 2) { // Don't include 1 and 2 character words
									lineBuilder.append(w);
									lineBuilder.append(" ");
								}
							}
						}
					}
				}
			}

			text = lineBuilder.toString();
			if (!text.isEmpty()) {					
				debug(lineBuilder.toString());
				builder.append(" " + lineBuilder.toString());
			}
		}


		return builder.toString();
	}

}