#!/usr/bin/env python

"""
@file ion/siamci/siam_driver.py
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

from ion.siamci.siamci_proxy import SiamCiAdapterProxy

from net.ooici.play.instr_driver_interface_pb2 import Command, SuccessFail, OK, ERROR

       
class SiamInstrumentDriver(InstrumentDriver):
    """
    @todo: under construction
    """

    def __init__(self, *args, **kwargs):
        """
        Creates an instance of the driver.
        
        kwargs["spawnargs"]["pid"] will be used to connect to the SIAM-CI adapter service.
        
        This instance will be for a particular SIAM instrument if the 'port' parameter is given, which
        is obtained from kwargs["spawnargs"]["port"] if defined.
        """
        InstrumentDriver.__init__(self, *args, **kwargs)
        pid = None
        port = None
        if kwargs["spawnargs"]:
            args = kwargs["spawnargs"];
            if args.has_key('pid'): pid = args['pid']
            if args.has_key('port'): port = args['port']
            
        log.debug("SiamInstrumentDriver __init__: pid = '" +str(pid)+ "' port = '" +str(port)+ "'")
        self.siamci = SiamCiAdapterProxy(pid, port)


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
        
        response = yield self.siamci.get_status()
        log.debug("get_status: %s -> %s " % (str(content), str(response)))

        result = response.result
        yield self.reply_ok(msg, result)

    @defer.inlineCallbacks
    def op_ping(self, content, headers, msg):
        log.debug('In SiamDriver op_ping')
        
        response = yield self.siamci.ping()
        yield self.reply_ok(msg, response)

    @defer.inlineCallbacks
    def op_list_ports(self, content, headers, msg):
        log.debug('In SiamDriver op_list_ports')
        
        response = yield self.siamci.list_ports()
        
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
        
        response = yield self.siamci.get_last_sample()
        log.debug('In SiamDriver op_get_last_sample --> ' +str(response))    
        yield self.reply_ok(msg, response)


    @defer.inlineCallbacks
    def op_fetch_params(self, content, headers, msg):
        log.debug('In SiamDriver op_fetch_params')
        
        response = yield self.siamci.fetch_params(content)
        log.debug('In SiamDriver op_fetch_params --> ' +str(response))    
        yield self.reply_ok(msg, response)

    @defer.inlineCallbacks
    def op_set_params(self, content, headers, msg):
        log.debug('In SiamDriver op_set_params')
        
        response = yield self.siamci.fetch_params(content)
        log.debug('In SiamDriver op_set_params --> ' +str(response))    
        yield self.reply_ok(msg, response)





class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    """
    
    @defer.inlineCallbacks
    def ping(self):
        log.debug("SiamInstrumentDriverClient ping ...")
        # 'dummy': arg required by rpc_send
        (content, headers, message) = yield self.rpc_send('ping', 'dummy')
        defer.returnValue(content)

    @defer.inlineCallbacks
    def list_ports(self):
        log.debug("SiamInstrumentDriverClient list_ports ...")
        # 'dummy': arg required by rpc_send
        (content, headers, message) = yield self.rpc_send('list_ports', 'dummy')
        defer.returnValue(content)

    @defer.inlineCallbacks
    def get_last_sample(self):
        log.debug("SiamInstrumentDriverClient get_last_sample ...")
        # 'dummy': arg required by rpc_send
        (content, headers, message) = yield self.rpc_send('get_last_sample', 'dummy')
        defer.returnValue(content)

# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
