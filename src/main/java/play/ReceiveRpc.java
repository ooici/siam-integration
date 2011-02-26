package play;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Loop receiving messages from the "demo_receive_queue" queue and replying to
 * the rountingKey given by the value of the property reply-to, if any.
 * 
 * @author carueda
 */
public class ReceiveRpc {

	private final static String receiver_queue_name = "demo_receive_queue";

	private static ConnectionFactory factory;
	private static Connection connection;
	private static Channel channel;

	public static void main(String[] argv) throws Exception {
		_prepare();
		_dispatch();
	}

	private static void _prepare() throws Exception {
		factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();
		channel.queueDeclare(receiver_queue_name, true, false, false, null);
	}

	private static void _dispatch() throws Exception {
		System.out.println(" [*] ReceiveRpc: Waiting for messages. To exit press CTRL+C");

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
			System.out.println(" [x]   with replyTo= '" + replyTo + "' corr_id='" +corr_id+ "'");
			System.out.println(" [x]     contentType= '" + contentType + "'");
			
			if ( replyTo == null ) {
				System.out.println(" [x] NOT REPLYING as replyTo is null");
				continue;
			}
			
			BasicProperties props2 = new BasicProperties();
			props2.setCorrelationId(corr_id);
			props2.setAppId("MyAppId");
			
			// even that we specify the correlationId for the reply, include it the message as follows
			// just to facilitate testing.  The "lost properties" problem is happening with the
			// pika library on the python side -- the java client, SendRpc.java, is just fine.
			message = message.toUpperCase() + " -- ## corr_id=" +corr_id+ " ##";
			
		    channel.basicPublish("", replyTo, props2, message.getBytes());
		    System.out.println(" [x] Sent '" + message + "'");
		}
	}
}
