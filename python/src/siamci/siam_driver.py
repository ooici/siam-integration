#!/usr/bin/env python

"""
@file siamci/siam_driver.py
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

from siamci.siamci_proxy import SiamCiAdapterProxy

from net.ooici.play.instr_driver_interface_pb2 import Command, SuccessFail, OK, ERROR

       
class SiamInstrumentDriver(InstrumentDriver):
    """
    Instrument driver interface to a SIAM enabled instrument.
    Operations are supported by the core class SiamCiAdapterProxy.
    
    @todo: do translation of GPBs to the native python structures proposed by the 
           Instrument Driver Interface page. At this point, I'm focusing my attention on
           the core needed functionality and using the GPBs directly.
           NOTE: The translation may probably be done only in SiamInstrumentDriverClient
           and no necessarily in SiamInstrumentDriver. 
    
    @todo: NOT all operations are instrument-specific; there are some that are associated
           with the SIAM node in general, for example, to retrieve all the instruments that
           are deployed on that node.  The TODO is about separating this more generic 
           functionality and use relevant ION mechanisms (eg., resource registries) for
           those purposes.
    """

    def __init__(self, *args, **kwargs):
        """
        Creates an instance of the driver.
        
        kwargs["spawnargs"]["pid"] will be used to connect to the SIAM-CI adapter service.
        
        This instance will be for a particular SIAM instrument if the 'port' parameter is given, which
        is obtained from kwargs["spawnargs"]["port"] if defined. Otherwise, only generic
        operations (like, list_ports) will be enabled.
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
    Instrument driver interface to a SIAM enabled instrument.
    Operations are supported by the core class SiamCiAdapterProxy.
    
    @todo: do translation of GPBs to the native python structures proposed by the 
           Instrument Driver Interface page. At this point, I'm focusing my attention on
           the core needed functionality and using the GPBs directly.
           NOTE: The translation may probably be done only in SiamInstrumentDriverClient
           and no necessarily in SiamInstrumentDriver. 
    
    @todo: NOT all operations are instrument-specific; there are some that are associated
           with the SIAM node in general, for example, to retrieve all the instruments that
           are deployed on that node.  The TODO is about separating this more generic 
           functionality and use relevant ION mechanisms (eg., resource registries) for
           those purposes.
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
        # 'dummy': an arg required by rpc_send
        (content, headers, message) = yield self.rpc_send('get_last_sample', 'dummy')
        
        # @todo: conversion to python type 
        
        defer.returnValue(content)
        
        
    @defer.inlineCallbacks
    def fetch_params(self, param_list):
        """
        @todo: we override the method in the superclass because that method expects the
        resulting content to be a dict: assert(isinstance(content, dict))
        In my current design, we work with the GPBs directly.
        """
                
        log.debug("SiamInstrumentDriverClient fetch_params " +str(param_list))
        (content, headers, message) = yield self.rpc_send('fetch_params',
                                                          param_list)
        
        # @todo: conversion to python type 
        
        defer.returnValue(content)

    @defer.inlineCallbacks
    def set_params(self, param_dict):
        """
        @todo: we override the method in the superclass because that method expects the
        resulting content to be a dict: assert(isinstance(content, dict))
        In my current design, we work with the GPBs directly.
        """
        
        log.debug("SiamInstrumentDriverClient set_params " +str(param_dict))
        (content, headers, message) = yield self.rpc_send('set_params',
                                                          param_dict)
        
        # @todo: conversion to python type 
        
        defer.returnValue(content)
        
    @defer.inlineCallbacks
    def get_status(self, param_dict):
        """
        @todo: we override the method in the superclass because that method expects
        to work with python types (list, tuple)
        In my current design, we work with the GPBs directly.
        """
        
        log.debug("SiamInstrumentDriverClient get_status " +str(param_dict))
        (content, headers, message) = yield self.rpc_send('get_status',
                                                          param_dict)
        
        # @todo: conversion to python type 
        
        defer.returnValue(content)
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
