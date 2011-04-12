#!/usr/bin/env python
"""
@file ion/siamci/test/test_siamci.py
@brief This module has test cases to test out the SIAM-CI proxy class
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from ion.siamci.siamci_proxy import SiamCiAdapterProxy
from ion.siamci.test.siamcitest import SiamCiTestCase
from ion.siamci.receiver_service import SiamCiReceiverServiceClient
from net.ooici.play.instr_driver_interface_pb2 import OK, ERROR

from ion.core import ioninit
from ion.core import bootstrap

CONF = ioninit.config('startup.bootstrap-dx')

# Static definition of message queues
ion_messaging = ioninit.get_config('messaging_cfg', CONF)

# Note the ``'spawnargs':{ 'servicename':receiver_service_name }'' below to properly name
# the service; otherwise the default name in SiamCiReceiverService.declare would be used.
receiver_service_name = 'siamci_receiver_test_async'

class TestSiamCiAdapterProxyAsync(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname="siamci")

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
        
        services = [
            {'name':receiver_service_name,
             'module':'ion.siamci.receiver_service',
             'class':'SiamCiReceiverService',
             'spawnargs':{ 'servicename':receiver_service_name }
            }
        ]
        sup = yield bootstrap.bootstrap(ion_messaging, services)
        svc_id = yield sup.get_child_id(receiver_service_name)
        self.client = SiamCiReceiverServiceClient(proc=sup,target=svc_id)
        # set a deafult expected timeout:
        self.client.setExpectedTimeout(1)
    

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_list_ports_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "list_ports;"
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.list_ports(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)


    @defer.inlineCallbacks
    def test_get_status_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "get_status;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.get_status(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)


    @defer.inlineCallbacks
    def test_get_last_sample_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "get_last_sample;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.get_last_sample(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)
        

    @defer.inlineCallbacks
    def test_fetch_params_some_good_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(['startDelayMsec'], 
                                             publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)

        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)
        
        
    @defer.inlineCallbacks
    def test_fetch_params_some_wrong_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(['startDelayMsec', 'WRONG_PARAM'], 
                                             publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)  # NOTE the immediate reply should be OK ...
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # ... but the actual response should indicate ERROR: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, ERROR)

        
    @defer.inlineCallbacks
    def test_fetch_params_all_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "fetch_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.fetch_params(publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)

    @defer.inlineCallbacks
    def test_set_params_good_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "set_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000' }, 
                                           publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)

    @defer.inlineCallbacks
    def test_set_params_wrong_async(self):
        #
        # @todo: more robust assignment of publish IDs
        #
        publish_id = "set_params;port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000'
                                            , 'WRONG_PARAM' : 'fooVal'
                                          },
                                          publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, ERROR)
