#!/usr/bin/env python
"""
@file siamci/test/test_siamci.py
@brief This module has test cases to test out the SIAM-CI proxy class
@author Carlos Rueda
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.test.siamcitest import SiamCiTestCase
from net.ooici.play.instr_driver_interface_pb2 import OK, ERROR

#from twisted.trial import unittest
from os import getenv


class TestSiamCiAdapterProxy(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

        self.siamci = SiamCiAdapterProxy(SiamCiTestCase.pid, SiamCiTestCase.port)
        yield self.siamci.start()
                

    @defer.inlineCallbacks
    def tearDown(self):
        yield self.siamci.stop()
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_ping(self):
        ret = yield self.siamci.ping()
        self.assertTrue(ret)

    @defer.inlineCallbacks
    def test_list_ports(self):
        ret = yield self.siamci.list_ports()
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)

    @defer.inlineCallbacks
    def test_get_status(self):
        ret = yield self.siamci.get_status()

    @defer.inlineCallbacks
    def test_get_last_sample(self):
        ret = yield self.siamci.get_last_sample()
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
    @defer.inlineCallbacks
    def test_fetch_params_some_good(self):
        """fetch specific list of parameters"""
        ret = yield self.siamci.fetch_params(['startDelayMsec'])
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
    @defer.inlineCallbacks
    def test_fetch_params_some_wrong(self):
        """fetch specific list of parameters"""
        ret = yield self.siamci.fetch_params(['startDelayMsec', 'WRONG_PARAM'])
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, ERROR)
        
    @defer.inlineCallbacks
    def test_fetch_params_all(self):
        """fetch all parameters"""
        ret = yield self.siamci.fetch_params()
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
    @defer.inlineCallbacks
    def test_set_params_good(self):
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000' })
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, OK)
        
    @defer.inlineCallbacks
    def test_set_params_wrong(self):
        ret = yield self.siamci.set_params({'startDelayMsec' : '1000'
                                            , 'WRONG_PARAM' : 'fooVal'
                                          })
        self.assertIsSuccessFail(ret)
        self.assertEquals(ret.result, ERROR)
        
