#!/usr/bin/env python

"""
@file siamci/start_receiver_service.py
@author Carlos Rueda
@brief Simple service to receive asynchronous reponses from the SIAM-CI adapter.
       Based on responsesvc.py, part of example sent by Thom Lennan (2/22/11 email)
"""


"""
This service can be started as follows:

    $ cd python      # base directory of the SIAM-CI python code
    $ workon siamci  # or your corresponding virtenv
    $ bin/twistd --pidfile=ps1 -n cc -a sysname=siamci -h localhost src/siamci/start_receiver_service.py
    2011-05-12 16:31:37-0700 [-] Log opened.
    2011-05-12 16:31:37-0700 [-] twistd 11.0.0 (/Users/carueda/ooici3/Dev/virtenvs/siamci/bin/python 2.5.4) starting up.
    2011-05-12 16:31:37-0700 [-] reactor class: twisted.internet.selectreactor.SelectReactor.
    2011-05-12 16:31:37.663 [start_receiver_service: 55] INFO :Starting siamci_receiver service ...
    ...
    ION Python Capability Container (version 0.4.13)
    [env: /Users/carueda/ooici3/Dev/virtenvs/siamci/lib/python2.5/site-packages] 
    [container id: carueda@carueda.local.53913] 
    
    ><> 2011-05-12 16:31:45.095 [receiver_service:111] WARNING:op_acceptResponse: publish_id not given


The last line is generated when you run the SiamCiReceiverTest on the java side:
    $ SIAM_CI_RECEIVER=- mvn test -Dtest=SiamCiReceiverTest
"""

import logging
from twisted.internet import defer

from ion.core import bootstrap

@defer.inlineCallbacks
def start():
    """
    Starts a SiamCiReceiverService instance.
    """
    
    logging.info("Starting siamci_receiver service ...")
 
    services = [
        {'name':'siamci_receiver','module':'siamci.receiver_service','class':'SiamCiReceiverService'}
    ]
 
    yield bootstrap.spawn_processes(services)

start()
