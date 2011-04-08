#!/usr/bin/env python

"""
@file ion/siamci/test/test_receiver_service.py
@test ion.siam.receiver_service
@author Carlos Rueda
"""

from twisted.internet import defer

from ion.test.iontest import IonTestCase
import ion.util.ionlog
from ion.core import ioninit
from ion.core import bootstrap

from ion.siamci.receiver_service import SiamCiReceiverServiceClient

CONF = ioninit.config('startup.bootstrap-dx')

# Static definition of message queues
ion_messaging = ioninit.get_config('messaging_cfg', CONF)


class SiamCiReceiverServiceTest(IonTestCase):

    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_1(self):
        services = [
            {'name':'siamci_receiver1','module':'ion.siamci.receiver_service','class':'SiamCiReceiverService'}
        ]

        sup = yield bootstrap.bootstrap(ion_messaging, services)

        client = SiamCiReceiverServiceClient(proc=sup)

        yield client.acceptResponse()

                

