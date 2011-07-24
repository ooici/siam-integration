SIAM-CI Integration Prototype
Carlos Rueda - MBARI

This directory is to handle two cases with dependencies involving maven
to facilitate the build of the SIAM-CI project:

  - "install" case: this is to install some  external artifacts directly 
     in the local maven repository
  - "deploy" case: this is to deploy some other external artifacts in the 
    MBARI Maven repository

NOTE: 
	OOI currently uses Ivy as the build mechanism for some of its projects; 
	Maven is not currently supported at OOI (at least not officially).
	
	Interestingly, it seems that ioncore-java in particular could be resolved 
	using maven (note the .pom file): 
   		http://ooici.net/releases/maven/repo/net/ooici/ioncore-java/0.2.5-dev/
   	but ionproto is missing a corresponding .pom. 


"install" case
--------------

This is to install some external artifacts directly in the local maven repository.

This is accomplished by the build.xml file.  Just run:
	$ ant install

This is always needed upon a fresh pull of the SIAM-CI project.


"deploy" case
-------------

This is to deploy some other external artifacts in the MBARI Maven repository.

NOTE: This step is only to be performed upon a new release or SNAPSHOT 
of the corresponding artifact.

The pom.xml in this directory is only needed for this deploy case; it indicates
the needed extension and the distribution management definitions.
          
What follows are the "deploy" commands I've used. In each case, I simply 
edit the command, and the run it by copy-n-pasting it in my command line.

* SIAM jar. This JAR is from my working SIAM environment, which I set up based
on a checkout from the SIAM repository in January 2011, so I'm using 2011-01 
as the version.
	$ mvn deploy:deploy-file \
	  -Durl=dav:https://mbari-maven-repository.googlecode.com/svn/repository \
	  -DrepositoryId=mbari-maven-repository \
	  -DgroupId=org.mbari.interim \
	  -DartifactId=siam \
	  -Dpackaging=jar \
	  -Dfile=/Users/carueda/workspace/siam2/jars/siam.jar \
	  -Dversion=2011-07rmi \
	  -DgeneratePom=true
  
* Test SIAM instrument. This JAR is from my working SIAM environment.
	$ mvn deploy:deploy-file \
	  -Durl=dav:https://mbari-maven-repository.googlecode.com/svn/snapshot-repository \
	  -DrepositoryId=mbari-maven-snapshot-repository \
	  -DgroupId=org.mbari.interim \
	  -DartifactId=siam-test-instr1 \
	  -Dpackaging=jar \
	  -Dfile=/Users/carueda/workspace/siam2/ports/TestSiamInstrument-1235.jar \
	  -Dversion=0.0.1rmi-SNAPSHOT \
	  -DgeneratePom=true
  

* Open Source DataTurbine RBNB: This one from my installation of V3.2B5 (not SIAM's)
	$ mvn deploy:deploy-file \
	  -Durl=dav:https://mbari-maven-repository.googlecode.com/svn/repository \
	  -DrepositoryId=mbari-maven-repository \
	  -DgroupId=org.mbari.interim \
	  -DartifactId=rbnb \
	  -Dpackaging=jar \
	  -Dfile=/Users/carueda/Software/RBNB/bin/rbnb.jar \
	  -Dversion=V3.2B5 \
	  -DgeneratePom=true
	  
