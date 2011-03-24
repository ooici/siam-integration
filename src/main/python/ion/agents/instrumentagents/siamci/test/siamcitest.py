#!/usr/bin/env python
"""
@file ion/agents/instrumentagents/siamci/test/siamcitest.py
@brief A base class for SIAM-CI test cases.
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from ion.test.iontest import IonTestCase

from ion.agents.instrumentagents.siamci.SiamCi_proxy import SiamCiAdapterProxy

from os import getenv


class SiamCiTestCase(IonTestCase):
    """
    A base class for SIAM-CI test cases.
    Handles the situation where the tests should be skipped (via defining
    the class variable 'skip' to some string; this variable is used by 
    twisted trial to skip the whole class).
    
    Subclasses should only refer to the 'pid' and 'port' class variables defined
    in this class:
        pid:  The queue associated with the SIAM-CI adapter service (in java)
              Note that this queue is assumed to be associated with the message broker
              used by the running capability container.
        port: the SIAM instrument port
    
    Currently, the environment variable SIAM_CI is used to determine that the 
    tests should be performed. If undefined, the tests are skipped.
    If defined, the value of this variable is parsed for some properties.
    Examples:
      - With explicit values:     
            SIAM_CI="pid=SIAM-CI, port=testPort"
      - Using default values (values shown in the explicit value example above):     
            SIAM_CI="-"
    
    The properties are:      
        pid:  The queue associated with the SIAM-CI adapter service (in java)
              Note that this queue is assumed to be associated with the message broker
              used by the running capability container.
        port: the SIAM instrument port
        
    See https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status
    """
    SIAM_CI = getenv("SIAM_CI", None)
    
    skip = None if SIAM_CI else '''SIAM_CI environment variable not set. 
    If defined, the value of this variable is parsed for some properties and the tests are run.
    Examples:
      - With explicit values:     
            SIAM_CI="pid=SIAM-CI, port=testPort"
      - Using default values (values shown in the explicit value example above):     
            SIAM_CI="-"
    
    The properties are:      
        pid:  The queue associated with the SIAM-CI adapter service (in java)
              Note that this queue is assumed to be associated with the message broker
              used by the running capability container.
        port: the SIAM instrument port
        
    See https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status
    '''
    
    """The pid for the RPC requests."""
    pid = "SIAM-CI"
    
    """the specific instrument to test"""
    port = "testPort"
    
    if SIAM_CI and SIAM_CI != "-":
        # parse the string (note that values can be quoted)
        import re
        d = {}
        for k, v in re.compile('([^ =]+) *= *("[^"]*"|[^ ,]*)').findall(SIAM_CI):
            d[k]= v[1:-1] if v[:1]=='"' else v
        if d.has_key('pid'): 
            pid = d['pid']
            del d['pid']
        if d.has_key('port'): 
            port = d['port']
            del d['port']
        if len(d) > 0:
            log.warn("unrecognized keys in SIAM_CI environment variable: " +str(d.keys()))
    
    log.debug("Using: pid = '" +str(pid)+ "'  port = '" +str(port)+ "'")
    

    
