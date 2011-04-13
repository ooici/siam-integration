#!/usr/bin/env python

"""
@file ion/siamci/test/test_receiver_service.py
@test ion.siam.receiver_service
@brief Basic tests of SiamCiReceiverService
@author Carlos Rueda
"""

from twisted.internet import defer

from ion.test.iontest import IonTestCase
import ion.util.ionlog
from ion.core import ioninit
#from ion.core import bootstrap #-

from ion.siamci.receiver_service import SiamCiReceiverServiceClient

#CONF = ioninit.config('startup.bootstrap-dx') #-

# Static definition of message queues
#ion_messaging = ioninit.get_config('messaging_cfg', CONF) #-

# Note the ``'spawnargs':{ 'servicename':receiver_service_name }'' included below to properly name
# the service; if not included, the default name in SiamCiReceiverService.declare would be used.
receiver_service_name = 'siamci_receiver_test'

class SiamCiReceiverServiceTest(IonTestCase):
    """
    Basic tests of SiamCiReceiverService.
    """

    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()
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
        
        
    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_expect_1(self):
        publish_id = "some_publish_id"
        yield self.client.expect(publish_id)
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 1)
        self.assertTrue(publish_id in expected)

    @defer.inlineCallbacks
    def test_expect_accept_1(self):
        publish_id = "some_publish_id"
        yield self.client.expect(publish_id)
        yield self.client.acceptResponse(publish_id)
        expected = yield self.client.getExpected()
        self.assertEquals(len(expected), 0)

                

