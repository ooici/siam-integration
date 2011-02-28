#!/usr/bin/env python

"""
@file ion/agents/instrumentagents/SiamCi_proxy.py
@author Carlos Rueda
@brief Client to the SiamCi adapter server to support Siam_driver operations
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)

from net.ooici.play.instr.instrument_defs_pb2 import Command, SuccessFail, OK, ERROR

import pika
import uuid


class SiamCiAdapterProxy():
    """
    Communicates with a SiamCiAdapter to support SiamInstrumentDriver operations.
    @note: operations are blocking
    @todo: under construction
    """

    def __init__(self, queue='SIAM-CI', host='127.0.0.1'):
        """
        @param queue: the routing key to send requests
        @param host: AMQP server host
        """
        self.queue = queue
        self.host = host
        
        
    def _rpc(self, body):
        """
        Generic call and handling of reponse.
        @param body: the payload to be sent
        @return: the response from the server
        """
        self.connection = pika.adapters.BlockingConnection(pika.ConnectionParameters(host=self.host))
        self.channel = self.connection.channel()
        result = self.channel.queue_declare(exclusive=True)
        self.reply_queue = result.method.queue
        self.response = None

        corr_id = str(uuid.uuid4())
        log.debug("sending message to '%s' with correlation_id %s" %(self.queue, corr_id))
        self.channel.basic_publish(exchange='',
                              routing_key=self.queue,
                              body=body,
                              properties=pika.BasicProperties(
                                  content_type="application/octet-stream",
                                  delivery_mode=1,
                                  reply_to = self.reply_queue,
                                  correlation_id = corr_id
                              ))
                
        def _on_message(channel, method, header, body):
            self.response = body
            channel.stop_consuming()
            
        log.debug("waiting for response on queue: " +self.reply_queue)
        self.channel.basic_consume(_on_message, queue=self.reply_queue)
        
        self.connection.close()
        return self.response
    
    def ping(self):
        """
        A simple check of communication with the SiamCi server.
        """
        cmd = make_command("ping")
        body = cmd.SerializeToString()
        response = self._rpc(body)
        sf = SuccessFail()
        sf.ParseFromString(response)
        log.debug(show_message(sf, "ping response:"))
        return sf.result == OK
        
    def list_ports(self):
        """
        Retrieves the ports (instrument services) running on the SIAM node
        """
        cmd = make_command("list_ports")
        body = cmd.SerializeToString()
        response = self._rpc(body)
        sf = SuccessFail()
        sf.ParseFromString(response)
        log.debug(show_message(sf, "list_ports response:"))
        
        # TODO: convert the GPB to an appropriate structure
        return sf
    
    
    def get_status(self, port):
        """
        Gets the status of a particular instrument as indicated by the given port
        """
        cmd = make_command("get_status", [("port", port)])
        body = cmd.SerializeToString()
        response = self._rpc(body)
        sf = SuccessFail()
        sf.ParseFromString(response)
        log.debug(show_message(sf, "get_status response:"))
        
        # TODO: convert the GPB to an appropriate structure
        return sf
        
    def get_last_sample(self, port):
        """
        Gets the last sample from the instrument on the given "port"
        """
        cmd = make_command("get_last_sample", [("port", port)])
        body = cmd.SerializeToString()
        response = self._rpc(body)
        sf = SuccessFail()
        sf.ParseFromString(response)
        log.debug(show_message(sf, "get_last_sample response:"))
        
        # TODO: convert the GPB to an appropriate structure
        return sf
        
        


def make_command(cmd_name, args=[]):
    cmd = Command()
    cmd.command = cmd_name
    for (c,p) in args:
        arg = cmd.args.add()
        arg.channel = c
        arg.parameter = p
    return cmd
    
  
def show_message(msg, title="Message:"):
    prefix = "    | "
    contents = str(msg).strip().replace("\n", "\n"+prefix)
    return title+ "\n    " + str(type(msg)) + "\n" + prefix + contents
