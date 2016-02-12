package edu.utdallas.cs6301_502;

import java.io.File;
import java.io.IOException;
import java.util.List;

import seers.methspl.MethodDoc;
import seers.methspl.MethodSplitter;

class Runner {

	public static void main(String... args) {
		// SETUP

		// the root folder where the code is located
		String baseFolder = "src/main/java";
		// create the instance of the method splitter
		MethodSplitter splitter = new MethodSplitter(baseFolder);

		// SPLITTING: use the following code for each file in the system

		// file path of the java file that is going to be parsed and split
		String filePath = baseFolder + File.separator + "edu/utdallas/cs6301_502/LuceneUtil.java";
		File file = new File(filePath);
		
		System.out.println(file.getAbsolutePath() + "; " + file.exists());
		
		try {
			// split the java file and return the list of methods in the file
			List<MethodDoc> methods = splitter.splitIntoMethods(file);
			
			for (MethodDoc method : methods) {
				System.out.println(method.getName());
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}