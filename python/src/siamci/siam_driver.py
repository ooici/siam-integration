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


try:
    from ion.agents.instrumentagents.instrument_constants import InstErrorCode
except:
    # these pieces provisionally copied from 'develop' while release > 0.4.8 is available
    class BaseEnum(object):
        @classmethod
        def list(cls):
            return [getattr(cls,attr) for attr in dir(cls) if \
                not callable(getattr(cls,attr)) and not attr.startswith('__')]
        @classmethod
        def has(cls,item):
            return item in cls.list()
        
    class InstErrorCode(BaseEnum):
        OK = 'OK'
        @classmethod
        def is_ok(cls,x):
            return x == cls.OK
        @classmethod
        def is_error(cls,x):
            return (cls.has(list(x)) and x != cls.OK)


       
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
        
        self.spawn_args.get('pid') will be used to connect to the SIAM-CI adapter service.
        
        This instance will be for a particular SIAM instrument if self.spawn_args.get('port')
        is not None. Otherwise, only generic operations (like, list_ports) will be enabled.
        """
        
        InstrumentDriver.__init__(self, *args, **kwargs)
        pid = self.spawn_args.get('pid')
        port = self.spawn_args.get('port')
            
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
    def op_configure(self, content, headers, msg):
        """
        Configure the driver to establish communication with the device.
        @param content a dict containing required and optional
            configuration parameters.
        @retval A reply message dict {'success':success,'result':content}.
        """
        
        """
            Using SBE37_dirver as a basis for this operation
            
            @TODO: just starting implementation
        """
        
        assert(isinstance(content,dict)), 'Expected dict content.'
        params = content.get('params',None)
        assert(isinstance(params,dict)), 'Expected dict params.'
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass

        # Set up the reply message and validate the configuration parameters.
        # Reply with the error message if the parameters not valid.
        reply = {'success':None,'result':params}
        reply['success'] = self._validate_configuration(params)
        
        log.debug("xxxxxxxxxxx reply['success'] = " + str(reply['success']))

        if InstErrorCode.is_error(reply['success']):
            yield self.reply_ok(msg,reply)
            return
        
        # Fire EVENT_CONFIGURE with the validated configuration parameters.
        # Set the error message if the event is not handled in the current
        # state.
#        reply['success'] = self.fsm.on_event(SBE37Event.CONFIGURE,params)
        reply['success'] = "listo"

        yield self.reply_ok(msg, reply)

        
    def _validate_configuration(self,params):
        """
        Validate the configuration is valid.
        @param params a dict of named configuration parameters
            {'param1':val1,...,'paramN':valN}.
        @retval A success/fail value.
        """
        
        """
            Using SBE37_dirver as a basis for this operation
            
            @TODO: just starting implementation
        """
        
        
#        # Get required parameters.
#        _ipport = params.get('ipport',None)
#        _ipaddr = params.get('ipaddr',None)
#
#        # fail if missing a required parameter.
#        if not _ipport or not _ipaddr:
#            return InstErrorCode.REQUIRED_PARAMETER
#
#        # Validate port number.
#        if not isinstance(_ipport,int) or _ipport <0 or _ipport > 65535:
#            return InstErrorCode.INVALID_PARAM_VALUE
#        
#        # Validate ip address.
#        # TODO: Add logic to veirfy string format.
#        if not isinstance(_ipaddr,str): 
#            return InstErrorCode.INVALID_PARAM_VALUE

        return InstErrorCode.OK

        

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
