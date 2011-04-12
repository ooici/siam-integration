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

import ion.util.procutils as pu


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
        
        self.checkTimeout = None
        
        # the set of id's given via op_expect:
        self.expect = set()
        
        # the (id, content) pairs accepted via op_acceptResponse:
        self.accepted = {}


    def slc_init(self):
        log.info('SiamCiReceiverService.slc_init()')
        pass

    def slc_terminate(self):
        """
        Just logs the expect and accepted sets
        """
        log.info('SiamCiReceiverService.slc_terminate() ======= ')
        for e in self.expect:
            log.info('---- expect: ' +str(e))
        for a in self.accepted:
            log.info('---accepted: ' +str(a))


    def _get_publish_id(self, content, headers, msg):
        """
        Gets the publish_id from the headers or the content.
        Note: the java client puts the publish_id in the headers; we check there first.        
        If not found in the headers, we check in the content; this is basically to support 
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
            self.accepted[publish_id] = content
            yield self.reply_ok(msg, {'op_acceptResponse' : "OK: response for publish_id='" +str(publish_id)+ "' accepted"})
        else:
            log.warn('op_acceptResponse: publish_id not given')
            yield self.reply_err(msg, "op_acceptResponse : WARNING: publish_id not given")


    @defer.inlineCallbacks
    def op_setExpectedTimeout(self, content, headers, msg):
        """
        Sets the timeout for the op_getExpected operation. There in no
        timeout by default.
        """
        
        if 'timeout' in content.keys() and content['timeout']:
            self.checkTimeout = content['timeout']
            yield self.reply_ok(msg, {'checkTimeout' : self.checkTimeout})
        else:
            yield self.reply_err(msg, "Missing 'timeout' for op_setExpectedTimeout operation")
            
    @defer.inlineCallbacks
    def op_getExpected(self, content, headers, msg):
        """
        Returns a list with expected id's that have not been received.
        
        If the content includes a 'timeout' parameter, this is used to allow time for expected 
        responses to be received. If not, then the timeout indicated in the last call to 
        op_setExpectedTimeout, if any, will be used. Otherwise, no timeout at all is used.
        
        @return: a list with expected id's that have not been received
        """
        
        timeout = None
        if 'timeout' in content.keys() and content['timeout']:
            timeout = content['timeout']   # content takes precedence
        else:
            timeout = self.checkTimeout
            
        if timeout:
            """@todo: Needs to be done in a more robust, elegant way. For the moment,
            sleep for the timeout time."""
            log.debug("sleeping for " +str(timeout))
            yield pu.asleep(timeout);

        
        log.info('op_getExpected: ' +str(headers))
        r = []
        for e in self.expect:
            if not e in self.accepted.keys():
                r.append(e)
                
        yield self.reply_ok(msg, r)


    @defer.inlineCallbacks
    def op_getAccepted(self, content, headers, msg):
        """
        Returns the content received for a given publish_id; None if not received yet.
        """
        publish_id = yield self._get_publish_id(content, headers, msg)
        if publish_id:
            if publish_id in self.accepted:
                yield self.reply_ok(msg, self.accepted[publish_id])
            else:
                yield self.reply_ok(msg, None)
        else:
            log.warn('op_getAccepted: publish_id not given')
            yield self.reply_err(msg, 'op_getAccepted: publish_id not given')
        
        

class SiamCiReceiverServiceClient(ServiceClient):
    """
    This client for SiamCiReceiverService.
    """
    def __init__(self, proc=None, **kwargs):
        if not 'targetname' in kwargs:
            kwargs['targetname'] = "siamci_receiver"
        ServiceClient.__init__(self, proc, **kwargs)

        
    @defer.inlineCallbacks
    def expect(self, publish_id):
        """
        Adds an element to the list of expected publish_id's
        """
        if publish_id is None:
            raise Exception("publish_id cannot be None")
        payload = { "publish_id" : publish_id }
        (content, headers, payload) = yield self.rpc_send('expect', payload)
        log.debug('expect service reply: ' + str(content))
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def acceptResponse(self, publish_id):
        """
        @note: This method on the python side is mainly for testing purposes. The actual
        request to the service will come from the SIAM-CI adapter in java.
        """
        if publish_id is None:
            raise Exception("publish_id cannot be None")
        payload = { "publish_id" : publish_id }
        (content, headers, payload) = yield self.rpc_send('acceptResponse', payload)
        log.debug('acceptResponse service reply: ' + str(content))
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def setExpectedTimeout(self, timeout):
        """
        Sets the timeout for the getExpected operation. There in no
        timeout by default.
        """
        if timeout is None:
            raise Exception("timeout cannot be None")

        payload = { "timeout" : timeout }
        (content, headers, payload) = yield self.rpc_send('setExpectedTimeout', payload)
        log.debug('setExpectedTimeout reply: ' + str(content))
        defer.returnValue(content)

    @defer.inlineCallbacks
    def getExpected(self, timeout=None):
        """
        Returns a list with expected id's that have not been received.
        
        @param timeout: If given, this takes precedence for the operation. If not, 
        the timeout indicated in the last call to setExpectedTimeout, if any, will be used. 
        Otherwise, no timeout at all is used.
        
        @return: a list with expected id's that have not been received.
        """
        payload = {'timeout':timeout}
        (content, headers, payload) = yield self.rpc_send('getExpected', payload)
        log.info('getExpected service reply: ' + str(content))
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def getAccepted(self, publish_id):
        """
        Returns the content received for a given publish_id; None if not received yet.
        """
        if publish_id is None:
            raise Exception("publish_id cannot be None")
        payload = {'publish_id':publish_id}
        (content, headers, payload) = yield self.rpc_send('getAccepted', payload)
        log.info('getAccepted service reply: ' + str(content))
        defer.returnValue(content)
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamCiReceiverService)

