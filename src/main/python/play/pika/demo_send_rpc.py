#
# Adapted from pika's example demo_send to send and receive messages in an rcp style
# 
import sys
import pika
import time
import uuid

import pika.log as logging

pika.log.setup(color=True)

receiver_queue_name = "demo_receive_queue"

# See demo_receive_rpc.py.
reply_queue_name = None

connection = None
channel = None

# Import all adapters for easier experimentation
from pika.adapters import *


def on_connected(connection):
    logging.info("demo_send: Connected to RabbitMQ")
    connection.channel(on_channel_open)


def on_channel_open(channel_):
    global channel
    global receiver_queue_name
    channel = channel_
    logging.info("demo_send: Received our Channel")
    channel.queue_declare(queue=receiver_queue_name, durable=True,
                          exclusive=False, auto_delete=False,
                          callback=on_receiver_queue_declared)


def on_receiver_queue_declared(frame):
    global receiver_queue_name
    logging.info("demo_send: Receiver Queue Declared: %s", receiver_queue_name)
    channel.queue_declare(queue='', #durable=True,
                          exclusive=True, #auto_delete=False,
                          callback=on_reply_queue_declared)
        
        
def on_reply_queue_declared(frame):
    reply_queue_name = frame.method.queue
#    logging.info("on_reply_queue_declared: frame: %s", str(frame))
    logging.info("demo_send: Reply Queue Declared: %s", reply_queue_name)
    
    # now send some messages
    for x in xrange(0, 1):
        message = "Hello World #%i: %.8f" % (x, time.time())
        corr_id = str(uuid.uuid4())
        logging.info("Sending: %s  WITH CORR ID: %s" % (message,corr_id))

        channel.basic_publish(exchange='',
                              routing_key=receiver_queue_name,
                              body=message,
                              properties=pika.BasicProperties(
                                  content_type="text/plain",
                                  delivery_mode=1,
                                  reply_to = reply_queue_name,
                                  correlation_id = corr_id
                              ))

    # prepare to receive responses
    channel.basic_consume(handle_response, queue=reply_queue_name)
    
def handle_response(channel, method_frame, header_frame, body):
    pika.log.info("RESPONSE RECEIVED content_type=%s delivery-tag=%i: %s",
                  header_frame.content_type,
                  method_frame.delivery_tag,
                  body)
    pika.log.info("method_frame %s" , str(method_frame))
    pika.log.info("header_frame %s" , str(header_frame))
    pika.log.info("headers --> %s" , str(header_frame.headers))
    pika.log.info("correlation_id --> %s" , str(header_frame.correlation_id))
    channel.basic_ack(delivery_tag=method_frame.delivery_tag)

if __name__ == '__main__':
    host = (len(sys.argv) > 1) and sys.argv[1] or '127.0.0.1'
    parameters = pika.ConnectionParameters(host)
    connection = SelectConnection(parameters, on_connected)
    try:
        connection.ioloop.start()
    except KeyboardInterrupt:
        connection.close()
        connection.ioloop.start()
