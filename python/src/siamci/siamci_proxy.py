#!/usr/bin/env python

"""
@file siamci/siamci_proxy.py
@author Carlos Rueda
@brief Client to the SIAM-CI adapter service (in java) to support Siam_driver operations
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
import logging

from twisted.internet import defer

from ion.core.object import object_utils
from ion.core.messaging import message_client

from ion.core import bootstrap

from siamci.util.tcolor import red, blue

from net.ooici.play.instr_driver_interface_pb2 import Command, SuccessFail, OK, ERROR

Command_type = object_utils.create_type_identifier(object_id=20034, version=1)


class SiamCiAdapterProxy():
    """
    Communicates with a SIAM-CI adapter service to support SiamInstrumentDriver operations.
    
    @note: An instance of this proxy can have an associated port (given at construction time) 
    for those operations that are instrument-specific. Other non-instrument specific operations
    are also provided.
    
    There are two main behaviors for the various operations depending on the value given to
    the 'publish_stream' parameter: If the 'publish_stream' parameter in not None, this 
    indicates that the result of the command should be published to the queue (routing key)
    given by the parameter. In this case, the return of the method is a SuccessFail object
    indicating whether the command has been or has not been successfully submitted.
    If the 'publish_stream' is None (the default), then no such asynchronous behavior is enabled,
    with the command result being returned by the method itself.
    
    @note: All operations here work at the level of GPBs.
    """

    def __init__(self, queue, port=None):
        """
        @param queue: the routing key to send requests against (aka "pid" of the SIAM-CI adapter service).
        @param port: the port associated for those operations that are instrument specific. None by default.
        """
        if not queue:
            raise Exception("queue must be given")
        
        self.queue = queue
        self.port = port
        self.proc = None
        self.mc = None

        
    @defer.inlineCallbacks
    def start(self):
        """
        Starts the proxy. Call this before any other operation on this object.
        """
        if not self.proc:
            log.info("Starting SiamCiAdapterProxy")
            self.proc = yield bootstrap.create_supervisor()
            self.mc = message_client.MessageClient(proc=self.proc)
     
        
    @defer.inlineCallbacks
    def stop(self):
        """
        Shuts down the proxy.
        """
        log.debug("Stopping SiamCiAdapterProxy...")
        yield self.proc.shutdown()
        
        
    @defer.inlineCallbacks
    def _make_command(self, cmd_name, args=None, publish_stream=None):
        """
        Creates a command message.
        
        @param cmd_name: name of the command
        @param args: list of tuples
        @param publish_stream: Name of queue for asynchronous notification of result
        """
        
        assert self.proc is not None, "SiamCiAdapterProxy must be started"

        if args is None: args = [] 
        
        assert(isinstance(args,list))
        
        cmd = yield self.mc.create_instance(Command_type, MessageName='command sent message')
        cmd.command = cmd_name
        for (c,p) in args:
            arg = cmd.args.add()
            arg.channel = c
            arg.parameter = p
            
        if publish_stream:
            cmd.publish_stream = publish_stream
            
        defer.returnValue(cmd)
        
    @defer.inlineCallbacks
    def _rpc(self, message):
        """
        Generic call and handling of response.
        @param message: the payload to be sent
        @return: the response from the SIAM-CI adapter
        """
        
        _debug_message(message, "Sending command to " +self.queue)
        
        (response, headers, msg) = yield self.proc.rpc_send(recv=self.queue, operation='command', content=message)
        
        defer.returnValue(response)
        
    
    @defer.inlineCallbacks
    def ping(self):
        """
        A simple check of communication with the SiamCi server, it issues an "echo" command
        and returns true if the replied command is "echo".
        """
        cmd = yield self._make_command("echo")
        response = yield self._rpc(cmd)
        _debug_message(response, "ping response:")
#        from IPython.Shell import IPShellEmbed
#        ipshell = IPShellEmbed()
#        ipshell()
        defer.returnValue(response.command == "echo")
        
    @defer.inlineCallbacks
    def list_ports(self, publish_stream=None):
        """
        Retrieves the ports (instrument services) running on the SIAM node
        """
        cmd = yield self._make_command("list_ports", publish_stream=publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "list_ports response:")
        
        defer.returnValue(response)
    
    
    @defer.inlineCallbacks
    def get_channels(self, publish_stream=None):
        """
        Gets the names of the channels associated with the instrument identified by the given "port"
        
        @param publish_stream:
        """
        
        assert self.port, "No port provided"
        
        args = [("port", self.port)]
        cmd = yield self._make_command("get_channels", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "get_channels response:")
        
        defer.returnValue(response)
 
     
     
    @defer.inlineCallbacks
    def get_status(self, params=None, publish_stream=None):
        """
        Gets the status of a particular instrument as indicated by the given port at creation time.
        
        @param params: parameters
        
        @param publish_stream: If not None, the result of the command will be published to this
                  queue (routing queue). Otherwise, the result will be returned by this method.
                  
        @return: if publish_stream is None, the result of the command; otherwise a SuccessFail object
                 indicating whether the command has been or has not been successfully submitted, with
                 the actual result to be published in the given stream.
        """
        assert self.port, "No port provided"
        
        
        if params is None: params = []
        assert(isinstance(params,(list,tuple))), 'Expected list or tuple params in get_status (SiamCiProxy).'
        assert(all(map(lambda x:isinstance(x,tuple),params))), \
            'Expected tuple elements in params list'
            
        args = [("port", self.port)]
        # extend args with the elements in params (note that params can be a list or a tuple)
        for p in params:
            args.extend([p])
            
        cmd = yield self._make_command("get_status", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "get_status response:")
        
        defer.returnValue(response)
        


    @defer.inlineCallbacks
    def get_last_sample(self, publish_stream=None):
        """
        Gets the last sample from the instrument identified by the given "port"
        """
        assert self.port, "No port provided"
        cmd = yield self._make_command("get_last_sample", [("port", self.port)], publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "get_last_sample response:")
        
        defer.returnValue(response)
        
    @defer.inlineCallbacks
    def fetch_params(self, param_list=None, publish_stream=None):
        """
        Gets parameters associated with the instrument identified by the given "port"
        
        @param param_list: the list of desired parameters. If None (which is the default), 
              all parameters are requested.
        """
        if param_list is None: param_list = [] 
        assert self.port, "No port provided"
        
        args = [("port", self.port)]
        for it in param_list:
            if isinstance(it, tuple):
                args.extend([it])
            else:
                assert not isinstance(it, (list, dict))
                
                # see https://confluence.oceanobservatories.org/display/CIDev/Instrument+Driver+Interface#InstrumentDriverInterface-getmessage
                args.extend( [ ('instrument', it) ])
                
        cmd = yield self._make_command("fetch_params", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "fetch_params response:")
        
        defer.returnValue(response)
        
        
    @defer.inlineCallbacks
    def set_params(self, params, publish_stream=None):
        """
        TODO: note, currently, params must be a dict with string keys and values
        with the understanding that all parameters are for the
        instrument as a whole (ie., not for specific channels).
        """

        assert(isinstance(params, dict))
        assert(all(map(lambda x: isinstance(x,str),
                       params.keys()))), 'Each key must be a string'
        assert(all(map(lambda x: isinstance(x,str),
                       params.values()))), 'Each value must be a string'
        
        
        args = [("port", self.port)]
        args.extend([item for item in params.items()])
             
        cmd = yield self._make_command("set_params", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "set_params response:")
        
        defer.returnValue(response)
        
        
    @defer.inlineCallbacks
    def execute_StartAcquisition(self, channel, publish_stream):
        """
        Sends a execute_StartAcquisition command.
        @todo: very preliminary
        """

        assert(channel is not None)
        assert(publish_stream is not None)
        
        log.debug("execute_StartAcquisition: channel='%s' publish_stream='%s'" % (str(channel), str(publish_stream)))
        
        args = [("port", self.port), ("channel", channel)]
             
        cmd = yield self._make_command("execute_StartAcquisition", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "execute_StartAcquisition response:")
        
        defer.returnValue(response)
        
        
    @defer.inlineCallbacks
    def execute_StopAcquisition(self, channel, publish_stream):
        """
        Sends a execute_StopAcquisition command.
        @todo: very preliminary
        """

        assert(channel is not None)
        assert(publish_stream is not None)
        
        log.debug("execute_StopAcquisition: channel='%s' publish_stream='%s'" % (str(channel), str(publish_stream)))
        
        args = [("port", self.port), ("channel", channel)]
             
        cmd = yield self._make_command("execute_StopAcquisition", args, publish_stream)
        response = yield self._rpc(cmd)
        _debug_message(response, "execute_StopAcquisition response:")
        
        defer.returnValue(response)
        
        

        
        

def _debug_message(msg, title):
    if log.getEffectiveLevel() > logging.DEBUG:
        return
    log.debug(_show_message(msg, title))
  
def _show_message(msg, title="Message:"):
    prefix = "    | "
    contents = str(msg).strip().replace("\n", "\n"+prefix)
    return title+ "\n    " + str(type(msg)) + "\n" + prefix + contents
