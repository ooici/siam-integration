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
#import ion.util.procutils as pu

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
    

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_get_status_async(self):
        publish_id = "port=" + SiamCiTestCase.port
        
        # prepare to receive result:
        yield self.client.expect(publish_id);
        
        # make request:
        ret = yield self.siamci.get_status(publish_stream="siamci." + receiver_service_name)

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        expected = yield self.client.checkExpected()
        self.assertEquals(len(expected), 0)

