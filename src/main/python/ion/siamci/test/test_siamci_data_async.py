#!/usr/bin/env python

"""

        DATA TESTS - TO BE INCORPORATED IN test_siamci_async.py later


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

from twisted.trial import unittest


from ion.core import ioninit
#from ion.core import bootstrap

#CONF = ioninit.config('startup.bootstrap-dx') #-

# Static definition of message queues
#ion_messaging = ioninit.get_config('messaging_cfg', CONF) #-

# Note the ``'spawnargs':{ 'servicename':receiver_service_name }'' included below to properly name
# the service; if not included, the default name in SiamCiReceiverService.declare would be used.
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
        sup = yield self._spawn_processes(services) #=
#        sup = yield bootstrap.bootstrap(ion_messaging, services) #-
        svc_id = yield sup.get_child_id(receiver_service_name)
        self.client = SiamCiReceiverServiceClient(proc=sup,target=svc_id)
        # set a default expected timeout:
        self.client.setExpectedTimeout(2)
    

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_execute_StartAcquisition_async(self):
#        raise unittest.SkipTest('Not implemented yet')


        # @todo: capture channel from some parameter?
        channel = "val"
        
        #
        # @todo: more robust assignment of publish IDs
        # In this case should include the particular channel, not indicated yet either!
        #
        publish_id = "execute_StartAcquisition;port=" + SiamCiTestCase.port + ";channel=" +channel
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        ret = yield self.siamci.execute_StartAcquisition(channel=channel,
                                                         publish_stream="siamci." + receiver_service_name)
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.getExpected(timeout=30)
        self.assertEquals(len(expected), 0)
        
        # actual response should indicate OK: 
        response = yield self.client.getAccepted(publish_id)
        self.assertIsSuccessFail(response)
        self.assertEquals(response.result, OK)
