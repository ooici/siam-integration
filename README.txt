SIAM-CI integration prototype
Carlos Rueda - MBARI

Documentation is mainly maintained at:
  - https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status
  
For detailed updates, see ChangeLog.txt.


Building the SIAM-CI prototype
------------------------------

	Get the code:
		$ cd <some-base-directory>
		$ git clone git@github.com:ooici/siam-integration.git
	
	Build the java part:
		You will need: Apache Maven, Apache Ant
	
		$ cd <some-base-directory>/siam-integration
		$ (cd etc/maven && ant install)
		$ mvn compile

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
		- RabbitMQ broker on localhost
		- Open Source DataTurbine RBNB server on localhost
			This is in particular needed for the data acquisition operation as it is 
			the mechanism used by SIAM
		- SIAM node with test instrument on localhost
	
	Java part:
		$ cd <some-base-directory>/siam-integration
		$ mvn exec:java -Psiam-ci   # Runs the SIAM-CI adapter service
	
	Python part:
		In a different shell session:
		$ cd <some-base-directory>/siam-integration/python
		$ SIAM_CI=- bin/trial siamci
