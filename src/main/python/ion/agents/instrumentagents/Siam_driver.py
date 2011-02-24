#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/Siam_driver.py
@author Carlos Rueda (initially based on SBE49_dirver.py)
@brief Driver code for SIAM
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer, reactor


from ion.agents.instrumentagents.instrument_connection import InstrumentConnection
from twisted.internet.protocol import ClientCreator

from collections import deque

from ion.agents.instrumentagents.instrument_agent import InstrumentDriver
from ion.agents.instrumentagents.instrument_agent import InstrumentDriverClient, publish_msg_type
#from ion.agents.instrumentagents.instrument_agent import publish_msg_type

from ion.core.process.process import ProcessFactory

       
class SiamInstrumentDriver(InstrumentDriver):
    """
    @todo: under construction
    """

    def __init__(self, *args, **kwargs):
        InstrumentDriver.__init__(self, *args, **kwargs)


    @defer.inlineCallbacks
    def op_initialize(self, content, headers, msg):
        log.debug('In SiamDriver op_initialize')

        yield self.reply_ok(msg, content)

    @defer.inlineCallbacks
    def op_get_status(self, content, headers, msg):
        log.debug('In SiamDriver op_get_status')

        yield self.reply_ok(msg, content)



class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    """



# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
