package net.ooici.siamci.impl.gpb;

import java.io.IOException;

import net.ooici.siamci.SiamCiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Implementation using the GPBs directly.
 * TODO 
 * 
 * @author carueda
 */
class SiamCiServerGpb implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiServerGpb.class);
	

	private final String queueName = "demo_receive_queue"; //SiamCiConstants.DEFAULT_QUEUE_NAME;
	
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	private volatile boolean keepRunning;

	SiamCiServerGpb() throws Exception {
		log.info("Creating SiamCiProcess");
		factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();
		channel.queueDeclare(queueName, true, false, false, null);
	}
	
	public void run() {
		log.info(" [*] SiamCiServerGpb running. To exit press CTRL+C");
		keepRunning = true;
		try {
			_run();
		}
		catch ( Throwable ex) {
			ex.printStackTrace();
		}
		finally {
			log.info("Ending SiamCiProcess");
			try {
				connection.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * The dispatch loop.
	 * @throws Exception 
	 */
	private void _run() throws Exception {
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueName, true, consumer);
		
		while ( keepRunning ) {
			log.info("\n===========Waiting for call============");
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			
			BasicProperties props = delivery.getProperties();
			String replyTo = props.getReplyTo();
			String corr_id = props.getCorrelationId();
			String contentType = props.getContentType();
			
			log.info(" [x] Received '" + message + "'");
			log.info(" [x]   with replyTo= '" + replyTo + "' corr_id='" +corr_id+ "'");
			log.info(" [x]     contentType= '" + contentType + "'");
			
			if ( replyTo == null ) {
				log.info(" [x] NOT REPLYING as replyTo is null");
				continue;
			}
			
			BasicProperties props2 = new BasicProperties();
			props2.setCorrelationId(corr_id);
			
			// even that we specify the correlationId for the reply, include it the message as follows
			// just to facilitate testing.  The "lost properties" problem is happening with the
			// pika library on the python side -- the java client, SendRpc.java, is just fine.
			message = message.toUpperCase() + " -- ## corr_id=" +corr_id+ " ##";
			
		    channel.basicPublish("", replyTo, props2, message.getBytes());
		    log.info(" [x] Sent '" + message + "'");
		}
	}

	public void stop() {
		keepRunning = false;
	}
}