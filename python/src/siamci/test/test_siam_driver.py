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
    def test_001_initialize(self):
        
        reply = yield self.driver_client.initialize()
        success = reply['success']
        result = reply['result']
        self.assert_(InstErrorCode.is_ok(success))
        
        current_state = yield self.driver_client.get_state()
        self.assertEqual(current_state, SiamDriverState.UNCONFIGURED)
        

    @defer.inlineCallbacks
    def test_002_configure(self):

        yield self.test_001_initialize()
        
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
    def __connect(self):

        yield self.test_002_configure();
        
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
        
        
    @defer.inlineCallbacks
    def __disconnect(self):
        # Dissolve the connection to the device.
        reply = yield self.driver_client.disconnect()
        current_state = yield self.driver_client.get_state()
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))
        self.assertEqual(result,None)
        self.assertEqual(current_state, SiamDriverState.DISCONNECTED)
       
        
      
    @defer.inlineCallbacks
    def test_003_connect(self):
        """
        - connect
        - disconnect
        """
        
        yield self.__connect()
        yield self.__disconnect()
        



    @defer.inlineCallbacks
    def test_004_get_status(self):
        """
        - connect
        - get_status
        - disconnect
        """
        
        yield self.__connect()
        
        # @todo: Only the all-all params is handled; handle other possible params
        params = [('all','all')]
        reply = yield self.driver_client.get_status(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_ok(success))  
        
        yield self.__disconnect()
        
        
        
    @defer.inlineCallbacks
    def test_005_get_instrument_params_all(self):
        """
        - connect
        - get with [('instrument','all')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [('instrument','all')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_ok(success))  

        # verify all returned params are for 'instrument' and are OK (value doesn't matter)
        for key in result:
            ch, pr = key
            self.assertEqual('instrument', ch)
            successParam, val = result[key]
            self.assert_(InstErrorCode.is_ok(successParam))
        
#            print blue(pr + "  successParam = " +str(successParam) + "  value = " + str(val))
        
        yield self.__disconnect()
        
        

    @defer.inlineCallbacks
    def test_006_get_instrument_params_subset_good(self):
        """
        - connect
        - get with [('instrument','startDelayMsec'), ('instrument','diagnosticSampleInterval')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [('instrument','startDelayMsec'), ('instrument','diagnosticSampleInterval')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_ok(success))  
        
        # verify all returned params are for 'instrument' and are OK (value doesn't matter)
        for key in result:
            ch, pr = key
            self.assertEqual('instrument', ch)
            successParam, val = result[key]
            self.assert_(InstErrorCode.is_ok(successParam))
        
#            print blue(pr + "  successParam = " +str(successParam) + "  value = " + str(val))

        yield self.__disconnect()
        
        
    @defer.inlineCallbacks
    def test_007_get_instrument_params_subset_good_and_bad(self):
        """
        - connect
        - get with [('instrument','startDelayMsec'), ('instrument','INVALID_param')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [('instrument','startDelayMsec'), ('instrument','INVALID_param')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_error(success))  
        
        #
        # TODO: Note, the adapter returns a single Error when any of the requested
        # params is invalid, ie., does not yet provide discriminated errors.
        # So the result here is None.
#        successParam, val = result[('instrument','startDelayMsec')]
#        self.assert_(InstErrorCode.is_ok(successParam))
#        
#        successParam, val = result[('instrument','INVALID_param')]
#        self.assert_(InstErrorCode.is_error(successParam))
        

        yield self.__disconnect()
        
        
    @defer.inlineCallbacks
    def test_008_get_instrument_params_specific_channels(self):
        raise unittest.SkipTest('Not yet implemented')


    @defer.inlineCallbacks
    def _test_get_last_sample(self):
        raise unittest.SkipTest('UNDER DEVELOPMENT')
        ret = yield self.driver_client.get_last_sample()


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



