SiamCi integration prototype
Created 2/9/2011
Carlos Rueda - MBARI

This is the SIAM-CI integration prototype.

STATUS: Working initial proof-of-concept. It involves interaction with services in the python 
capability container to support instrument driver interface operations. It is in no way exhaustive
and the current design is just preliminary.

This is a Maven project with the JAR dependencies being those according to the SIAM middleware 
and the CI ion-object-definitions and ioncore-java libraries.

NOTE: this approach can be changed to use the ivy mechanism that was later enabled at OOICI.


* Preparation
-------------

It is assumed the following directory layout surrounding this project:
	<some-base-directory>/
		+ ion-object-definitions/    -- from git@github.com:ooici/ion-object-definitions.git
		+ ioncore-python/            -- from git@github.com:ooici/ioncore-python.git
		+ siam2/                     -- from MBARI folks;  assummed to be already built(*)
		+ siamci/                    -- from http://git.gitorious.org/siamci/siamci.git
	
		(*) siam2/jars/siam.jar and siam2/ports/TestSiamInstrument-1235.jar should available here; see 
		    https://confluence.oceanobservatories.org/display/CIDev/SIAM+test+of+enabling+a+new+instrument

where siamci/ is the parent directory of this README file. 
Note: If this is not the expected structure in your case, what follows should be easy to adjust.

Some files included in this project are intended to be placed elsewhere:

(Note: some of these can of course be pushed to corresponding repositories later on.)

$ cd <some-base-directory>/siamci/
$ mkdir -p ../ion-object-definitions/net/ooici/play/instr/
$ ln src/main/gpb/net/ooici/play/instr/instrument_defs.proto ../ion-object-definitions/net/ooici/play/instr/
$ ln src/main/python/ion/agents/instrumentagents/Siam*.py ../ioncore-python/ion/agents/instrumentagents/
$ ln src/main/python/ion/agents/instrumentagents/test/Siam*.py ../ioncore-python/ion/agents/instrumentagents/
$ ln src/main/python/ion/agents/instrumentagents/test/test_Siam*.py ../ioncore-python/ion/agents/instrumentagents/test/

Then, build the ion-object-definitions as usual:
$ cd <some-base-directory>/ion-object-definitions/
$ ant dist
$ cd python/
$ python setup.py install

Optionally, you can build ioncore-python as usual at this point (but the new SIAM scripts are not to be tested just yet).
$ cd <some-base-directory>/ioncore-python/
$ ant install

As some of the JAR dependencies (including ion-object-definitions, ioncore-java, SIAM) are not available 
in standard or external maven repositories, they are to be installed in the local repository before proceeding 
with normal development or build. The etc/build.xml ant script is included to install those dependencies.

Assuming the overall directory layout above, just run:
	ant -f etc/build.xml install
	
If needed, you can use the 'siam', 'ion-object-definitions', and 'ioncore-java' system properties as appropriate
to explicitly indicate any of these directories. For example:
	ant -f etc/build.xml -Dioncore-java=/Dev/ooici/ioncore-java install
See etc/build.xml for other properties that may need adjustment.



* Building the SIAM-CI adapter
------------------------------
Once the steps above have been completed and the dependencies are installed in your local maven repository:
	$ mvn clean compile
	
	
* Tests
-------

- Make sure your AMQP broker is running on localhost.

- In a shell session, run the SIAM-CI adapter:
	$ cd <some-base-directory>/siamci/
	$ mvn compile exec:java -Dsiam-ci
	.... output omitted here ....
	
- In another shell session, run the SIAM related unit tests in ioncore-python:
	$ cd <some-base-directory>/ioncore-python/
	$ trial ion/agents/instrumentagents/test/test_SiamCi.py
	ion.agents.instrumentagents.test.test_SiamCi
	  TestSiamCi
	    test_get_last_sample ...                                               [OK]
	    test_get_status ...                                                    [OK]
	    test_list_ports ...                                                    [OK]
	    test_ping ...                                                          [OK]
	
	-------------------------------------------------------------------------------
	Ran 4 tests in 2.779s
	
	PASSED (successes=4)


* Other
-------
The following are only SIAM related tests (ie., not involving the adapter).

- Run a program that uses a higher-level interface to the SIAM library; the maven profile 
  here is called "isiam" for "interface to SIAM:"
	$ mvn exec:java -Disiam
	...
	**listPorts:
	Node 999 has 2 ports
	
	 port name  device id              service     Status     sample      error      retry
	       Foo       1151                Dummy          -          0          0          0
	  testPort       1235   TEstSiamInstrument   SLEEPING        162          0          0
	
	**getPortStatus: port=testPort
	   status: SLEEPING
	
	**getPortLastSample: port=testPort
	   result: {buffer=161: Hello SIAM(161), parentId=999, systemTime=1298867339354, seqNo=7421, mdref=7262, recordType=1, nBytes=20}

The following examples use direct access to classes in the SIAM library via UtilRunner. These were
created during initial set-up of this project.
 
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
	
  The class that is run above is org.mbari.siam.operations.utils.PortLister in SIAM.
  	
  Note: the RMI exception above is about an instrument (DummyInstrument) that is installed in the 
  SIAM node but whose library is not in this project's classpath. By contrast, the library 
  corresponding to TestSiamInstrument is made a explicit dependency (see pom.xml) and so it's
  resolved properly. Of course, this could be done in a more dynamic way.

- A similar test, with org.mbari.siam.operations.utils.PortSampler:
	$ mvn exec:java -Dsiam-runner -Dexec.args="samplePort localhost testPort"
	...
	SIAM version $Name:  $
	TEstSiamInstrument:
	parentID=999, recordType=1
	devid=1235, t=1298867119326, seqNo=7413, mdref=7262
	nBytes=20
	153: Hello SIAM(153)
	
	sequenceNumber: 153 count
	foo:  Hello SIAM(153) string
	
For the test instrument see https://confluence.oceanobservatories.org/display/CIDev/SIAM+test+of+enabling+a+new+instrument


