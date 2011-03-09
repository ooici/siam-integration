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

        self.siamci = SiamCiAdapterProxy("testPort")
        yield self.siamci.start()
                

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_ping(self):
        ret = yield self.siamci.ping()
        self.assertTrue(ret)

    @defer.inlineCallbacks
    def test_list_ports(self):
        ret = yield self.siamci.list_ports()

    @defer.inlineCallbacks
    def test_get_status(self):
        ret = yield self.siamci.get_status()

    @defer.inlineCallbacks
    def test_get_last_sample(self):
        ret = yield self.siamci.get_last_sample()
        self.assertIsInstance(ret, dict)
        
    @defer.inlineCallbacks
    def test_fetch_params_some(self):
        """fetch specific list of parameters"""
        ret = yield self.siamci.fetch_params(['startDelayMsec', 'wrongParam'])
        self.assertIsInstance(ret, dict)
        
    @defer.inlineCallbacks
    def test_fetch_params_all(self):
        """fetch all parameters"""
        ret = yield self.siamci.fetch_params()
        self.assertIsInstance(ret, dict)
        
    @defer.inlineCallbacks
    def test_set_params(self):
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000'
                                            , 'wrongParam' : 'fooVal'
                                          })
        self.assertIsInstance(ret, (dict, str))
        
