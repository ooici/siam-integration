#!/usr/bin/env python

"""
@file siamci/receiver_service.py
@author Carlos Rueda
@brief Simple service to receive asynchronous responses from 
the SIAM-CI adapter in java
"""

import sys
import traceback

import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
import logging

from twisted.internet import defer

from ion.core.object import object_utils
from ion.core.process.process import ProcessFactory
from ion.core.process.service_process import ServiceProcess, ServiceClient

from ion.core.messaging.message_client import MessageClient

from ion.services.coi.resource_registry.resource_client import ResourceClient

import ion.util.procutils as pu


from ion.core.object.object_utils import create_type_identifier

Command_type = create_type_identifier(object_id=20034, version=1)
ChannelParameterPair_type = create_type_identifier(object_id=20035, version=1)
SuccessFail_type = create_type_identifier(object_id=20036, version=1)



class SiamCiReceiverService(ServiceProcess):
    """
    Simple service to receive asynchronous reponses from the SIAM-CI adapter
    in java
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
        log.debug('SiamCiReceiverService.slc_init()')

    def slc_terminate(self):
        """
        Just logs the expect and accepted sets
        """
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('SiamCiReceiverService.slc_terminate() ======= ')
            for e in self.expect:
                log.debug('---- expect: ' +str(e))
            for a in self.accepted:
                log.debug('---accepted: ' +str(a))


    def _get_publish_id(self, content, headers, msg):
        """
        Gets the publish_id from the headers or the content.
        Note: the java client puts the publish_id in the headers; we check there first.        
        If not found in the headers, we check in the content (if it is a dict); this is 
        basically to support python-side clients for testing purposes.
        
        @return: the publish ID; None if not found
        """
        publish_id = None
        if 'publish_id' in headers.keys():
            publish_id = headers['publish_id']
            log.debug('_get_publish_id: publish_id = "'+publish_id+ '" (from headers)')
        elif isinstance(content, dict) and 'publish_id' in content.keys():
            publish_id = content['publish_id']
            log.debug('_get_publish_id: publish_id = "'+publish_id+ '" (from content)')
            
        return publish_id
       
    
    @defer.inlineCallbacks
    def op_expect(self, content, headers, msg):
        log.debug('op_expect: ' +str(content))
        
        publish_id = self._get_publish_id(content, headers, msg)
        if publish_id:
            self.expect.add(publish_id)
        else:
            log.warn('op_expect: publish_id not given')
        
        yield self.reply_ok(msg, {'value' : "TODO-some-result"})


    @defer.inlineCallbacks
    def op_acceptResponse(self, content, headers, msg):
#        
        publish_id = self._get_publish_id(content, headers, msg)
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("op_acceptResponse: publish_id = " +str(publish_id))
        
        if publish_id:
            content = self._get_python_content(content)
            self.accepted[publish_id] = content
            yield self.reply_ok(msg, {'op_acceptResponse' : "OK: response for publish_id='" +str(publish_id)+ "' accepted"})
        else:
            log.warn('op_acceptResponse: publish_id not given')
            yield self.reply_err(msg, "op_acceptResponse : WARNING: publish_id not given")


    def _get_python_content(self, content):
        
#        from IPython.Shell import IPShellEmbed; (IPShellEmbed())()
        
        if isinstance(content, dict):
            # this is the case when op_acceptResponse is called from python code
            return content
        
        if SuccessFail_type == content.MessageType:
            obj = content.MessageObject
            # TODO complete conversion
            return {'result' : obj.result,
                    'item' : obj.item[0].str}
        
        elif ChannelParameterPair_type == content.MessageType:
            # FIXME do conversion
            return content
        
        elif Command_type == content.MessageType:
            # FIXME do conversion
            return content
        
        # TODO this should probably not happen
        return content
        
        
            

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
        
        log.debug('op_getExpected: ' +str(headers))
        
        timeout = None
        if 'timeout' in content.keys() and content['timeout']:
            timeout = content['timeout']   # content in this operation takes precedence
        else:
            timeout = self.checkTimeout    # use the overall timeout, if any
            
        # the total time in seconds we will wait while there is still expected id's
        remaining = timeout if timeout else 0.0
        
        expected = self._get_still_expected()
        while len(expected) > 0 and remaining > 0.0:
            yield pu.asleep(0.2);   # sleep for a moment
            remaining -= 0.2
            expected = self._get_still_expected()

        yield self.reply_ok(msg, expected)


    def _get_still_expected(self):
        expected = []
        for e in self.expect:
            if not e in self.accepted.keys():
                expected.append(e)
        return expected


    @defer.inlineCallbacks
    def op_getAccepted(self, content, headers, msg):
        """
        Returns the content received for a given publish_id; None if not received yet.
        """
        
        publish_id = self._get_publish_id(content, headers, msg)
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
        
        @param timeout: A timeout in seconds, to wait for the expected id's to be received.
                If given, this takes precedence for the operation. If not, 
                the timeout indicated in the last call to setExpectedTimeout, if any, will be used. 
                Otherwise, no timeout at all is used.
        
        @return: a list with expected id's that have not been received.
        """
        # note: we pass the timeout as it is directly to our own payload:
        payload = {'timeout':timeout}
        
        # but we need to explicitly indicate it for purposes of ION processing:
        if timeout is None:
            timeout = 15.0
            
        (content, headers, payload) = yield self.rpc_send('getExpected', payload, timeout=timeout)
        log.debug('getExpected service reply: ' + str(content))
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
        log.debug('getAccepted service reply: ' + str(content))
        defer.returnValue(content)
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamCiReceiverService)

