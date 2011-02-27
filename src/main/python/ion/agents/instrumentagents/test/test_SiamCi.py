#!/usr/bin/env python
"""
@file ion/agents/instrumentagents/test/test_SiamCi.py
@brief This module has test cases to test out the SIAM-CI proxy class
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from ion.agents.instrumentagents.SiamCi_proxy import SiamCiAdapterProxy

#from twisted.trial import unittest


class TestSiamCi(IonTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

        self.siamci = SiamCiAdapterProxy()
                

    @defer.inlineCallbacks
    def tearDown(self):
#        yield self.simulator.stop()

        yield self._stop_container()


    @defer.inlineCallbacks
    def test_ping(self):
        """
        @todo: implement
        """
        ret = yield self.siamci.ping()
        self.assertTrue(ret)

