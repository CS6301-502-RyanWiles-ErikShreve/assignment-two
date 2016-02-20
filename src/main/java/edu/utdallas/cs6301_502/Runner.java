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

	private static MethodSplitter splitter;
	private static LuceneUtil luceneUtil;	
	private TextScrubber textScrubber;	
	private BugReports bugReports;

	
	private static boolean debug = false;
	private static boolean create = true;

	
	public static void main(String... args) throws Exception {
		String baseFolder = args[0];
		String indexPath = args[1];
		String goldSetFile = args[2];
		
		luceneUtil = new LuceneUtil(create, indexPath);

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

		try {
			// SETUP
			textScrubber = new TextScrubber(loadWords("stop_words.xml"), loadWords("java_keywords.xml"), 2);
			
			bugReports = loadBugReports(goldSetFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void debug(String line) {
		if (Runner.debug) {
			System.out.println(line);
		}
	}

	private Set<String> loadWords(String resource) {
		Set<String> words = new HashSet<String>();
		
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(resource).getFile());
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String line = reader.readLine().trim();
				if (line.startsWith("<word>") && line.endsWith("</word>"))
					words.add(line.substring(6, line.length() - 7));

			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return words;
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
			for (String str : textScrubber.scrub(splitterMethodText)) {
				if (!str.isEmpty()) {
					builder.append(str + " ");
				}
			}
		}

		luceneUtil.indexDocument(fileName, methodName, builder.toString());
	}


}