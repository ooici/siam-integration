SiamCi integration prototype
Created 2/9/2011
Carlos Rueda - MBARI

This is the SIAM-CI integration prototype.

STATUS: Very preliminary. Just general preparations at this point.

This is a Maven project with the JAR dependencies being those according to the SIAM middleware 
and the CI ioncore-java library.

As some of the JAR dependencies are not available in standard or external maven repositories, 
they are to be installed in the local repository before proceeding with normal development or build. 
The etc/build.xml ant script is included to install those dependencies. Just run:
	ant -f etc/build.xml install
This assumes the following locations:
   - ioncore-java root directory --> ../../ioncore-java/ 
   - SIAM root directory --> ../../siam2/ 
both relative to etc/. 
To explicitly indicate these directories, use the 'ioncore-java' and 'siam' system properties, 
for example:
	ant -f etc/build.xml -Dioncore-java=/Dev/ooici/ioncore-java install


Build:
	$ mvn clean compile
	
Initial tests:

- Run a program similar to SIAM's listPorts:	
	$ mvn exec:java -Dexec.args="listPorts localhost -stats"
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
	$ mvn exec:java -Dexec.args="samplePort localhost testPort"
	...
	SIAM version $Name:  $
	TEstSiamInstrument:
	parentID=999, recordType=1
	devid=1235, t=1297323135484, seqNo=996, mdref=723
	nBytes=15
	275: Hello SIAM
	Parser not implemented.
		 
For the test instrument see https://confluence.oceanobservatories.org/display/CIDev/SIAM+test+of+enabling+a+new+instrument


