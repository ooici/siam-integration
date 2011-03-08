#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/SiamCi_proxy.py
@author Carlos Rueda
@brief Client to the SiamCi adapter server to support Siam_driver operations
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from twisted.internet import defer

from ion.core.object import object_utils
from ion.core.messaging import message_client

from ion.core import bootstrap

from net.ooici.play.instr.instrument_defs_pb2 import Command, SuccessFail, OK, ERROR

Command_type = object_utils.create_type_identifier(object_id=22001, version=1)


class SiamCiAdapterProxy():
    """
    Communicates with a SiamCiAdapter to support SiamInstrumentDriver operations.
    @todo: under construction
    """

    def __init__(self, queue='SIAM-CI'):
        """
        @param queue: the routing key to send requests. By default, "SIAM=CI"
        """
        self.queue = queue
        self.proc = None
        self.mc = None

        
    @defer.inlineCallbacks
    def start(self):
        """
        Starts the proxy. Call this before any other operation on this object.
        """
        if not self.proc:
            self.proc = yield bootstrap.create_supervisor()
            self.mc = message_client.MessageClient(proc=self.proc)
        
        
    @defer.inlineCallbacks
    def _make_command(self, cmd_name, args=[]):
        yield self.start()
        cmd = yield self.mc.create_instance(Command_type, MessageName='command sent message')
        cmd.command = cmd_name
        for (c,p) in args:
            arg = cmd.args.add()
            arg.channel = c
            arg.parameter = p
            
        defer.returnValue(cmd)
        
    @defer.inlineCallbacks
    def _rpc(self, message):
        """
        Generic call and handling of response.
        @param message: the payload to be sent
        @return: the response from the server
        """
        
        log.debug(show_message(message, "Sending command to " +self.queue))
        
        (response, headers, msg) = yield self.proc.rpc_send(recv=self.queue, operation='command', content=message)
        
        defer.returnValue(response)
    
    @defer.inlineCallbacks
    def ping(self):
        """
        A simple check of communication with the SiamCi server, it issues an "echo" command
        and returns true if the replied command is, hmm, "echo".
        """
        cmd = yield self._make_command("echo")
        response = yield self._rpc(cmd)
        log.debug(show_message(response, "ping response:"))
#        from IPython.Shell import IPShellEmbed
#        ipshell = IPShellEmbed()
#        ipshell()
        defer.returnValue(response.command == "echo")
        
    @defer.inlineCallbacks
    def list_ports(self):
        """
        Retrieves the ports (instrument services) running on the SIAM node
        """
        cmd = yield self._make_command("list_ports")
        response = yield self._rpc(cmd)
        log.debug(show_message(response, "list_ports response:"))
        
        defer.returnValue(response)
    
    
    @defer.inlineCallbacks
    def get_status(self, port):
        """
        Gets the status of a particular instrument as indicated by the given port
        """
        cmd = yield self._make_command("get_status", [("port", port)])
        response = yield self._rpc(cmd)
        log.debug(show_message(response, "get_status response:"))
        
        defer.returnValue(response)
        
    @defer.inlineCallbacks
    def get_last_sample(self, port):
        """
        Gets the last sample from the instrument on the given "port"
        """
        cmd = yield self._make_command("get_last_sample", [("port", port)])
        response = yield  self._rpc(cmd)
        log.debug(show_message(response, "get_last_sample response:"))
        
        defer.returnValue(response)
        
        


  
def show_message(msg, title="Message:"):
    prefix = "    | "
    contents = str(msg).strip().replace("\n", "\n"+prefix)
    return title+ "\n    " + str(type(msg)) + "\n" + prefix + contents
