#!/usr/bin/env python

"""
@file siamci/start_receiver_service.py
@author Carlos Rueda
@brief Simple service to receive asynchronous reponses from the SIAM-CI adapter.
       Based on responsesvc.py, part of example sent by Thom Lennan (2/22/11 email)
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
