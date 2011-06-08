SIAM-CI integration prototype
Carlos Rueda - MBARI

Documentation is mainly maintained at:
  - https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status
  
For detailed updates, see ChangeLog.txt.


NOTE: the following build and run simplified instructions are appropriate 
for a local development environment where all needed runtime components
are on localhost. These instructions will be complemented as part of 
OOIION-67 to facilitate nightly builds and the use of a new VM where a 
SIAM node and an RBNB server will be running (OOIION-63).

Building the SIAM-CI prototype
------------------------------

	Get the code and check out develop branch:
		$ cd <some-base-directory>
		$ git clone git@github.com:ooici/siam-integration.git
		$ cd siam-integration
		$ git checkout -b develop origin/develop
	
	Build the java part:
		You will need: Apache Maven, Apache Ant
	
		$ cd <some-base-directory>/siam-integration
		$ ./siamci.sh build

	Python part:
		You will need: Python 2.5, virtualenv
		 
		$ cd <some-base-directory>/siam-integration/python
		$ mkvirtualenv --no-site-packages --python=/usr/bin/python2.5 siamci
		$ workon siamci
		$ python bootstrap.py  # only once
		$ bin/buildout         # whenever you change buildout.cfg and/or any parent files 


Running the integration tests
-----------------------------
	Runtime prerequisites:
		- RabbitMQ broker on some host (by default, localhost)
		- Open Source DataTurbine RBNB server on some host:port (by default, localhost:3333)
			This is in particular needed for the data acquisition operation as it is 
			the mechanism used by SIAM
		- SIAM node with test instrument on some host (by default, localhost)
	
	Run the SIAM-CI adapter service:
		$ cd <some-base-directory>/siam-integration
		$ ./siamci.sh start
	
	Run the integration tests (python):
		In a different shell session:
		$ cd <some-base-directory>/siam-integration/python
		$ SIAM_CI=- bin/trial siamci
		
	Shut down the SIAM-CI adapter service:
		$ cd <some-base-directory>/siam-integration
		$ ./siamci.sh stop
	
