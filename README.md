# assignment-two

Due February 23, 2016, 5:00 pm CT

Team #1

## Systems:

### Eclipse JDT/Core

[Tag for last official release (4.5.1)](http://git.eclipse.org/c/jdt/eclipse.jdt.core.git/tag/?h=M20150904-0015)

[5 specific bugs](https://bugs.eclipse.org/bugs/buglist.cgi?bug_id=470506%2C470986%2C469753%2C471090%2C465566%20&bug_id_type=anyexact&columnlist=product%2Ccomponent%2Cassigned_to%2Cbug_status%2Cresolution%2Cshort_desc%2Cchangeddate&query_format=advanced)
that are all present in 4.5 and fixed in 4.5.1, and have commit messages referencing the bug id, and don't have eclipse code in the bug description.


### Apache Ant

[Tag for the 1.9.4 release of Ant:](https://github.com/apache/ant/commit/1c927b15af84cfce315a0ef6f4db60c7d47c2c50)
[5 specific bugs](https://bz.apache.org/bugzilla/buglist.cgi?bug_id=57965%2C58886%2C57789%2C57048%2C57822&bug_id_type=anyexact&bug_status=RESOLVED&product=Ant&query_format=advanced&resolution=FIXED&version=1.9.4)

## Use of CS6301-method-splitter

The assignment requests the use of https://github.com/amarcusgit/cs6301-method-splitter.git to split .java files into methods for the corpus.
This repo has been added to our repo as a subtree, located at /method-splitter.

To update the subtree:

Change your CWD to the root of our repo (this is very important as the commands below assume this.)

Run:
    git remote -v 
If a link to  https://github.com/amarcusgit/cs6301-method-splitter.git 
is not present, run:
    git remote add -f method-splitter  https://github.com/amarcusgit/cs6301-method-splitter.git

Then run:
    git subtree pull --prefix methdod-splitter method-splitter master --squash
	
Finally, commit and push.
