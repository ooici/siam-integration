#!/usr/bin/env python

"""
@file ion/siamci/start_receiver_service.py
@author Carlos Rueda
@brief Simple service to receive asynchronous reponses from the SIAM-CI adapter.
       Based on responsesvc.py, part of example sent by Thom Lennan (2/22/11 email)
"""

import logging
from twisted.internet import defer

from ion.core import ioninit
from ion.core import bootstrap

CONF = ioninit.config('startup.bootstrap-dx')

# Static definition of message queues
ion_messaging = ioninit.get_config('messaging_cfg', CONF)

# Static definition of service names
#dx_services = ioninit.get_config('services_cfg', CONF)


@defer.inlineCallbacks
def start():
    """
    Main function of bootstrap. Starts DX system with static config
    """
    logging.info("ION/DX bootstrapping now...")
    startsvcs = []
 
 
    services = [
        {'name':'ds1','module':'ion.services.coi.datastore','class':'DataStoreService','spawnargs':{'servicename':'datastore'}},
        {'name':'resource_registry1','module':'ion.services.coi.resource_registry.resource_registry','class':'ResourceRegistryService','spawnargs':{'datastore_service':'datastore'}},
        {'name':'responder','module':'ion.data.test.test_dataobject','class':'ResponseService'},
        {'name':'identity_registry','module':'ion.services.coi.identity_registry','class':'IdentityRegistryService'},
        
        {'name':'siamci_receiver','module':'ion.siamci.receiver_service','class':'SiamCiReceiverService'}
    ]
 
    startsvcs.extend(services)

    yield bootstrap.bootstrap(ion_messaging, startsvcs)

start()
