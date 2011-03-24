#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/siamci/test/test_echo_service_in_java.py
      Based on ion/core/messaging/test_message_client.py
@author: Carlos Rueda
@summary: tests the interaction between python and java by making requests to
          an echo service in java, and verifying the response.
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from twisted.internet import defer

from ion.core import bootstrap

from ion.core.object import object_utils
from ion.core.messaging import message_client

from ion.agents.instrumentagents.siamci.test.siamcitest import SiamCiTestCase

Command_type = object_utils.create_type_identifier(object_id=20034, version=1)

class JavaEchoTest(SiamCiTestCase):
    
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()


    @defer.inlineCallbacks
    def test_send_message_instance1(self):
        """
        Adapted from test_send_message_instance() in ion/core/messaging/test_message_client.py
        but here with communication with an echo service in java.
        See IonSimpleEcho.java, SiamCiServerIonMsg.java in the SIAM-CI project.
        
        This test uses the support provided by IonTestCase, ie., by using self.test_sup.rpc_send(..),
        """
        
        mc = message_client.MessageClient(proc=self.test_sup)
        message = yield mc.create_instance(Command_type, MessageName='command sent message')
        message.command ='echo'
        arg = message.args.add()
        arg.channel = "myCh1"
        arg.parameter = "myParam1"
        
        pid = SiamCiTestCase.pid
        
        log.debug(show_message(message, "Sending command to " +pid))
        
        (response, headers, msg) = yield self.test_sup.rpc_send(pid, 'echo', message)
        
#        from IPython.Shell import IPShellEmbed
#        ipshell = IPShellEmbed()
#        ipshell()
        
        self.assertIsInstance(response, message_client.MessageInstance)
        
        self.assertEqual(Command_type, response.MessageObject.ObjectType)
        
        self.assertEqual(response.command, 'echo')

  
    @defer.inlineCallbacks
    def test_send_message_instance2(self):
        """
        Tests the RPC request/response without using the support provided by IonTestCase,
        ie., not using self.test_sup.rpc_send(..), but instead by creating a bootstrap.create_supervisor() 
        and using its rpc_send method directly.  
        This served as a basis to implement the SiamCiAdapterProxy.
        """
        
        p1 = yield bootstrap.create_supervisor()
        
        mc = message_client.MessageClient(proc=p1)
        message = yield mc.create_instance(Command_type, MessageName='command sent message')
        message.command ='echo'
        arg = message.args.add()
        arg.channel = "myCh1"
        arg.parameter = "myParam1"
        
        pid = SiamCiTestCase.pid

        (response, headers, msg) = yield p1.rpc_send(pid, 'echo', message)
        
        self.assertIsInstance(response, message_client.MessageInstance)
        
        self.assertEqual(Command_type, response.MessageObject.ObjectType)
        
        self.assertEqual(response.command, 'echo')
  
  
def show_message(msg, title="Message:"):
    prefix = "    | "
    contents = str(msg).strip().replace("\n", "\n"+prefix)
    return title+ "\n    " + str(type(msg)) + "\n" + prefix + contents
        