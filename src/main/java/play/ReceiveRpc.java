package play;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Loop receiving messages from the "demo_receive_queue" queue and replying to
 * the the rountingKey given by the value of the property reply-to, if any.
 * 
 * @author carueda
 */
public class ReceiveRpc {

	private final static String receiver_queue_name = "demo_receive_queue";

	public static void main(String[] argv) throws java.io.IOException,
			java.lang.InterruptedException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(receiver_queue_name, true, false, false, null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(receiver_queue_name, true, consumer);

		while (true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			
			BasicProperties props = delivery.getProperties();
			String replyTo = props.getReplyTo();
			String corr_id = props.getCorrelationId();
			String contentType = props.getContentType();
			
			System.out.println(" [x] Received '" + message + "'");
			System.out.println(" [x]     replyTo '" + replyTo + "'    corr_id=" +corr_id);
			System.out.println(" [x]     contentType '" + contentType + "'");
			
			if ( replyTo == null ) {
				System.out.println(" [x] NOT REPLYING as replyTo is null");
				continue;
			}
			
			BasicProperties props2 = new BasicProperties();
			props2.setCorrelationId(corr_id);
			props2.setAppId("MyAppId");
			
			// even that we specify the correlationId for the reply, include in the message as follows
			// just to faciliate testing.  The "lost properties" problem is happening with the
			// pika library on the python side-- the java client, Send.java, is just fine.
			message = "##corr_id=" +corr_id+ "## " +message;
			
		    channel.basicPublish("", replyTo, props2, message.getBytes());
		    System.out.println(" [x] Sent '" + message + "'");
		}
	}
}
