SiamCi integration prototype
Created 2/9/2011
Carlos Rueda - MBARI

This is the SIAM-CI integration prototype.

For more information:
  - ChangeLog.txt
  - https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status


Basic tests
-----------
0) Have SIAM node running on localhost (with the test instrument on port testPort)

1) Run SIAM specific tests using maven: 
	$ SIAM="-" mvn test 
	...
	-------------------------------------------------------
	 T E S T S
	-------------------------------------------------------
	Running TestSuite
	SiamTestCase: SIAM host = 'localhost'; port = 'testPort'
	    testGetNodeId ...                                                           [OK]
	    testGetNodeInfo ...                                                         [OK]
	    testGetPortLastSample ...                                                   [OK]
	    testGetPortProperties ...                                                   [OK]
	    testGetPortStatus ...                                                       [OK]
	    testListPorts ...                                                           [OK]
	    testSetPortProperties ...                                                   [OK]
	
	Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 20.354 sec
 
Launching the SIA-CI adapter service
------------------------------------
0) Have SIAM node running on localhost
	$ mvn exec:java -Dsiam-ci
	...
	03-28-2011 23:02:40,370 [SiamCiMain        ] INFO  net.ooici.siamci.SiamCiMain - Listing instruments in the SIAM node:
	03-28-2011 23:02:40,378 [SiamCiMain        ] WARN  siam.PortLister - UnmarshalException with device by id '1151'. Is the instrument jar in the classpath?
	03-28-2011 23:02:40,390 [SiamCiMain        ] INFO  net.ooici.siamci.SiamCiMain -  * port='Foo' device='1151' service='Dummy'
	03-28-2011 23:02:40,390 [SiamCiMain        ] INFO  net.ooici.siamci.SiamCiMain -  * port='testPort' device='1235' service='TEstSiamInstrument'
	03-28-2011 23:02:40,390 [SiamCiMain        ] INFO  net.ooici.siamci.SiamCiMain - Starting SIAM-CI adapter. SIAM node host: localhost
	03-28-2011 23:02:40,501 [SiamCiServerIonMsg] INFO  net.ooici.siamci.impl.ionmsg.SiamCiServerIonMsg - Running SiamCiServerIonMsg (broker='localhost:5672', queue='SIAM-CI', excahange='magnet.topic')
	03-28-2011 23:02:40,501 [SiamCiServerIonMsg] INFO  net.ooici.siamci.impl.ionmsg.SiamCiServerIonMsg - [0] Waiting for request ...

	
	
	