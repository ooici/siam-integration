#
# Adapted from pika's example demo_receive to reply in an rcp style.
# NOTE: Does not work well: it gets messages from the demo_receive_queue queue
# but reply_to and other properties are not gotten! (pika bug?)
# 
import sys
import pika

# Import all adapters for easier experimentation
from pika.adapters import *

#pika.log.setup(pika.log.DEBUG, color=True)
pika.log.setup(color=True)

receiver_queue_name = "demo_receive_queue"
connection = None
channel = None

def on_connected(connection):
    global channel
    pika.log.info("demo_receive: Connected to RabbitMQ")
    connection.channel(on_channel_open)


def on_channel_open(channel_):
    global channel, receiver_queue_name
    channel = channel_
    pika.log.info("demo_receive: Received our Channel")
    channel.queue_declare(queue=receiver_queue_name, durable=True,
                          exclusive=False, auto_delete=False,
                          callback=on_receiver_queue_declared)


def on_receiver_queue_declared(frame):
    global receiver_queue_name
    pika.log.info("demo_receive: Receiver Queue Declared: %s", receiver_queue_name)
    channel.basic_consume(handle_delivery, queue=receiver_queue_name)



def handle_delivery(channel, method_frame, header_frame, body):
    """ 
    @todo reply_to and other properties not gotten! (pika bug?) 
    """
    
    reply_queue_name = None 
    if header_frame.reply_to:
        reply_queue_name = header_frame.reply_to
    else:
        pika.log.info("!!!!  header_frame.reply_to is None!  Not replying.")
        return
        
    pika.log.info("Replying to %s", reply_queue_name)    
        
    corr_id = "**arbitrary-corr-id**"
    if header_frame.correlation_id:
        corr_id = header_frame.correlation_id
    else:
        pika.log.info("!!!!  header_frame.correlation_id is None!!   Using arbitrary value")
    pika.log.info("Using correlation_id: %s", corr_id)
    
    pika.log.info("Basic.Deliver content_type=%s delivery-tag=%i: %s",
                  header_frame.content_type,
                  method_frame.delivery_tag,
                  body)
    pika.log.info("method_frame %s" , str(method_frame))
    pika.log.info("header_frame %s" , str(header_frame))

    channel.basic_ack(delivery_tag=method_frame.delivery_tag)

    pika.log.info("SENDING RESPONSE")
    channel.basic_publish(exchange='',
                          routing_key=reply_queue_name,
                          body=body,
                          properties=pika.BasicProperties(
                              content_type="text/plain",
                              delivery_mode=1,
                              correlation_id = corr_id
                          ))

if __name__ == '__main__':
    host = (len(sys.argv) > 1) and sys.argv[1] or '127.0.0.1'
    parameters = pika.ConnectionParameters(host)
    connection = SelectConnection(parameters, on_connected)
    try:
        connection.ioloop.start()
    except KeyboardInterrupt:
        connection.close()
        connection.ioloop.start()
