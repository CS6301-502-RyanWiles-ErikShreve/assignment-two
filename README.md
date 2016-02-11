# cs6301-method-splitter
Use of the library to split java files into methods.

#Usage

1. In Eclipse, make sure you add all the jars files, located in the lib folder, to the build path
2. Use the API to split java files into methods. Example:

```java

        // SETUP
		
		// the root folder where the code is located
		String baseFolder = "jabref-src";
		// create the instance of the method splitter
		MethodSplitter splitter = new MethodSplitter(baseFolder);

		// SPLITTING: use the following code for each file in the system

		// file path of the java file that is going to be parsed and split
		String filePath = baseFolder + File.separator + "JabRefDesktop.java";
		// split the java file and return the list of methods in the file
		List<MethodDoc> methods = splitter.splitIntoMethods(new File(filePath));
```
