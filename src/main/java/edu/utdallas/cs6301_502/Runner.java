package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import edu.utdallas.cs6301_502.dto.BugReport;
import edu.utdallas.cs6301_502.dto.BugReports;
import edu.utdallas.cs6301_502.dto.Method;
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
	private static LuceneUtil luceneUtil;

	private static Set<String> keywordSet = new HashSet<String>();
	private Set<String> stopWords = new HashSet<String>();
	
	private BugReports bugReports;

	private static boolean debug = false;
	private static boolean create = true;

	
	public static void main(String... args) throws Exception {
		String baseFolder = args[0];
		String indexPath = args[1];
		String goldSetFile = args[2];
		
		luceneUtil = new LuceneUtil(create, indexPath);
		// SETUP

		keywordSet.addAll(Arrays.asList(JAVA_KEYWORDS));

		// the root folder where the code is located
		// create the instance of the method splitter
		splitter = new MethodSplitter(baseFolder);

		System.out.println("Indexing to directory '" + indexPath + "'...");

		Runner r = new Runner(true, goldSetFile);
		r.walkFolder(Paths.get(baseFolder));
	}

	public Runner(boolean debug, String goldSetFile) {
		super();
		this.debug = debug;
		loadStopWords();
		try {
			bugReports = loadBugReports(goldSetFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}

	private void loadStopWords() {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("stop_words.xml").getFile());
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String line = reader.readLine().trim();
				if (line.startsWith("<word>") && line.endsWith("</word>"))
					stopWords.add(line.substring(6, line.length() - 7));

			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private BugReports loadBugReports(String resourceFile) throws Exception {
		JAXBContext jaxbContext = JAXBContext.newInstance(BugReports.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		StringReader reader = new StringReader(readResourceFile(resourceFile));
		BugReports bugReports = (BugReports) jaxbUnmarshaller.unmarshal(reader);

		if (debug) {
			for (BugReport report : bugReports.getBugReports()) {
				System.out.println("Title: " + report.getTitle());
				System.out.println("Description: " + report.getDescription());

				System.out.println("\tSystem Revision: " + report.getChangeSet().getSystemRevision());

				for (Method method : report.getChangeSet().getModifiedMethods().getMethods()) {
					System.out.println("\tFile: " + method.getFile());
				}
			}
		}
		return bugReports;
	}

	private String readResourceFile(String resourceName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				builder.append(reader.readLine());
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

	private void walkFolder(Path path) throws IOException {
		System.out.println("Input directory '" + path.toString() + "'...");

		// Based on code from Lucene demo (https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html)
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				try {
					if (file.toString().endsWith(".java")) {
						System.out.println("processing " + file.toString());

						File f = file.toFile();
						List<MethodDoc> methods = splitter.splitIntoMethods(f);

						for (MethodDoc m : methods) {
							indexMethod(m, file);
						}
					} else {
						debug("ignoring " + file.toString());
					}
				} catch (IOException ignore) {
					System.out.println("Could not index: " + file);
				}
				return FileVisitResult.CONTINUE;
			}

		});
	}

	private void indexMethod(MethodDoc m, Path path) throws IOException {
		String fileName = path.toString();
		String methodName = m.getName();

		debug("fileName: " + fileName + "; methodName: " + methodName);
		
		StringBuilder builder = new StringBuilder();
		
		for (String splitterMethodText : m.getTxtElements()) {
			for (String str : parse(splitterMethodText)) {
				if (!str.isEmpty()) {
					builder.append(str + " ");
				}
			}
		}

		luceneUtil.indexDocument(fileName, methodName, builder.toString());
	}

	// FIXME: This function is checking for more that it probably needs to. Probably no need to look for import/package statements or comments.
	private List<String> parse(String text) throws FileNotFoundException, IOException {

		List<String> output = new ArrayList<String>();

		boolean inDoc = false;
		boolean hasProcessedPackage = false;

		text = text.trim();

		if (text.isEmpty()) {
			return output;
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
			for (String word : text.split("\\s+")) {
				if (word.length() > 2) {
					if (!keywordSet.contains(word.toLowerCase()) && !stopWords.contains(word.toLowerCase())) {
						output.add(word);

						String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

						if (explodedWord.length > 1) {
							for (String w : explodedWord) {
								if (w.length() > 2 && !stopWords.contains(word.toLowerCase())) { 
									output.add(w);
								}
							}
						}
					}
				}
			}
		}

		return output;
	}

}