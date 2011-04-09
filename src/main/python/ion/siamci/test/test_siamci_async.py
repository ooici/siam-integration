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

#from twisted.trial import unittest
from os import getenv

from ion.core import ioninit
from ion.core import bootstrap

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
        yield bootstrap.bootstrap(ion_messaging, services)
                

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_get_status_async(self):
        ret = yield self.siamci.get_status_async()

