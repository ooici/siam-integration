#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/Siam_driver.py
@author Carlos Rueda
@brief Driver code for SIAM
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer  #, reactor



from ion.agents.instrumentagents.instrument_agent import InstrumentDriver
from ion.agents.instrumentagents.instrument_agent import InstrumentDriverClient
#from ion.agents.instrumentagents.instrument_agent import publish_msg_type

from ion.core.process.process import ProcessFactory

from ion.agents.instrumentagents.SiamCi_proxy import SiamCiAdapterProxy

from net.ooici.play.instr.instrument_defs_pb2 import Command, SuccessFail, OK, ERROR

       
class SiamInstrumentDriver(InstrumentDriver):
    """
    @todo: under construction
    """

    def __init__(self, *args, **kwargs):
        InstrumentDriver.__init__(self, *args, **kwargs)
        self.siamci = SiamCiAdapterProxy()


    @defer.inlineCallbacks
    def op_initialize(self, content, headers, msg):
        """
        @todo: Not implemented yet
        """
        log.debug('In SiamDriver op_initialize')
        result = True        
        yield self.reply_ok(msg, result)

    @defer.inlineCallbacks
    def op_get_status(self, content, headers, msg):
        log.debug('In SiamDriver op_get_status')
        
        response = self.siamci.get_status(content)
        log.debug("get_status: %s -> %s " % (str(content), str(response)))

        #
        # NOTE: ION works with carrot.backends.txamqplib.Message's:
        #    type(msg) --> <class 'carrot.backends.txamqplib.Message'>
        # 
        # We would in general reply with the response from the operation, which in this case is
        # of type --> net.ooici.play.instr.instrument_defs_pb2.SuccessFail
        # but this would raise an error like so:
        #  TypeError: can't serialize <net.ooici.play.instr.instrument_defs_pb2.SuccessFail object at 0x2b346c0>
        # so, just extract the non-gpb piece and reply with it:
        #
        result = response.result
        yield self.reply_ok(msg, result)

    @defer.inlineCallbacks
    def op_ping(self, content, headers, msg):
        log.debug('In SiamDriver op_ping')
        
        response = self.siamci.ping()
        yield self.reply_ok(msg, response)

    @defer.inlineCallbacks
    def op_list_ports(self, content, headers, msg):
        log.debug('In SiamDriver op_list_ports')
        
        response = self.siamci.list_ports()
        
        result = "ERROR"
        if response.result == OK:
            result = {}
            for it in response.item:
                result[it.pair.first] = it.pair.second
        
        log.debug('In SiamDriver op_list_ports --> ' +str(result))    
        yield self.reply_ok(msg, result)

    @defer.inlineCallbacks
    def op_get_last_sample(self, content, headers, msg):
        log.debug('In SiamDriver op_get_last_sample')
        
        response = self.siamci.get_last_sample(content)
        
        result = "ERROR"
        if response.result == OK:
            result = {}
            for it in response.item:
                result[it.pair.first] = it.pair.second
        
        log.debug('In SiamDriver op_get_last_sample --> ' +str(result))    
        yield self.reply_ok(msg, result)


class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    """
    
    @defer.inlineCallbacks
    def ping(self):
        log.debug("SiamInstrumentDriverClient ping ...")
        # 'dummy' arg as required by rpc_send
        (content, headers, message) = yield self.rpc_send('ping', 'dummy')
        defer.returnValue(content)

    @defer.inlineCallbacks
    def list_ports(self):
        log.debug("SiamInstrumentDriverClient list_ports ...")
        # 'dummy' arg as required by rpc_send
        (content, headers, message) = yield self.rpc_send('list_ports', 'dummy')
        defer.returnValue(content)

    @defer.inlineCallbacks
    def get_last_sample(self, port):
        log.debug("SiamInstrumentDriverClient get_last_sample port='%s' ..." % port)
        (content, headers, message) = yield self.rpc_send('get_last_sample', port)
        defer.returnValue(content)

# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
