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
import ion.util.procutils as pu

CONF = ioninit.config('startup.bootstrap-dx')

# Static definition of message queues
ion_messaging = ioninit.get_config('messaging_cfg', CONF)


class TestSiamCiAdapterProxyAsync(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container(sysname="siamci")

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
        
        services = [
                    {'name':'siamci_receiver','module':'ion.siamci.receiver_service','class':'SiamCiReceiverService'}
        ]
        self.sup = yield bootstrap.bootstrap(ion_messaging, services)
        self.svc_id = yield self.sup.get_child_id('siamci_receiver')
        self.rs_client = SiamCiReceiverServiceClient(proc=self.sup,target=self.svc_id)
    

    @defer.inlineCallbacks
    def tearDown(self):
        # Sleep for a bit here to allow AMQP messages to complete
        yield pu.asleep(0.5)
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_get_status_async(self):
        publish_id = "port=" + SiamCiTestCase.port
        yield self.rs_client.expect(publish_id);
        ret = yield self.siamci.get_status(publish_stream="siamci.siamci_receiver")

        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
        # check that all expected were received
        r = yield self.rs_client.checkExpected()
        self.assertEquals(len(r), 0)

