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
    
    """
    This is equal to 'instrument' per the instrument driver interface page: 
    https://confluence.oceanobservatories.org/display/syseng/CIAD+SA+SV+Instrument+Driver+Interface
    (accessed 2011-05-10).
    """
    INSTRUMENT = 'instrument'
    

class SiamDriverAnnouncement(DriverAnnouncement):
    """
    Add siam driver specific announcements here.
    """
    pass



