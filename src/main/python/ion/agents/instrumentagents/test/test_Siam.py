#!/usr/bin/env python
"""
@file ion/agents/instrumentagents/test/test_Siam.py
@brief This module has test cases to test out the SIAM driver.
@author Carlos Rueda (using test_SBE49.py as a basis)
@see ion.agents.instrumentagents.test.test_instrument
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer

from ion.test.iontest import IonTestCase

from ion.agents.instrumentagents.Siam_driver import SiamInstrumentDriverClient

#from ion.services.dm.distribution.pubsub_service import PubSubClient

#import ion.util.procutils as pu
#from ion.resources.dm_resource_descriptions import PubSubTopicResource, SubscriptionResource

from twisted.trial import unittest

class TestSiam(IonTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

        services = [
            # not publishing data yet
#            {'name':'pubsub_registry','module':'ion.services.dm.distribution.pubsub_registry','class':'DataPubSubRegistryService'},
#            {'name':'pubsub_service','module':'ion.services.dm.distribution.pubsub_service','class':'DataPubsubService'},

            {'name':'Siam_Driver','module':'ion.agents.instrumentagents.Siam_driver','class':'SiamInstrumentDriver'
                # no need: ,'spawnargs':{'ipport':self.SimulatorPort}
             }
            ]

        self.sup = yield self._spawn_processes(services)
        
        self.driver_pid = yield self.sup.get_child_id('Siam_Driver')
        log.debug("Driver pid %s" % (self.driver_pid))

        self.driver_client = SiamInstrumentDriverClient(proc=self.sup,
                                                         target=self.driver_pid)

    @defer.inlineCallbacks
    def tearDown(self):
#        yield self.simulator.stop()

        yield self._stop_container()


    @defer.inlineCallbacks
    def test_initialize(self):
        result = yield self.driver_client.initialize("dummy-arg")
        self.assertTrue(result)

    @defer.inlineCallbacks
    def test_get_status(self):
        """
        @todo: implement
        """
        yield self.driver_client.get_status("testPort")

    @defer.inlineCallbacks
    def test_ping(self):
        ret = yield self.driver_client.ping()
        self.assertTrue(ret)

    @defer.inlineCallbacks
    def test_list_ports(self):
        ret = yield self.driver_client.list_ports()

    @defer.inlineCallbacks
    def test_get_last_sample(self):
        ret = yield self.driver_client.get_last_sample("testPort")


#    @defer.inlineCallbacks
#    def test_fetch_set(self):
#        raise unittest.SkipTest('Not implemented yet')
#        """
#        @todo: implement
#        """


#    @defer.inlineCallbacks
#    def test_execute(self):
#        raise unittest.SkipTest('Needs new PubSub services')
#        """
#        Test the execute command to the Instrument Driver
#        @todo: implement
#        """


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

