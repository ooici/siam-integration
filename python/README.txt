SIAM-CI Integration Prototype (Python)
Carlos Rueda - MBARI

This is the python part.

1- If not already, create a virtualenv to isolate system site-packages:
	$ mkvirtualenv --no-site-packages --python=/usr/bin/python2.5 siamci
	$ workon siamci

2- If not already, run bootstrap.py to set up buildout:  
	$ python bootstrap.py
    
3- Run this as many times as you change buildout.cfg and/or any parent files:
	$ bin/buildout

4- Run the SIAM-CI integration tests:
   a- Make sure the SIAM-CI adapter service is running
   b- Run the python tests:
	$ SIAM_CI=- bin/trial siamci


