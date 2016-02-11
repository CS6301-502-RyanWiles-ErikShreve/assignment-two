package test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import seers.methspl.MethodDoc;
import seers.methspl.MethodSplitter;

public class MainTest {

	public static void main(String[] args) throws IOException {

		// -----------------------
		// the root folder where the code is located
		String baseFolder = "jabref-src";
		// create the instance of the method splitter
		MethodSplitter splitter = new MethodSplitter(baseFolder);

		// -------------------------

		// file path of the java file that is going to be parsed and split
		String filePath = baseFolder + File.separator + "JabRefDesktop.java";
		// split the java file and return the list of methods
		List<MethodDoc> methods = splitter.splitIntoMethods(new File(filePath));

		// -------------------------

		// get the inner comments (single and block comments) and text elements
		// (identifiers, literals, javadoc)
		List<String> innerComments = methods.get(0).getInnerComments();
		List<String> txtElements = methods.get(0).getTxtElements();

		System.out.println("** Inner comments of method " + methods.get(0).getName());
		System.out.println(innerComments);

		System.out.println("** Tokens of method " + methods.get(0).getName());
		System.out.println(txtElements);

		// -------------------------

		System.out.println("----------------------------------------");

		printMethods(methods);

		// -------------------------

		System.out.println("----------------------------------------");

		methods = splitter.splitIntoMethods(new File(baseFolder + File.separator + "GrammarBasedSearchRule.java"));
		printMethods(methods);

		// -------------------------

	}

	private static void printMethods(List<MethodDoc> methods) {
		for (MethodDoc methodDoc : methods) {
			System.out.println(methodDoc);
		}
	}

}
