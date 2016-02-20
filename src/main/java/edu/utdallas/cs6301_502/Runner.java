//***************************************************************************
// Assignment: 2
// Team : 2
// Team Members: Ryan Wiles, Erik Shreve
//
// Code reuse/attribution notes:
// args4j (for command line parsing) based on example code from:
//   https://github.com/kohsuke/args4j/blob/master/args4j/examples/SampleMain.java
// walkFolder method based on example code from:
//   https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
//***************************************************************************
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import edu.utdallas.cs6301_502.dto.BugReport;
import edu.utdallas.cs6301_502.dto.BugReports;
import edu.utdallas.cs6301_502.dto.Method;
import seers.methspl.MethodDoc;
import seers.methspl.MethodSplitter;

class Runner {
	@Option(name="-d",usage="print debug information to console")
	private boolean debug = false;
		
	@Option(name="-c",usage="create new index")
	private static boolean create = false;

	@Option(name="-src", usage="base folder containing .java source to index")
	private String baseFolder = "";
	
	@Option(name="-i", usage="index location")
	private String indexPath = "";
	
	@Option(name="-g", usage="gold set file")
	private String goldSetFile ="";
	
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

	public void doMain(String[] args)
	{
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);
			
			// check if enough arguments are given.
			if( goldSetFile.isEmpty() )
				throw new CmdLineException(parser,"No gold set file was provided");
			
			if( indexPath.isEmpty() )
				throw new CmdLineException(parser,"No index location was provided");
			
			if( create && baseFolder.isEmpty() )
				throw new CmdLineException(parser,"No base folder was provided, but create new index was specified");

		} catch( CmdLineException e ) {
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

	public void run()
	{
		try {
			// SETUP
			luceneUtil = new LuceneUtil(create, indexPath);
			textScrubber = new TextScrubber(loadWords("stop_words.xml"), loadWords("java_keywords.xml"), 2);
			
			if (!baseFolder.isEmpty())
			{
				System.out.println("Indexing to directory '" + indexPath + "'...");				
			
				splitter = new MethodSplitter(baseFolder);	
				walkFolder(Paths.get(baseFolder));
			}
			else
			{
				System.out.println("Using index at '" + indexPath + "'");
			}
			
			bugReports = loadBugReports(goldSetFile);
			
			// TODO: RUN QUERIES		
			for (BugReport bugReport : bugReports.getBugReports())
			{
				String query = "";
				List<String> queryStrings = textScrubber.scrub(bugReport.getTitle() + bugReport.getDescription());
				for (String q : queryStrings)
				{
					query = query + " " + q;
				}
				
				List<Document> docResults = luceneUtil.queryLucene(query);
				
				for (Document d : docResults)
				{
					System.out.println(d.get("title") + " from " + d.get("fileName"));
				}
				
			}
						
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