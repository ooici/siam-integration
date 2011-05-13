#!/usr/bin/env python

"""
@file siamci/siamci_constants.py
@author Carlos Rueda
@brief Some constants associated with the SIAM-CI integration
"""

from ion.agents.instrumentagents.instrument_constants import BaseEnum
from ion.agents.instrumentagents.instrument_constants import DriverCommand
from ion.agents.instrumentagents.instrument_constants import DriverState
from ion.agents.instrumentagents.instrument_constants import DriverEvent
from ion.agents.instrumentagents.instrument_constants import DriverAnnouncement
from ion.agents.instrumentagents.instrument_constants import DriverChannel
from ion.agents.instrumentagents.instrument_constants import InstErrorCode

"""
@TODO: Some of the classes below extend corresponding classes in the
       general CI instrument framework. Sure, that's the idea. However,
       there is still a need to examine and align these various 
       components; for example, there are probably certain elements 
       (states, events) that are not necessarily applicable to a SIAM driver(?).
"""

# Device states.
class SiamDriverState(DriverState):
    """
    Add siam driver specific states here.
    """
    pass

# Device events.
class SiamDriverEvent(DriverEvent):
    """
    Add siam driver specific events here.
    """
    pass

# Device commands.
class SiamDriverCommand(DriverCommand):
    """
    Add siam driver specific commands here.
    """
    GET_CHANNELS = 'DRIVER_CMD_GET_CHANNELS'
    GET_LAST_SAMPLE = 'DRIVER_CMD_GET_LAST_SAMPLE'


# Device channels / transducers.
class SiamDriverChannel(BaseEnum):
    """
    SIAM instrument driver channels.
    As the base SIAM instrument driver is still at a generic level, 
    this only defines INSTRUMENT, not any particular channels.
    @TODO: mechanism to actually maintain the concrete channels
    belonging to a concrete SIAM instrument driver.
    """
    
    INSTRUMENT = DriverChannel.INSTRUMENT
    

class SiamDriverAnnouncement(DriverAnnouncement):
    """
    Add siam driver specific announcements here.
    """
    pass



