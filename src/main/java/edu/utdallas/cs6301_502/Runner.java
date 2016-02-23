// ***************************************************************************
// Assignment: 2
// Team : 2
// Team Members: Ryan Wiles, Erik Shreve
//
// Code reuse/attribution notes:
// args4j (for command line parsing) based on example code from:
// https://github.com/kohsuke/args4j/blob/master/args4j/examples/SampleMain.java
// walkFolder method based on example code from:
// https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
// ***************************************************************************
package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import edu.utdallas.cs6301_502.dto.BugReport;
import edu.utdallas.cs6301_502.dto.BugReports;
import edu.utdallas.cs6301_502.dto.Method;
import edu.utdallas.cs6301_502.dto.ModifiedMethods;
import seers.methspl.MethodDoc;
import seers.methspl.MethodSplitter;

class Runner {
	@Option(name = "-d", usage = "print debug information to console")
	private boolean debug = false;

	@Option(name = "-c", usage = "create new index")
	private static boolean create = false;

	@Option(name = "-src", usage = "base folder containing .java source to index")
	private String baseFolder = "";

	@Option(name = "-i", usage = "index location")
	private String indexPath = "";

	@Option(name = "-g", usage = "gold set file")
	private String goldSetFile = "";

	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	private static MethodSplitter splitter;
	private static LuceneUtil luceneUtil;
	private TextScrubber textScrubber;
	private BugReports bugReports;

	public static void main(String... args) throws Exception {

		Runner r = new Runner();
		r.doMain(args);
		r.run();
	}

	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			// check if enough arguments are given.
			if (goldSetFile.isEmpty())
				throw new CmdLineException(parser, "No gold set file was provided");

			if (indexPath.isEmpty())
				throw new CmdLineException(parser, "No index location was provided");

			if (create && baseFolder.isEmpty())
				throw new CmdLineException(parser, "No base folder was provided, but create new index was specified");

		} catch (CmdLineException e) {
			// report an error message.
			System.err.println(e.getMessage());
			System.err.println("java Runner [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			return;
		}
	}

	public Runner() {
		super();
	}

	public void run() {
		try {
			// SETUP
			luceneUtil = new LuceneUtil(create, indexPath);
			textScrubber = new TextScrubber(loadWords("stop_words.xml"), loadWords("java_keywords.xml"), 2);

			if (!baseFolder.isEmpty()) {
				System.out.println("Indexing to directory '" + indexPath + "'...");

				if (create) {
					splitter = new MethodSplitter(baseFolder);
					walkFolder(Paths.get(baseFolder));
				}
			} else {
				System.out.println("Using index at '" + indexPath + "'");
			}

			bugReports = loadBugReports(goldSetFile);

			HashMap<String, Double> results = new HashMap<String, Double>();
			
			// RUN QUERIES		
			for (BugReport bugReport : bugReports.getBugReports()) {
				ModifiedMethods modifiedMethods = bugReport.getChangeSet().getModifiedMethods();
				System.out.println("=======================================================================");
				System.out.println("Bug ID: " + bugReport.getId() + "; Bug Info: " + bugReport.getTitle());
				System.out.println("  Methods Changed: ");
				for (Method m : bugReport.getChangeSet().getModifiedMethods().getMethods()) {
					System.out.println("  " + m.getSignature() + " from " + m.getFile());
				}

				debug("Results from Title + Description:");
				results.putAll(doQuery(bugReport.getTitle() + bugReport.getDescription(), modifiedMethods, bugReport.getId() + "_td", 20));

				debug("Results from Title Only:");
				results.putAll(doQuery(bugReport.getTitle(), modifiedMethods, bugReport.getId() + "_t", 20));

				debug("Results from Description Only:");
				results.putAll(doQuery(bugReport.getDescription(), modifiedMethods, bugReport.getId() + "_d", 20));
			}

			double avgP5, avgP10, avgP20;
			double avgR5, avgR10, avgR20;
			avgP5 = avgP10 = avgP20 = avgR5 = avgR10 = avgR20 = 0;
			int count = 0;
			
			for (BugReport bugReport : bugReports.getBugReports()) {
				System.out.println("====================================== RESULTS ====================================");
				System.out.println("Bug ID: " + bugReport.getId() + "; Bug Info: " + bugReport.getTitle());
				
				for (String s : new String[] { "_td", "_t", "_d"} ) {
					String prefix = bugReport.getId() + s;
					String precision = results.get(prefix + "_p_5") + ", " + results.get(prefix + "_p_10") + ", " + results.get(prefix + "_p_20");
					String recall = results.get(prefix + "_r_5") + ", " + results.get(prefix + "_r_10") + ", " + results.get(prefix + "_r_20");
					String effectiveness = results.get(prefix + "_effectiveness") + "";
					
					avgP5 += results.get(prefix + "_p_5");
					avgP10 += results.get(prefix + "_p_10");
					avgP20 += results.get(prefix + "_p_20");
					
					avgR5 += results.get(prefix + "_r_5");
					avgR10 += results.get(prefix + "_r_10");
					avgR20 += results.get(prefix + "_r_20");
					count++;
					
					if ("_td".equals(s)) {
						System.out.println("Title & Description\t\t" + precision + "\t\t" + recall + "\t\t" + effectiveness);
					} else if ("_t".equals(s)) {
						System.out.println("Title              \t\t" + precision + "\t\t" + recall + "\t\t" + effectiveness);
					} else if ("_d".equals(s)) {
						System.out.println("Description        \t\t" + precision + "\t\t" + recall + "\t\t" + effectiveness);
					}
				}
			}
			String precision = (avgP5 / count) + ", " + (avgP10 / count) + ", " + (avgP20 / count);
			String recall = (avgR5 / count) + ", " + (avgR10 / count) + ", " + (avgR20 / count);
			System.out.println("====================================== RESULTS ====================================");
						System.out.println("Average Results    \t\t" + precision + "\t\t" + recall);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}

	private Set<String> loadWords(String resource) {
		Set<String> words = new HashSet<String>();

		ClassLoader classLoader = getClass().getClassLoader();
		System.out.println("classloader == null: " + (classLoader == null));
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
					System.out.println("\tFile: " + method.getFile() + "; " + method.getSignature());
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

	private HashMap<String, Double> doQuery(String dirtyQueryString, ModifiedMethods modifiedMethods, String id, int limit) throws IOException, ParseException {
		String cleanQueryString = "";
		List<String> queryStrings = textScrubber.scrub(dirtyQueryString);
		for (String q : queryStrings) {
			cleanQueryString = cleanQueryString + " " + q;
		}

		List<Document> docResults = luceneUtil.queryLucene(cleanQueryString, limit);

		Integer[] measurementValsArr = new Integer[] { 5, 10, 20 };
		List<Integer> measurementVals = Arrays.asList(measurementValsArr);

		HashMap<String, Double> results = new HashMap<String, Double>();

		int effectiveness = 0;
		for (Integer val : measurementVals) {
			int numberOfReturnedDocuments = 0;
			int numberOfReturnedRelevantDocuments = 0;

			HashSet<String> recall = new HashSet<String>();

			for (Document d : docResults) {
				String methodName = d.get("title");
				String fileName = d.get("fileName");

//				debug(methodName + " from " + d.get("fileName"));
				numberOfReturnedDocuments++;

				for (Method method : modifiedMethods.getMethods()) {
					if (fileName.endsWith(method.getFile())) {
						debug("\tmatched file: " + method.getSignature() + " vs " + methodName);
						if (method.getSignature().equals(methodName)) {
							debug("\tmatched method!!!");
							numberOfReturnedRelevantDocuments++;
							recall.add(method.getSignature());
							if (effectiveness == 0) {
								effectiveness = numberOfReturnedDocuments;
							}
						}
					}
				}
				if (numberOfReturnedDocuments == val) {
					break;
				}
			}
			if (val == numberOfReturnedDocuments) {
				results.put(id + "_p_" + numberOfReturnedDocuments, (numberOfReturnedRelevantDocuments / (double) numberOfReturnedDocuments));
				results.put(id + "_r_" + numberOfReturnedDocuments, (recall.size() / (double) modifiedMethods.getMethods().size()));
				debug("precision @" + numberOfReturnedDocuments + ": " + (numberOfReturnedRelevantDocuments / (double) numberOfReturnedDocuments));
				debug("recall @" + numberOfReturnedDocuments + ": " + (recall.size() / (double) modifiedMethods.getMethods().size()));
			}
		}
		results.put(id + "_effectiveness", (double) effectiveness);
		debug("effectiveness: " + effectiveness);
		return results;
	}

	private void walkFolder(Path path) throws IOException {
		System.out.println("Input directory '" + path.toString() + "'...");

		long start = System.nanoTime();

		luceneUtil.openIndexForAdd();
		final ExecutorService executor = Executors.newFixedThreadPool(4); // FIXME: make number of threads configurable

		// Based on code from Lucene demo (https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html)
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				try {
					if (file.toString().endsWith(".java") && !file.toString().contains("test")) {
						System.out.println("processing " + file.toString());

						File f = file.toFile();
						List<MethodDoc> methods = splitter.splitIntoMethods(f);

						for (MethodDoc m : methods) {
							Runnable methodIndexer = new MethodIndexer(m, file);
							executor.execute(methodIndexer);
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

		executor.shutdown();
		while (!executor.isTerminated()) {}

		luceneUtil.closeIndexForAdd();

		long duration = System.nanoTime() - start;
		debug("Index time (ns): " + duration);
	}

	private class MethodIndexer implements Runnable {
		private MethodDoc m;
		private Path path;

		public MethodIndexer(MethodDoc m, Path path) {
			super();
			this.m = m;
			this.path = path;
		}

		private void indexMethod() throws IOException {
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

		public void run() {
			try {
				indexMethod();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}