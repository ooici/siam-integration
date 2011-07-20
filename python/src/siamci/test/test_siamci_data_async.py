#!/usr/bin/env python

"""
@file siamci/test/test_siamci_data_async.py
@brief Data aqcuisition related test cases directly on the SiamCiAdapterProxy
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from twisted.trial import unittest

import ion.util.procutils as pu

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.test.siamcitest import SiamCiTestCase

from net.ooici.play.instr_driver_interface_pb2 import OK, ERROR


sysname = "siamci"

class TestSiamCiAdapterProxyDataAsync(SiamCiTestCase):
    
    # increase timeout for Trial tests
    timeout = 120
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname=sysname)

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
        
    

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_acquisition_start_verify_data(self, receiver_service_name='test_acquisition_start_verify_data'):
        """
        - start receiver service and set expected publish id
        - start acquisition
        - get expected elements with some timeout
        - verify all exptected were received.
        
        Note that this test does not stop the acquisition. Unless this is explicitly stopped 
        by a caller (see next test), the receiver service will shutdown during tearDown, but
        also note that the SIAM-CI service will also eventually stop sending the notifications
        because they won't be delivered successfully anymore.
        """
        self._check_skip()

        receiver_client = yield self._start_receiver_service(receiver_service_name)
        
        # @todo: capture channel from some parameter?
        channel = "val"
        
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "data_acquisition;port=" + SiamCiTestCase.port + ";channel=" + channel
        
        # prepare to receive result:
        yield receiver_client.expect(publish_id);
        
        ret = yield self.siamci.execute_StartAcquisition(channel=channel,
                                                         publish_stream=sysname + "." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield receiver_client.getExpected(timeout=30)
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)
        
        defer.returnValue((receiver_client, channel, publish_id))


    @defer.inlineCallbacks
    def test_acquisition_start_wait_stop(self):
        """
        - start acquisition by calling test_acquisition_start_verify_data
        - wait for a few seconds
        - stop acquisition
        """
        self._check_skip()

        receiver_service_name='test_acquisition_start_wait_stop'
        
        # start acquisition
        (receiver_client, channel, publish_id) = \
            yield self.test_acquisition_start_verify_data(receiver_service_name)
        
        # wait for a few samples to be notified to the receiver service
        yield pu.asleep(20)
        
        # stop acquisition
        ret = yield self.siamci.execute_StopAcquisition(channel=channel,
                                                         publish_stream=sysname + "." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        

