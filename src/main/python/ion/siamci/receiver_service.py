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
        log.debug('SiamCiReceiverService.__init__()')
        
        self.rc = ResourceClient(proc=self)
        self.mc = MessageClient(proc=self)
        
        self.expect = set()
        self.accepted = set()


    def slc_init(self):
        log.info('SiamCiReceiverService.slc_init()')
        pass

    def slc_terminate(self):
        """
        Just reports the expect and accepted sets
        """
        log.info('SiamCiReceiverService.slc_terminate() ======= ')
        for e in self.expect:
            log.info('---- expect: "%s"' % (e))
        for a in self.accepted:
            log.info('---accepted: "%s"' % (a))


    def _get_publish_id(self, content, headers, msg):
        """
        Gets the publish_id from the headers or the content.
        Note: the java client puts the publish_id in the headers; we check there first.        
        If not in the headers, we check in the content; this is basically to support 
        python-side clients for testing purposes.
        
        @return: the publish ID; None if not found
        """
        publish_id = None
        if 'publish_id' in headers.keys():
            publish_id = headers['publish_id']
            log.info('_get_publish_id: publish_id = "'+publish_id+ '" (from headers)')
        elif 'publish_id' in content.keys():
            publish_id = content['publish_id']
            log.info('_get_publish_id: publish_id = "'+publish_id+ '" (from content)')
            
        return publish_id
       
    @defer.inlineCallbacks
    def op_expect(self, content, headers, msg):
        log.info('op_expect: ' +str(content))
        
        publish_id = yield self._get_publish_id(content, headers, msg)
        if publish_id:
            self.expect.add(publish_id)
        else:
            log.warn('op_expect: publish_id not given')
        
        yield self.reply_ok(msg, {'value' : "TODO-some-result"})


    @defer.inlineCallbacks
    def op_acceptResponse(self, content, headers, msg):
        log.info('op_acceptResponse: ' +str(headers))
#        
#        from IPython.Shell import IPShellEmbed
#        ipshell = IPShellEmbed()
#        ipshell()
#
        publish_id = yield self._get_publish_id(content, headers, msg)
        if publish_id:
            self.accepted.add(publish_id)
        else:
            log.warn('op_acceptResponse: publish_id not given')

        yield self.reply_ok(msg, {'value' : "TODO-some-result"})

    @defer.inlineCallbacks
    def op_checkExpected(self, content, headers, msg):
        """
        Checks that the expected publish_id's were received
        @return: a set with expected id's that have not been received
        """
        log.info('op_checkExpected: ' +str(headers))
        r = []
        for e in self.expect:
            if not e in self.accepted:
                r.append(e)
                
        yield self.reply_ok(msg, r)



class SiamCiReceiverServiceClient(ServiceClient):
    """
    This client is mainly for testing purposes within python. (The actual client
    of the service is the SIAM-CI adapter.)
    """
    def __init__(self, proc=None, **kwargs):
        if not 'targetname' in kwargs:
            kwargs['targetname'] = "siamci_receiver"
        ServiceClient.__init__(self, proc, **kwargs)

    @defer.inlineCallbacks
    def expect(self, publish_id):
        if publish_id is None:
            raise Exception("publish_id cannot be None")
        payload = { "publish_id" : publish_id }
        (content, headers, payload) = yield self.rpc_send('expect', payload)
        log.debug('expect service reply: ' + str(content))
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def acceptResponse(self, publish_id):
        """
        This method on the python side is mainly for testing purposes
        """
        if publish_id is None:
            raise Exception("publish_id cannot be None")
        payload = { "publish_id" : publish_id }
        (content, headers, payload) = yield self.rpc_send('acceptResponse', payload)
        log.debug('acceptResponse service reply: ' + str(content))
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def checkExpected(self):
        payload = {}
        (content, headers, payload) = yield self.rpc_send('checkExpected', payload)
        log.info('checkExpected service reply: ' + str(content))
        defer.returnValue(content)
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamCiReceiverService)

