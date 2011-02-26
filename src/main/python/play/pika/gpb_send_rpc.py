#
# Adapted from demo_send_rpc.py to send a GPB message
# 

# remember to "python setup.py install" ion-object-definitions
from net.ooici.play.instr.instrument_defs_pb2 import Command

import pika
import uuid

import pika.log as logging

pika.log.setup(color=True)

receiver_queue_name = "SIAM-CI"

# See demo_receive_rpc.py.
reply_queue_name = None

connection = None
channel = None

# Import all adapters for easier experimentation
from pika.adapters import *


def on_connected(connection):
    logging.debug("demo_send: Connected to RabbitMQ")
    connection.channel(on_channel_open)


def on_channel_open(channel_):
    global channel
    global receiver_queue_name
    channel = channel_
    logging.debug("Received our Channel")
    channel.queue_declare(queue=receiver_queue_name, durable=True,
                          exclusive=False, auto_delete=False,
                          callback=on_receiver_queue_declared)


def on_receiver_queue_declared(frame):
    global receiver_queue_name
    logging.debug("Receiver Queue Declared: %s", receiver_queue_name)
    channel.queue_declare(queue='', #durable=True,
                          exclusive=True, #auto_delete=False,
                          callback=on_reply_queue_declared)
        
        
def on_reply_queue_declared(frame):
    reply_queue_name = frame.method.queue
    logging.debug("demo_send: Reply Queue Declared: %s", reply_queue_name)
    
    cmd = make_command()
    body = cmd.SerializeToString()
    corr_id = str(uuid.uuid4())
    logging.info("Sending command WITH CORR ID: %s" % (corr_id))

    channel.basic_publish(exchange='',
                          routing_key=receiver_queue_name,
                          body=body,
                          properties=pika.BasicProperties(
                              content_type="application/octet-stream",
                              delivery_mode=1,
                              reply_to = reply_queue_name,
                              correlation_id = corr_id
                          ))

    show_message(cmd, "Command sent:")
    
    # prepare to receive responses
    channel.basic_consume(handle_response, queue=reply_queue_name)
    
def handle_response(channel, method_frame, header_frame, body):
    pika.log.debug("RESPONSE RECEIVED content_type=%s delivery-tag=%i",
                  header_frame.content_type,
                  method_frame.delivery_tag
                  #, cmd.command
                  )
#    pika.log.debug("method_frame %s" , str(method_frame))
#    pika.log.debug("header_frame %s" , str(header_frame))
#    pika.log.debug("headers --> %s" , str(header_frame.headers))
    pika.log.info("correlation_id --> %s" , str(header_frame.correlation_id))
    channel.basic_ack(delivery_tag=method_frame.delivery_tag)
    
    cmd = Command()
    cmd.ParseFromString(body)
    show_message(cmd, "Command received:")
    

def make_command():
    cmd = Command()
    cmd.command = "hiCmd"
    for (c,p) in [ ("ch1", "pr1"), ("ch2", "pr2") ]:
        arg = cmd.args.add()
        arg.channel = c
        arg.parameter = p
    return cmd
    
  
def show_message(msg, title="Message:"):
    prefix = "    | "
    pika.log.info(title+ "\n    " + str(type(msg)) + "\n" + prefix + str(msg).replace("\n", "\n"+prefix))

if __name__ == '__main__':
    import sys
    host = (len(sys.argv) > 1) and sys.argv[1] or '127.0.0.1'
    parameters = pika.ConnectionParameters(host)
    connection = SelectConnection(parameters, on_connected)
    try:
        connection.ioloop.start()
    except KeyboardInterrupt:
        connection.close()
        connection.ioloop.start()
