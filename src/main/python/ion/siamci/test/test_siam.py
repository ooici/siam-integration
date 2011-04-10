#!/usr/bin/env python
"""
@file ion/siamci/test/test_siam.py
@brief This module has test cases to test out the SIAM driver.
@author Carlos Rueda (using test_SBE49.py as a basis)
@see ion.agents.instrumentagents.test.test_instrument
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from ion.siamci.siam_driver import SiamInstrumentDriverClient
from ion.siamci.test.siamcitest import SiamCiTestCase

#from ion.services.dm.distribution.pubsub_service import PubSubClient

#import ion.util.procutils as pu
#from ion.resources.dm_resource_descriptions import PubSubTopicResource, SubscriptionResource

from twisted.trial import unittest

class TestSiamInstrumentDriver(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

        services = [
            # not publishing data yet
#            {'name':'pubsub_registry','module':'ion.services.dm.distribution.pubsub_registry','class':'DataPubSubRegistryService'},
#            {'name':'pubsub_service','module':'ion.services.dm.distribution.pubsub_service','class':'DataPubsubService'},

            {'name':'SiamInstrumentDriver_' + SiamCiTestCase.port,
             'module':'ion.siamci.siam_driver',
             'class':'SiamInstrumentDriver',
             'spawnargs':{ 'pid':SiamCiTestCase.pid, 'port':SiamCiTestCase.port }
             }
            ]

        self.sup = yield self._spawn_processes(services)
        
        self.driver_pid = yield self.sup.get_child_id('SiamInstrumentDriver_testPort')
        log.debug("Driver pid %s" % (self.driver_pid))

        self.driver_client = SiamInstrumentDriverClient(proc=self.sup,
                                                         target=self.driver_pid)

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_initialize(self):
        raise unittest.SkipTest('Not implemented yet')
        result = yield self.driver_client.initialize("dummy-arg")
        self.assertTrue(result)

    @defer.inlineCallbacks
    def test_get_status(self):
        ret = yield self.driver_client.get_status("dummy arg")

    @defer.inlineCallbacks
    def test_ping(self):
        ret = yield self.driver_client.ping()
        self.assertTrue(ret)

    @defer.inlineCallbacks
    def test_list_ports(self):
        ret = yield self.driver_client.list_ports()

    @defer.inlineCallbacks
    def test_get_last_sample(self):
        ret = yield self.driver_client.get_last_sample()
        self.assertIsInstance(ret, dict)

    @defer.inlineCallbacks
    def test_fetch_params_some(self):
        ret = yield self.driver_client.fetch_params(['startDelayMsec', 'wrongParam'])
        self.assertIsInstance(ret, dict)
        
    @defer.inlineCallbacks
    def test_fetch_params_all(self):
        ret = yield self.driver_client.fetch_params([])
        self.assertIsInstance(ret, dict)
        


    @defer.inlineCallbacks
    def test_fetch_set1(self):
        """Note: these tests are for successful interaction with driver regarding the
        set_params operation, but not necessarily that the parameter was actually set.
        
        @todo: A more complete test would involve the retrieval of the settable parameters,
        ideally along with an indication of valid (range of) values, and then set the 
        parameter with one such value.
        """
        
        """these happen to be present in the TestInstrument1 instrument """
        params = {'startDelayMsec':'600', 'packetSetSize' : '21'}
        ret = yield self.driver_client.set_params(params)
        self.assertIsInstance(ret, (dict, str))

    @defer.inlineCallbacks
    def test_fetch_set2(self):
        """these might not be valid, but we just check the operation completes"""
        params = {'baudrate':'19200', 'someWrongParam' : 'someValue'}
        ret = yield self.driver_client.set_params(params)
        self.assertIsInstance(ret, (dict, str))


    @defer.inlineCallbacks
    def test_execute(self):
        raise unittest.SkipTest('Not implemented yet')
        """
        Test the execute command to the Instrument Driver
        @todo: implement
        """


#    @defer.inlineCallbacks
#    def test_sample(self):
#        raise unittest.SkipTest('Needs new PubSub services')
#        """
#        @todo: implement
#        """
#        yield self.driver_client.initialize('some arg')
#
#        cmd1 = ['ds', 'now']
#        yield self.driver_client.execute(cmd1)
#
#        yield pu.asleep(1)
#
#        yield self.driver_client.disconnect(['some arg'])

