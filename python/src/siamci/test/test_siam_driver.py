#!/usr/bin/env python
"""
@file siamci/test/test_siam_driver.py
@brief This module has test cases to test out the SIAM driver.
   Initial version based on test_SBE49.py.
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from siamci.util.tcolor import red, blue
from siamci.test.siamcitest import SiamCiTestCase
from twisted.trial import unittest


from siamci.siam_driver import SiamInstrumentDriverClient
from siamci.siam_driver import SiamDriverState
from siamci.siam_driver import SiamDriverChannel
from siamci.siam_driver import SiamDriverCommand
    
from ion.agents.instrumentagents.instrument_fsm import InstrumentFSM
from ion.agents.instrumentagents.instrument_constants import InstErrorCode



class TestSiamInstrumentDriver(SiamCiTestCase):
    """
    @todo: complement the tests here with more rigourous checks. In general, at the moment 
    the focus of the tests are on the proxy. 
    """
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()
        
        self.driver_config = {
            'pid':SiamCiTestCase.pid, 
            'port':SiamCiTestCase.port
        }        
        

        driver_name = 'SiamInstrumentDriver_' + SiamCiTestCase.port
        
        services = [
            {'name': driver_name,
             'module':'siamci.siam_driver',
             'class':'SiamInstrumentDriver',
             'spawnargs':self.driver_config
             }
            ]

        self.sup = yield self._spawn_processes(services)
        
        self.driver_pid = yield self.sup.get_child_id(driver_name)

        self.driver_client = SiamInstrumentDriverClient(proc=self.sup,
                                                         target=self.driver_pid)

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()


    @defer.inlineCallbacks
    def _test_001_initialize(self):
        
        reply = yield self.driver_client.initialize()
        success = reply['success']
        result = reply['result']
        self.assert_(InstErrorCode.is_ok(success))
        
        current_state = yield self.driver_client.get_state()
        self.assertEqual(current_state, SiamDriverState.UNCONFIGURED)
        

    @defer.inlineCallbacks
    def _test_002_configure(self):

        yield self._test_001_initialize()
        
        params = self.driver_config

        # Configure the driver and verify.
        reply = yield self.driver_client.configure(params)        
        current_state = yield self.driver_client.get_state()
        success = reply['success']
        result = reply['result']
                
        self.assert_(InstErrorCode.is_ok(success))
        self.assertEqual(result,params)
        self.assertEqual(current_state, SiamDriverState.DISCONNECTED)

      
    @defer.inlineCallbacks
    def test_003_connect(self):

        yield self._test_002_configure();
        
        # Establish connection to device and verify.
        try:
            reply = yield self.driver_client.connect()
        except:
            self.fail('Could not connect to the device.')
            
        current_state = yield self.driver_client.get_state()
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))
        self.assertEqual(result,None)
        self.assertEqual(current_state, SiamDriverState.CONNECTED)

        
        # Dissolve the connection to the device.
        reply = yield self.driver_client.disconnect()
        current_state = yield self.driver_client.get_state()
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))
        self.assertEqual(result,None)
        self.assertEqual(current_state, SiamDriverState.DISCONNECTED)
       
        



    @defer.inlineCallbacks
    def _test_get_status(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        ret = yield self.driver_client.get_status("dummy arg")

    @defer.inlineCallbacks
    def _test_get_last_sample(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        ret = yield self.driver_client.get_last_sample()

    @defer.inlineCallbacks
    def _test_fetch_params_some(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        ret = yield self.driver_client.fetch_params(['startDelayMsec', 'wrongParam'])
        
    @defer.inlineCallbacks
    def _test_fetch_params_all(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        ret = yield self.driver_client.fetch_params([])
        


    @defer.inlineCallbacks
    def _test_fetch_set1(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        """Note: these tests are for successful interaction with driver regarding the
        set_params operation, but not necessarily that the parameter was actually set.
        
        @todo: A more complete test would involve the retrieval of the settable parameters,
        ideally along with an indication of valid (range of) values, and then set the 
        parameter with one such value.
        """
        
        """these happen to be present in the TestInstrument1 instrument """
        params = {'startDelayMsec':'600', 'packetSetSize' : '21'}
        ret = yield self.driver_client.set_params(params)

    @defer.inlineCallbacks
    def _test_fetch_set2(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        """these might not be valid, but we just check the operation completes"""
        params = {'baudrate':'19200', 'someWrongParam' : 'someValue'}
        ret = yield self.driver_client.set_params(params)


    @defer.inlineCallbacks
    def _test_execute(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        """
        Test the execute command to the Instrument Driver
        @todo: implement
        """



