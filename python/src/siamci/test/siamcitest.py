#!/usr/bin/env python
"""
@file siamci/test/siamcitest.py
@brief A base class for SIAM-CI test cases.
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from twisted.trial import unittest
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
    tests should (in principle) be performed. If undefined, the tests are skipped.
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
    If defined, the value of this variable is parsed for some properties and the 
    tests are run (unless skipped because of other conditions).
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
            d[k] = v[1:-1] if v[:1] == '"' else v
        if d.has_key('pid'): 
            pid = d['pid']
            del d['pid']
        if d.has_key('port'): 
            port = d['port']
            del d['port']
        if len(d) > 0:
            log.warn("unrecognized keys in SIAM_CI environment variable: " + str(d.keys()))
    
    log.debug("Using: pid = '" + str(pid) + "'  port = '" + str(port) + "'")
    
    
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
        """
        
        """
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


    def _check_skip(self):
        """
        Call this in a test_* method that may be subject to be skipped
        under certain runtime conditions.
        In this class, this method simply calls self._run_this_test(), 
        which checks the environment variable TEST_METHOD to decide whether 
        to raise unittest.SkipTest or not.
        """
        
        self._run_this_test()
        
        
    def _run_this_test(self, max_depth=50):
        """
        This method will raise a unittest.SkipTest only if the following 
        two conditions hold:
        - The environment variable TEST_METHOD is defined. This is 
          interpreted as a method name, wich can be a regular expression,
        AND
        - The method name does not match any of the names of the functions in 
          the calling stack of _run_this_test up to a given maximum depth. 
        
        So, this allows to only run a set of methods whose name conform
        to a given pattern, even across multiple modules/classes. For example:
           TEST_METHOD=".*set_params.*" bin/trial myrootmodule
        
        Inspecting the call stack allows to handle the case where the test  
        method that should be run makes calls to other test methods that
        should otherwise be skipped. (Accomplishing this behavior using a 
        decorator plus some form of instrumenting the method would be 
        rather intricate.)
        
        @param max_depth: max depth to examine the calling stack, by default 50.
               This should not be too small 
        """
        
        test_method = getenv('TEST_METHOD', None)
        if test_method is None:
            return
        
        import inspect
        import re
        
        prog = re.compile(test_method)
        stack = inspect.stack()
        depth = min(max_depth, len(stack))
        skip = True
        i = 1
        while skip and i < depth:
            funcname = stack[i][3]
            skip = not prog.match(funcname)
            i += 1
                
        if skip:
            raise unittest.SkipTest('skipped per environment variable %s="%s"'\
                                     % ("TEST_METHOD", test_method))


class TestCheckSkip(SiamCiTestCase):
    """
    A simple demo of the _run_this_test functionality which allows to
    select the test methods using the TEST_METHOD enviroment variable.
    
    Running the tests in this class:
    
    - Run all tests:
        bin/trial siamci.test.siamcitest
        
    - Run only test_foo:
        TEST_METHOD=".*foo" bin/trial siamci.test.siamcitest
        
    - Run only test_baz:
        TEST_METHOD=".*baz" bin/trial siamci.test.siamcitest
      Note that test_baz calls test_foo, but since test_baz is in
      the calling stack of test_foo, no skip exception is raised
      within test_foo.
    """
    
    # just to disable other checks by SiamCiTestCase regarding
    # integration tests
    skip = None
    

    def test_foo(self):
        self._check_skip()
        return True

    def test_baz(self):
        self._check_skip()
        self.assertTrue(self.test_foo())
