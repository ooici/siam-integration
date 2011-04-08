#!/usr/bin/env python

"""
@file ion/siamci/receiver_service.py
@author Carlos Rueda
@brief Simple service to receive asynchronous reponses from the SIAM-CI adapter
"""

import sys
import traceback

import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer


from ion.core.object import object_utils
from ion.core.process.process import ProcessFactory
from ion.core.process.service_process import ServiceProcess, ServiceClient

from ion.core.messaging.message_client import MessageClient

from ion.services.coi.resource_registry_beta.resource_client import ResourceClient, ResourceInstance



class SiamCiReceiverService(ServiceProcess):
    """
    Simple service to receive asynchronous reponses from the SIAM-CI adapter
    """
    # Declaration of service
    declare = ServiceProcess.service_declare(name='siamci_receiver',
                                             version='0.1.0',
                                             dependencies=[])

    def __init__(self, *args, **kwargs):
        ServiceProcess.__init__(self, *args, **kwargs)
        self.rc = ResourceClient(proc=self)
        
        self.mc = MessageClient(proc=self)

        log.info('$$$$$$$$$$$$$$$$$$$$$$$$$$$$ SiamCiReceiverService.__init__()')

    def slc_init(self):
        pass

        
    @defer.inlineCallbacks
    def op_acceptResponse(self, content, headers, msg):
        log.info('$$$$$$$$$$$$$$$$$$$$$$$$$$$$ op_acceptResponse:\n'+str(content))
        
        yield self.reply_ok(msg, {'value' : "TODO-some-result"})


class SiamCiReceiverServiceClient(ServiceClient):
    def __init__(self, proc=None, **kwargs):
        if not 'targetname' in kwargs:
            kwargs['targetname'] = "siamci_receiver"
        ServiceClient.__init__(self, proc, **kwargs)

    @defer.inlineCallbacks
    def acceptResponse(self):
        payload = {'userId' : "some-userid",
                   'foo' : "baz"}
        (content, headers, payload) = yield self.rpc_send('acceptResponse', payload)
        log.info('acceptResponse service reply: ' + str(content))
        defer.returnValue(content)
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamCiReceiverService)

