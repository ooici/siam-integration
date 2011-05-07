#!/usr/bin/env python

"""
@file siamci/test/test_siam_agent.py
@brief Test cases for the InstrumentAgent and InstrumentAgentClient classes
    using a SIAM driver.
    Initially based on test_SBE37_agent.py.
@author Carlos Rueda
"""

import uuid
import re
import os

from twisted.internet import defer
from ion.test.iontest import IonTestCase
from twisted.trial import unittest

import ion.util.ionlog
import ion.util.procutils as pu
from ion.core.exception import ReceivedError
import ion.agents.instrumentagents.instrument_agent as instrument_agent

from ion.agents.instrumentagents.instrument_constants import AgentCommand
from ion.agents.instrumentagents.instrument_constants import AgentEvent
from ion.agents.instrumentagents.instrument_constants import DriverChannel
from ion.agents.instrumentagents.instrument_constants import DriverCommand
from ion.agents.instrumentagents.instrument_constants import InstErrorCode

from siamci.test.siamcitest import SiamCiTestCase

log = ion.util.ionlog.getLogger(__name__)


class TestSiamAgent(SiamCiTestCase):

    # Increase the timeout so we can handle longer instrument interactions.
#    timeout = 180


    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()
        
        driver_name = 'SiamInstrumentDriver_' + SiamCiTestCase.port
        driver_client_name = 'SiamInstrumentDriverClient_' + SiamCiTestCase.port
        instr_agent_name = 'InstrumentAgent_' + SiamCiTestCase.port
       
        driver_config = {
            'pid':SiamCiTestCase.pid, 
            'port':SiamCiTestCase.port,
            'notify_agent':True
        }        
        agent_config = {}
        
        # Process description for the SiamInstrumentDriver driver.
        driver_desc = {
            'name':driver_name,
            'module':'siamci.siam_driver',
            'class':'SiamInstrumentDriver',
            'spawnargs':driver_config
        }

        # Process description for the SiamInstrumentDriverClient driver client.
        driver_client_desc = {
            'name':driver_client_name,
            'module':'siamci.siam_driver',
            'class':'SiamInstrumentDriverClient',
            'spawnargs':{}
        }

        # Spawnargs for the instrument agent.
        spawnargs = {
            'driver-desc':driver_desc,
            'client-desc':driver_client_desc,
            'driver-config':driver_config,
            'agent-config':agent_config
        }

        # Process description for the instrument agent.
        agent_desc = {
            'name':instr_agent_name,
            'module':'ion.agents.instrumentagents.instrument_agent',
            'class':'InstrumentAgent',
            'spawnargs':spawnargs
        }

        # Processes for the tests.
        processes = [
            agent_desc
        ]
        
        # Spawn agent and driver, create agent client.
        self.sup = yield self._spawn_processes(processes)
        self.svc_id = yield self.sup.get_child_id(instr_agent_name)
        self.ia_client = instrument_agent.InstrumentAgentClient(proc=self.sup,
                                                                target=self.svc_id)        
        

    @defer.inlineCallbacks
    def tearDown(self):
#        yield pu.asleep(1)
        yield self._stop_container()
   
   
         

    @defer.inlineCallbacks
    def _close_transaction(self, transaction_id):
        log.debug(" _close_transaction: transaction_id=" +str(transaction_id))
        reply = yield self.ia_client.end_transaction(transaction_id)
        success = reply['success']
        self.assert_(InstErrorCode.is_ok(success))
        
        
    @defer.inlineCallbacks
    def _test_001_initialize(self, close_transaction=True):
        # Begin an explicit transaction.
        reply = yield self.ia_client.start_transaction(0)
        success = reply['success']
        transaction_id = reply['transaction_id']
        self.assert_(InstErrorCode.is_ok(success))
        self.assertEqual(type(transaction_id),str)
        self.assertEqual(len(transaction_id),36)
    
        log.debug("transaction_id = " + str(transaction_id))
        
        # Issue state transition commands to bring the agent into
        # observatory mode.
        
        # Initialize the agent.
        cmd = [AgentCommand.TRANSITION,AgentEvent.INITIALIZE]
        reply = yield self.ia_client.execute_observatory(cmd,transaction_id) 
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_ok(success))
        
        if close_transaction:
            yield self._close_transaction(transaction_id)

        defer.returnValue(transaction_id)

        
    @defer.inlineCallbacks
    def test_002_go_active(self, close_transaction=True):
        
#        raise unittest.SkipTest("only partially implemented")
    
        transaction_id = yield self._test_001_initialize(False) 
        
        # Connect to the device.
        cmd = [AgentCommand.TRANSITION,AgentEvent.GO_ACTIVE]
        log.debug("About to call self.ia_client.execute_observatory(cmd,transaction_id)")
        reply = yield self.ia_client.execute_observatory(cmd,transaction_id) 
        success = reply['success']
        result = reply['result']

        # TODO: Not yet implemented (use SiamCiProxy)
        self.assert_(InstErrorCode.is_ok(success))

        if close_transaction:
            yield self._close_transaction(transaction_id)

        defer.returnValue(transaction_id)


    @defer.inlineCallbacks
    def _test_003_clear(self, close_transaction=True):
        
        raise unittest.SkipTest("only partially implemented")
    
        transaction_id = yield self.test_002_go_active(False) 
        
        # Clear the driver state.
        cmd = [AgentCommand.TRANSITION,AgentEvent.CLEAR]
        reply = yield self.ia_client.execute_observatory(cmd,transaction_id) 
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))
        
        if close_transaction:
            yield self._close_transaction(transaction_id)

        defer.returnValue(transaction_id)

        
    @defer.inlineCallbacks
    def _test_004_run(self, close_transaction=True):
        
        raise unittest.SkipTest("only partially implemented")
    
        transaction_id = yield self._test_003_clear(False) 
        
        
        # Start observatory mode.
        cmd = [AgentCommand.TRANSITION,AgentEvent.RUN]
        reply = yield self.ia_client.execute_observatory(cmd,transaction_id) 
        success = reply['success']
        result = reply['result']

        #print 'run reply:'
        #print reply

        self.assert_(InstErrorCode.is_ok(success))
        
        if close_transaction:
            yield self._close_transaction(transaction_id)

        defer.returnValue(transaction_id)
        
        

    @defer.inlineCallbacks
    def _test_999_execute_instrument(self):
        
#        raise unittest.SkipTest("only partially implemented")
    
        transaction_id = yield self._test_004_run() 
        
        
        # somewhat revised up to this point, but very preliminarily
        #########################################################################
        """
        
        # Get driver parameters.
        params = [('all','all')]
        reply = yield self.ia_client.get_device(params,transaction_id)
        success = reply['success']
        result = reply['result']

        # Strip off individual success vals to create a set params to
        # restore original config later.
        orig_config = dict(map(lambda x : (x[0],x[1][1]),result.items()))

        #print 'get device reply:'
        #print reply
        #print orig_config

        self.assert_(InstErrorCode.is_ok(success))

        # Set a few parameters. This will test the device set functions
        # and set up the driver for sampling commands. 
        params = {}
        params[(DriverChannel.INSTRUMENT,'NAVG')] = 1
        params[(DriverChannel.INSTRUMENT,'INTERVAL')] = 5
        params[(DriverChannel.INSTRUMENT,'OUTPUTSV')] = True
        params[(DriverChannel.INSTRUMENT,'OUTPUTSAL')] = True
        params[(DriverChannel.INSTRUMENT,'TXREALTIME')] = True
        params[(DriverChannel.INSTRUMENT,'STORETIME')] = True
        
        reply = yield self.ia_client.set_device(params,transaction_id)
        success = reply['success']
        result = reply['result']
        setparams = params
        
        #print 'set device reply:'
        #print reply

        self.assert_(InstErrorCode.is_ok(success))

        # Verify the set changes were made.
        params = [('all','all')]
        reply = yield self.ia_client.get_device(params,transaction_id)
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))

        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'NAVG')],
                         result[(DriverChannel.INSTRUMENT,'NAVG')][1])
        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'INTERVAL')],
                         result[(DriverChannel.INSTRUMENT,'INTERVAL')][1])
        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'OUTPUTSV')],
                         result[(DriverChannel.INSTRUMENT,'OUTPUTSV')][1])
        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'OUTPUTSAL')],
                         result[(DriverChannel.INSTRUMENT,'OUTPUTSAL')][1])
        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'TXREALTIME')],
                         result[(DriverChannel.INSTRUMENT,'TXREALTIME')][1])
        self.assertEqual(setparams[(DriverChannel.INSTRUMENT,'STORETIME')],
                         result[(DriverChannel.INSTRUMENT,'STORETIME')][1])
        
        #print 'acquisition parameters successfully set'
        
        # Acquire sample.
        chans = [DriverChannel.INSTRUMENT]
        cmd = [DriverCommand.ACQUIRE_SAMPLE]
        reply = yield self.ia_client.execute_device(chans,cmd,transaction_id)
        success = reply['success']
        result = reply['result']        

        #print 'acquisition result'
        #print result

        self.assert_(InstErrorCode.is_ok(success))
        self.assertIsInstance(result.get('temperature',None),float)
        self.assertIsInstance(result.get('salinity',None),float)
        self.assertIsInstance(result.get('sound velocity',None),float)
        self.assertIsInstance(result.get('pressure',None),float)
        self.assertIsInstance(result.get('conductivity',None),float)
        self.assertIsInstance(result.get('time',None),tuple)
        self.assertIsInstance(result.get('date',None),tuple)
        
        # Start autosampling.
        chans = [DriverChannel.INSTRUMENT]
        cmd = [DriverCommand.START_AUTO_SAMPLING]
        reply = yield self.ia_client.execute_device(chans,cmd,transaction_id)
        success = reply['success']
        result = reply['result']
        
        self.assert_(InstErrorCode.is_ok(success))

        #print 'autosampling started'
        
        # Wait for a few samples to arrive.
        yield pu.asleep(30)
        
        # Stop autosampling.
        chans = [DriverChannel.INSTRUMENT]
        cmd = [DriverCommand.STOP_AUTO_SAMPLING,'GETDATA']
        while True:
            reply = yield self.ia_client.execute_device(chans,cmd,
                                                        transaction_id)
            success = reply['success']
            result = reply['result']
            
            if InstErrorCode.is_ok(success):
                break
            
            elif success == InstErrorCode.TIMEOUT:
                pass
            
            else:
                self.fail('Stop autosample failed with error: '+str(success))
            
        #print 'autosample result'
        #print result
        
        self.assert_(InstErrorCode.is_ok(success))
        for sample in result:
            self.assertIsInstance(sample.get('temperature'),float)
            self.assertIsInstance(sample.get('salinity'),float)
            self.assertIsInstance(sample.get('pressure',None),float)
            self.assertIsInstance(sample.get('sound velocity',None),float)
            self.assertIsInstance(sample.get('conductivity',None),float)
            self.assertIsInstance(sample.get('time',None),tuple)
            self.assertIsInstance(sample.get('date',None),tuple)
        
        # Restore original configuration.
        reply = yield self.ia_client.set_device(orig_config,transaction_id)
        success = reply['success']
        result = reply['result']

        self.assert_(InstErrorCode.is_ok(success))

        # Verify the original configuration was restored.
        
        params = [('all','all')]
        reply = yield self.ia_client.get_device(params,transaction_id)
        success = reply['success']
        result = reply['result']

        # Strip off individual success vals to create a set params to
        # restore original config later.
        final_config = dict(map(lambda x : (x[0],x[1][1]),result.items()))

        self.assert_(InstErrorCode.is_ok(success))
        for (key,val) in orig_config.iteritems():
            if isinstance(val,float):
                self.assertAlmostEqual(val,final_config[key],4)
            else:
                self.assertEqual(val,final_config[key])

        #print 'original configuration restored'
                
        # Disconnect from device.
        cmd = [AgentCommand.TRANSITION,AgentEvent.GO_INACTIVE]
        reply = yield self.ia_client.execute_observatory(cmd,transaction_id) 
        success = reply['success']
        result = reply['result']
        self.assert_(InstErrorCode.is_ok(success))

        #print 'go inactive reply:'
        #print reply
                
        # Close the transaction.
        reply = yield self.ia_client.end_transaction(transaction_id)
        success = reply['success']
        self.assert_(InstErrorCode.is_ok(success))

        
        """
