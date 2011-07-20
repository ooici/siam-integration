#!/usr/bin/env python
"""
@file siamci/test/test_siamci_async.py
@brief This module has test cases to test out the SIAM-CI proxy class
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.test.siamcitest import SiamCiTestCase
from siamci.receiver_service import SiamCiReceiverServiceClient
from net.ooici.play.instr_driver_interface_pb2 import OK, ERROR

from ion.core import ioninit


receiver_service_name = 'siamci_receiver_test_async'

class TestSiamCiAdapterProxyAsync(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname="siamci")

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
        
        self.receiver_client = yield self._start_receiver_service(receiver_service_name)
        

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_list_ports_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "list_ports;"
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.list_ports(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)

        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)


    @defer.inlineCallbacks
    def test_get_status_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "get_status;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id)
        
        # make request:
        ret = yield self.siamci.get_status(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)


    @defer.inlineCallbacks
    def test_get_last_sample_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "get_last_sample;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.get_last_sample(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)

        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)
        

    @defer.inlineCallbacks
    def test_get_channels_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "get_channels;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.get_channels(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)

        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)
        

    @defer.inlineCallbacks
    def test_fetch_params_some_good_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(['startDelayMsec'], 
                                             publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)

        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)
        
        
    @defer.inlineCallbacks
    def test_fetch_params_some_wrong_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(['startDelayMsec', 'WRONG_PARAM'], 
                                             publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)  # NOTE the immediate reply should be OK ...
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # ... but the actual response should indicate ERROR: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], ERROR)
        self.assertTrue(response.get('item') != None)
        

        
    @defer.inlineCallbacks
    def test_fetch_params_all_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)


    @defer.inlineCallbacks
    def test_set_params_good_async_timeout_30(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "set_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000' }, 
                                           publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected(timeout=30)
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], OK)
        self.assertTrue(response.get('item') != None)

        

    @defer.inlineCallbacks
    def test_set_params_wrong_async(self):
        self._check_skip()

        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "set_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.receiver_client.expect(publish_id);
        
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000'
                                            , 'WRONG_PARAM' : 'fooVal'
                                          },
                                          publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.receiver_client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate ERROR: 
        response = yield self.receiver_client.getAccepted(publish_id)
        
        self.assertTrue(isinstance(response, dict))
        self.assertEquals(response['result'], ERROR)
        self.assertTrue(response.get('item') != None)
        
