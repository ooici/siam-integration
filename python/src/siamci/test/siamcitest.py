#!/usr/bin/env python
"""
@file siamci/test/siamcitest.py
@brief A base class for SIAM-CI test cases.
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from ion.test.iontest import IonTestCase

from twisted.internet import defer

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.receiver_service import SiamCiReceiverServiceClient

from os import getenv

from ion.core.object.object_utils import create_type_identifier

Command_type = create_type_identifier(object_id=20034, version=1)
ChannelParameterPair_type = create_type_identifier(object_id=20035, version=1)
SuccessFail_type = create_type_identifier(object_id=20036, version=1)


class SiamCiTestCase(IonTestCase):
    """
    A base class for SIAM-CI test cases.
    Handles the situation where the tests should be skipped (via defining
    the class variable 'skip' to some string; this variable is used by 
    twisted trial to skip the whole class).
    
    Besides some helper methods, subclasses should only refer to the 'pid' and 'port' 
    class variables defined in this class:
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
    

    def assertIsCommand(self, obj):
        self.assertEquals(Command_type, obj.MessageType)
    
    def assertIsChannelParameterPair(self, obj):
        self.assertEquals(ChannelParameterPair_type, obj.MessageType)
    
    def assertIsSuccessFail(self, obj):
        self.assertEquals(SuccessFail_type, obj.MessageType)
    

    @defer.inlineCallbacks
    def _start_receiver_service(self, receiver_service_name, timeout=2):
        """
        Starts an instance of SiamCiReceiverService with the given name.
        
        @param receiver_service_name: service name
        
        @param timeout: timeout for expected notifications to the service 
        
        @return: A new instance of SiamCiReceiverServiceClient associated
                 with the service.
        
        @note: the given name is also passed in the 'spawnargs' 
        (ie., ``'spawnargs':{ 'servicename':receiver_service_name }'' 
        to properly name the service; if not included, the default name in 
        SiamCiReceiverService.declare would be used.
        """
        services = [
            {'name':receiver_service_name,
             'module':'siamci.receiver_service',
             'class':'SiamCiReceiverService',
             'spawnargs':{ 'servicename':receiver_service_name }
            }
        ]
        sup = yield self._spawn_processes(services)
        svc_id = yield sup.get_child_id(receiver_service_name)
        receiver_client = SiamCiReceiverServiceClient(proc=sup, target=svc_id)

        yield receiver_client.setExpectedTimeout(timeout)
        
        defer.returnValue(receiver_client)
