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


       
class SiamInstrumentDriver(InstrumentDriver):
    """
    @todo: under construction
    """

    def __init__(self, *args, **kwargs):
        InstrumentDriver.__init__(self, *args, **kwargs)
        self.siamci = SiamCiAdapterProxy()


    @defer.inlineCallbacks
    def op_initialize(self, content, headers, msg):
        log.debug('In SiamDriver op_initialize')

        yield self.reply_ok(msg, content)

    @defer.inlineCallbacks
    def op_get_status(self, content, headers, msg):
        log.debug('In SiamDriver op_get_status')
        
        content = self.siamci.get_status(content, headers, msg)

        yield self.reply_ok(msg, content)



class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    """



# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
