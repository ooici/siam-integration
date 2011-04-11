package net.ooici.siamci.impl.gpb;

import java.io.IOException;

import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.siamci.IRequestDispatcher;
import net.ooici.siamci.SiamCiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Implementation using the GPBs directly.
 * 
 * @author carueda
 */
class SiamCiServerGpb implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiServerGpb.class);
	
	private final IRequestDispatcher requestProcessor;

	private final String queueName = SiamCiConstants.DEFAULT_QUEUE_NAME;
	
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	private volatile boolean keepRunning;

	SiamCiServerGpb(IRequestDispatcher requestProcessor) throws Exception {
		this.requestProcessor = requestProcessor;
		
		log.debug("Creating SiamCiProcess");
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
			byte[] body = delivery.getBody();
			
			
			BasicProperties props = delivery.getProperties();
			String replyTo = props.getReplyTo();
			String corr_id = props.getCorrelationId();
			String contentType = props.getContentType();
			
			log.debug(" [x] Received body len " + body.length);
			log.debug(" [x]   with replyTo= '" + replyTo + "' corr_id='" +corr_id+ "'");
			log.debug(" [x]     contentType= '" + contentType + "'");
			
			Command cmd = Command.parseFrom(body);
			_showMessage(cmd, "Command received:");
			
			
			if ( replyTo == null ) {
				log.warn(" [x] NOT REPLYING as replyTo is null");
				continue;
			}
			BasicProperties props2 = new BasicProperties();
			props2.setCorrelationId(corr_id);
			
			Message response = requestProcessor.dispatchRequest(cmd);
			
			byte[] replyBody = response.toByteArray();
			
		    channel.basicPublish("", replyTo, props2, replyBody);
		    _showMessage(response, "Response replied:");
		}
	}

	private void _showMessage(Message msg, String title) {
		final String prefix = "    | ";
		log.info(" [x] " +title+ "\n    " +msg.getClass() + "\n" + prefix + msg.toString().trim().replaceAll("\n", "\n"+prefix));
	}

	public void stop() {
		keepRunning = false;
	}
}
