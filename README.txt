SiamCi integration prototype
Created 2/9/2011
Carlos Rueda - MBARI

This is the SIAM-CI integration prototype.

STATUS: Preliminary.

This is a Maven project with the JAR dependencies being those according to the SIAM middleware 
and the CI ion-object-definitions and ioncore-java libraries.


* Preparing dependencies
------------------------
As some of the JAR dependencies (including ion-object-definitions, ioncore-java, SIAM) are not available 
in standard or external maven repositories, they are to be installed in the local repository before proceeding 
with normal development or build. The etc/build.xml ant script is included to install those dependencies. 
The ant script assumes the following locations:
   - ion-object-definitions --> ../../ion-object-definitions/ 
   - ioncore-java root directory --> ../../ioncore-java/ 
   - SIAM root directory --> ../../siam2/ 
both relative to etc/. 
If these locations are correct for your envirnoment, just run:
	ant -f etc/build.xml install
To explicitly indicate any of these directories, you can use the 'siam', 'ion-object-definitions', and 
'ioncore-java' system properties as appropriate. For example:
	ant -f etc/build.xml -Dioncore-java=/Dev/ooici/ioncore-java install
See etc/build.xml for other properties that may need adjustment.

NOTE: the above approach to be changed to use the ivy mechanism that was later enabled.


* Building the program
----------------------
Once the dependencies above are installed in your local maven repository:
	$ mvn clean compile
	
	
* Initial tests
---------------
See ChangeLong.txt


* Some tests for SIAM operations
--------------------------------
The following are only SIAM related (not with the adapter)

- Run a program similar to SIAM's listPorts:	
	$ mvn exec:java -Dsiam-runner -Dexec.args="listPorts localhost -stats"
	...
	SIAM version $Name:  $
	java.rmi.UnmarshalException: error unmarshalling return; nested exception is: 
		java.lang.ClassNotFoundException: org.mbari.siam.devices.dummy.DummyInstrument_Stub
	
	Node 999 has 2 ports
	
	Port Name ISI-ID            Service Status  S   E   R  
	--------- ------            ------- ------ --- --- --- 
	 testPort   1235 TEstSiamInstrument     OK 143   0   0 
	
  The class here is test.siam.utils.PortLister, which is an exact copy (except for the package)
  of org.mbari.siam.operations.utils.PortLister in SIAM.
  	
  Note: the RMI exception above is about an instrument (DummyInstrument) that is installed in the 
  SIAM node but whose library is not in this project's classpath. By contrast, the library 
  corresponding to TestSiamInstrument is made a explicit dependency (see pom.xml). Of course,
  this could be done in a more dynamic way.

- A similar test, but running the original utility class in the siam.jar library,
  org.mbari.siam.operations.utils.PortSampler:
	$ mvn exec:java -Dsiam-runner -Dexec.args="samplePort localhost testPort"
	...
	SIAM version $Name:  $
	TEstSiamInstrument:
	parentID=999, recordType=1
	devid=1235, t=1297323135484, seqNo=996, mdref=723
	nBytes=15
	275: Hello SIAM
	Parser not implemented.
		 
For the test instrument see https://confluence.oceanobservatories.org/display/CIDev/SIAM+test+of+enabling+a+new+instrument


