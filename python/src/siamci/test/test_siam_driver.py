#!/usr/bin/env python
"""
@file siamci/test/test_siam_driver.py
@brief This module has test cases to test out the SIAM driver.
   Initial version based on test_SBE49.py and then on test_SBE37
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from twisted.trial import unittest

from siamci.test.siamcitest import SiamCiTestCase
from siamci.util.tcolor import red, blue
from siamci.siam_driver import SiamInstrumentDriverClient
from siamci.siamci_constants import SiamDriverState
from siamci.siamci_constants import SiamDriverChannel
from siamci.siamci_constants import SiamDriverCommand
    
from ion.agents.instrumentagents.instrument_fsm import InstrumentFSM
from ion.agents.instrumentagents.instrument_constants import InstErrorCode

import random
import ion.util.procutils as pu


class TestSiamInstrumentDriver(SiamCiTestCase):
    
    # Increase timeout for Trial tests
    timeout = 120
    
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname="siamci")
        
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
    def test_005_get_params_all(self):
        """
        - connect
        - get with [(SiamDriverChannel.INSTRUMENT,'all')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [(SiamDriverChannel.INSTRUMENT,'all')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_ok(success))  

        # verify all returned params are for SiamDriverChannel.INSTRUMENT and are OK (value doesn't matter)
        for key in result:
            ch, pr = key
            self.assertEqual(SiamDriverChannel.INSTRUMENT, ch)
            successParam, val = result[key]
            self.assert_(InstErrorCode.is_ok(successParam))
        
        
        yield self.__disconnect()
        
        

    @defer.inlineCallbacks
    def test_005_get_params_subset_good(self):
        """
        - connect
        - get with [(SiamDriverChannel.INSTRUMENT,'startDelayMsec'), (SiamDriverChannel.INSTRUMENT,'diagnosticSampleInterval')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [(SiamDriverChannel.INSTRUMENT,'startDelayMsec'), (SiamDriverChannel.INSTRUMENT,'diagnosticSampleInterval')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_ok(success))  
        
        # verify all returned params are for SiamDriverChannel.INSTRUMENT and are OK (value doesn't matter)
        for key in result:
            ch, pr = key
            self.assertEqual(SiamDriverChannel.INSTRUMENT, ch)
            successParam, val = result[key]
            self.assert_(InstErrorCode.is_ok(successParam))
        

        yield self.__disconnect()
        
        
    @defer.inlineCallbacks
    def test_005_get_params_subset_good_and_bad(self):
        """
        - connect
        - get with [(SiamDriverChannel.INSTRUMENT,'startDelayMsec'), (SiamDriverChannel.INSTRUMENT,'INVALID_param')]
        - disconnect
        """
        
        yield self.__connect()
        
        params = [(SiamDriverChannel.INSTRUMENT,'startDelayMsec'), (SiamDriverChannel.INSTRUMENT,'INVALID_param')]
        reply = yield self.driver_client.get(params)
        success = reply['success']
        result = reply['result']      
        self.assert_(InstErrorCode.is_error(success))  
        
        #
        # TODO: Note, the adapter returns a single Error when any of the requested
        # params is invalid, ie., does not yet provide discriminated errors.
        # So the result here is None.
#        successParam, val = result[(SiamDriverChannel.INSTRUMENT,'startDelayMsec')]
#        self.assert_(InstErrorCode.is_ok(successParam))
#        
#        successParam, val = result[(SiamDriverChannel.INSTRUMENT,'INVALID_param')]
#        self.assert_(InstErrorCode.is_error(successParam))
        

        yield self.__disconnect()
        
        
    @defer.inlineCallbacks
    def test_005_get_params_specific_channels(self):
        raise unittest.SkipTest('Not yet implemented')


    @defer.inlineCallbacks
    def test_006_set_params_valid(self):
        """
        - connect
        - set a couple of valid parameters
        - disconnect
        
        @todo: A more complete test would involve the retrieval of the settable parameters,
        ideally along with an indication of valid (range of) values, and then set the 
        parameter with one such value.
        """
        
        yield self.__connect()
        
        channel = SiamDriverChannel.INSTRUMENT
        """these parameters happen to be present in the TestInstrument1 instrument """
        params = {(channel,'startDelayMsec'):'600', (channel,'packetSetSize'):'21'}
        reply = yield self.driver_client.set(params)
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_ok(success))
        
        assert(isinstance(result, dict))   
        self.assertEqual(result.get("startDelayMsec", None), '600')
        self.assertEqual(result.get("packetSetSize", None), '21')
        
        yield self.__disconnect()
        

    @defer.inlineCallbacks
    def test_006_set_params_invalid(self):
        """
        - connect
        - set some invalid parameters (we just check the operation completes)
        - disconnect
        """
        
        yield self.__connect()
        
        channel = SiamDriverChannel.INSTRUMENT
        params = {(channel,'baudrate'):'19200', (channel,'someWrongParam'):'someValue'}
        reply = yield self.driver_client.set(params)
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_error(success))
        
        yield self.__disconnect()
        

    @defer.inlineCallbacks
    def __connect_and_get_channels(self):
        """
        @return: the reported channels
        """
        
        yield self.__connect()
        
        channels = [SiamDriverChannel.INSTRUMENT]
        command = [SiamDriverCommand.GET_CHANNELS]
        timeout = 10
        reply = yield self.driver_client.execute(channels,command,timeout)
            
        success = reply['success']
        result = reply['result']
        
        log.debug("test_009_get_channels result =" +str(result))
        
        self.assert_(InstErrorCode.is_ok(success))
        
        assert(isinstance(result, (tuple,list)))    
        
        current_state = yield self.driver_client.get_state()
        self.assertEqual(current_state, SiamDriverState.CONNECTED)
        
        defer.returnValue(result)
        
        
    @defer.inlineCallbacks
    def test_007_get_channels(self):
        """
        - connect 
        - get channels reported by instrument
        - disconnect
        """
        
        channels = yield self.__connect_and_get_channels()
        
        log.debug("test_009_get_channels channels =" +str(channels))
        
        yield self.__disconnect()
        
        
    @defer.inlineCallbacks
    def test_008_get_last_sample(self):
        """
        - connect
        - execute with SiamDriverChannel.INSTRUMENT and SiamDriverCommand.GET_LAST_SAMPLE
        - disconnect
        """
        
        yield self.__connect()
        
        channels = [SiamDriverChannel.INSTRUMENT]
        command = [SiamDriverCommand.GET_LAST_SAMPLE]
        timeout = 10
        reply = yield self.driver_client.execute(channels,command,timeout)
            
        success = reply['success']
        result = reply['result']
        
        log.debug("test_010_get_last_sample result =" +str(result))
        
        self.assert_(InstErrorCode.is_ok(success))
        
        assert(isinstance(result, dict))    
        
        # NOTE: the following specific channels are for the TEstInstrument used
        # during development (which cannot be asserted in a general setting, of course) 
#        self.assert_('sequenceNumber' in result)
#        self.assert_('val' in result)
        
        current_state = yield self.driver_client.get_state()
        self.assertEqual(current_state, SiamDriverState.CONNECTED)
        

        yield self.__disconnect()
        
        

    @defer.inlineCallbacks
    def test_009_acquisition_start_wait_stop(self):
        """
        - connect and get channels reported by instrument
        - select a reported channel randomly
        - start receiver service (in lieu of InstrumentAgent)
        - execute START_AUTO_SAMPLING with selected channel
        - wait for a few seconds
        - execute STOP_AUTO_SAMPLING with selected channel
        - disconnect
        """
        
        #
        # connect and get channels
        #
        channels = yield self.__connect_and_get_channels()
        if len(channels) == 0:
            raise unittest.SkipTest('No reported channels from the instrument at port ' +str(SiamCiTestCase.port))
        
        #
        # randomly select a reported channel:
        #
        channel = channels[random.randint(0, len(channels) - 1)]
        
        #
        # start receiver service and set the publish_stream
        #
        receiver_service_name = 'test_siam_driver_receiver' 
        receiver_client = yield self._start_receiver_service(receiver_service_name)
        publish_stream = "siamci." + receiver_service_name
        reply = yield self.driver_client.set_publish_stream(publish_stream)
        success = reply['success']
        result = reply['result']
        self.assert_(InstErrorCode.is_ok(success))
        
        #
        # tell receiver to expect related publication
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "data_acquisition;port=" + SiamCiTestCase.port + ";channel=" +channel
        
        # prepare to receive result:
        yield receiver_client.expect(publish_id);
        
        #
        # execute START_AUTO_SAMPLING
        # NOTE SiamDriverChannel.INSTRUMENT is not handled. We use the concrete
        # channel selected above
        #
        channels = [channel]
        command = [SiamDriverCommand.START_AUTO_SAMPLING]
        timeout = 20
        reply = yield self.driver_client.execute(channels,command,timeout)
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_ok(success))
        
        assert(isinstance(result, dict))    
        self.assertEqual(result.get("channel", None), channel)
        self.assertEqual(result.get("publish_stream", None), publish_stream)
        
        #
        # Wait for a few samples to be notified to the receiver service
        #
        yield pu.asleep(20)

        #
        # check that we actually received data
        #
        expected = yield receiver_client.getExpected()
        self.assertEquals(len(expected), 0)

        #
        # execute STOP_AUTO_SAMPLING
        # 
        channels = [channel]
        command = [SiamDriverCommand.STOP_AUTO_SAMPLING]
        reply = yield self.driver_client.execute(channels, command)
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_ok(success))
        

        #
        # disconnect
        #
        yield self.__disconnect()
        
        
