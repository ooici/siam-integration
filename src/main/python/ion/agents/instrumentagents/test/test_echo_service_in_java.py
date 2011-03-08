#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/test/test_echo_service_in_java.py
      Based on ion/core/messaging/test_message_client.py
@author: Carlos Rueda
@summary: tests the interaction between python and java by making requests to
          an "EchoRpc" service in java, and verifying the response.
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from twisted.internet import defer
from ion.test.iontest import IonTestCase
from ion.core.object import object_utils
from ion.core.messaging import message_client

from net.ooici.play.instr.instrument_defs_pb2 import Command, SuccessFail, OK, ERROR

Command_type = object_utils.create_type_identifier(object_id=22001, version=1)


class MessageClientTest(IonTestCase):
    @defer.inlineCallbacks
    def setUp(self):
        yield self._start_container()

    @defer.inlineCallbacks
    def tearDown(self):
        yield self._stop_container()

    @defer.inlineCallbacks
    def test_send_message_instance2(self):
        """
        Adapted from test_send_message_instance() but here with communication 
        with an echo service in java.
        See IonSimpleEcho.java, SiamCiServerIonMsg.java in the SIAM-CI project.
        """
        
        mc = message_client.MessageClient(proc=self.test_sup)
        
        message = yield mc.create_instance(Command_type, MessageName='command sent message')
        message.command ='echo'
        arg = message.args.add()
        arg.channel = "myCh1"
        arg.parameter = "myParam1"
        
        # pid: use "EchoRpc" to make the request against the IonSimpleEcho service in java.
        #      use "SIAM-CI" to make the request against the SIAM-CI adapter in java, which accepts
        #      the "echo" operation for this purpose.
#        pid = "EchoRpc"
        pid = "SIAM-CI"
        
        log.debug(show_message(message, "Sending command to " +pid))
        
        (response, headers, msg) = yield self.test_sup.rpc_send(pid, 'echo', message)
        
#        from IPython.Shell import IPShellEmbed
#        ipshell = IPShellEmbed()
#        ipshell()
        
        self.assertIsInstance(response, message_client.MessageInstance)
        
        self.assertEqual(Command_type, response.MessageObject.ObjectType)
        
        self.assertEqual(response.command, 'echo')
        

#    @defer.inlineCallbacks
#    def test_send_message_instance(self):
#        """ Verbatim copy from test_message_client.py """
#        
#        processes = [
#            {'name':'echo1','module':'ion.core.process.test.test_process','class':'EchoProcess'},
#        ]
#        sup = yield self._spawn_processes(processes)
#        pid = sup.get_child_id('echo1')
#            
#        mc = message_client.MessageClient(proc=self.test_sup)
#            
##        addresslink_type = object_utils.create_type_identifier(object_id=20003, version=1)
#        person_type = object_utils.create_type_identifier(object_id=20001, version=1)
#
#        message = yield mc.create_instance(person_type, MessageName='person message')
#        message.name ='David'
#        
#        (response, headers, msg) = yield self.test_sup.rpc_send(pid, 'echo', message)
#        
#        self.assertIsInstance(response, message_client.MessageInstance)
#        self.assertEqual(response.name, 'David')
       
    
  
def show_message(msg, title="Message:"):
    prefix = "    | "
    contents = str(msg).strip().replace("\n", "\n"+prefix)
    return title+ "\n    " + str(type(msg)) + "\n" + prefix + contents
        