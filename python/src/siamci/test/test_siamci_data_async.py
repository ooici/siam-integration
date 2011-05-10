#!/usr/bin/env python

"""
@file siamci/test/test_siamci_data_async.py
@brief Data aqcuisition related test cases
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.test.siamcitest import SiamCiTestCase

from net.ooici.play.instr_driver_interface_pb2 import OK, ERROR

from twisted.trial import unittest

from ion.core import ioninit



class TestSiamCiAdapterProxyDataAsync(SiamCiTestCase):
    
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname="siamci")

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
        
    

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_execute_StartAcquisition_async(self):

        receiver_service_name = 'test_execute_StartAcquisition_async'
        receiver_client = yield self._start_receiver_service(receiver_service_name)
        
        # @todo: capture channel from some parameter?
        channel = "val"
        
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "data_acquisition;port=" + SiamCiTestCase.port + ";channel=" +channel
        
        # prepare to receive result:
        yield receiver_client.expect(publish_id);
        
        ret = yield self.siamci.execute_StartAcquisition(channel=channel,
                                                         publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield receiver_client.getExpected(timeout=30)
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield receiver_client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)


    @defer.inlineCallbacks
    def test_execute_StartAndStopAcquisition_async(self):
        raise unittest.SkipTest('Not implemented yet')

        # TODO
        # this will startAcquisition, wait for a few samples, then
        # request stopAcquisition, and then verify that this request is
        # successful 

