package edu.utdallas.cs6301_502;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Parser {
	private String indexDir;

	private Path indexPath;

	public Parser(String indexDir) {
		this.indexDir = indexDir;
		indexPath = new File(indexDir).toPath();
	}

	public static void main(String[] args) {
		Parser parser = new Parser("/Users/rwiles/lucene/index");

		parser.indexDocument("document title or id goes here", "document body that you'll query on goes here");
		List<Document> docs = parser.queryLucene("your query goes here");
		for (Document doc : docs) {
			System.out.println("Found matching document with the following title: " + doc.getField("title").stringValue());
			
		}
	}

	public void indexDocument(String title, String body) {
		try {
			Directory directory = FSDirectory.open(indexPath);
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);//  package create  Represents the creation or appended to the existing index database  
			IndexWriter indexWriter = new IndexWriter(directory, iwc);//  to write the document to the index database  

			Document document = new Document();
			Field FieldPath = new StoredField("title", title);
			Field FieldBody = new TextField("body", body, Store.YES);
			document.add(FieldPath);
			document.add(FieldBody);
			indexWriter.addDocument(document);

			indexWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Document> queryLucene(String queryString) {
		IndexSearcher searcher;
		URI indexUri = new File(indexDir).toURI();
		List<Document> docs = new ArrayList<Document>();
		
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexUri)));
			searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();

			QueryParser parser = new QueryParser("body", analyzer);

			Query query = parser.parse(queryString);
			TopDocs results = searcher.search(query, 60);
			ScoreDoc[] hits = results.scoreDocs;

			for (ScoreDoc scoreDoc : hits) {
				docs.add(searcher.doc(scoreDoc.doc));
				System.out.println(scoreDoc.score);
			}

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return docs;
	}

}
