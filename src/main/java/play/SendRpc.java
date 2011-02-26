package play;

import java.util.UUID;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

/**
 * A simple program demonstrating the basic publish of a message in RPC style
 * @author carueda
 */
public class SendRpc {

	private final static String receiver_queue_name = "demo_receive_queue";
	
	private static String reply_queue_name;

	private static ConnectionFactory factory;
	private static Connection connection;
	private static Channel channel;

	public static void main(String[] argv) throws Exception {

		factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();

		channel.queueDeclare(receiver_queue_name, true, false, false, null);
		
		DeclareOk declareOk = channel.queueDeclare("", false, true, false, null);
		reply_queue_name = declareOk.getQueue();
		
		
		_prepareForResponses();
		
		String corr_id = UUID.randomUUID().toString();
		BasicProperties props = new BasicProperties();
		props.setCorrelationId(corr_id);
		props.setReplyTo(reply_queue_name);
		props.setContentType("MyContentType");
		
		String message = "Hello World !!!!";
		channel.basicPublish("", receiver_queue_name, props, message.getBytes());
		
	    System.out.println(" [x] Sent '" + message + "'  with CORR ID = " +corr_id + "\n" + 
	    		          "      replyTo = " +reply_queue_name);
	}


	private static void _prepareForResponses() {
		new Thread(new Runnable() {
			public void run() {
				System.out.println(" [*] Waiting for responses. To exit press CTRL+C");

				QueueingConsumer consumer = new QueueingConsumer(channel);
				try {
					channel.basicConsume(reply_queue_name, true, consumer);
					while (true) {
						QueueingConsumer.Delivery delivery = consumer.nextDelivery();
						String message = new String(delivery.getBody());
						
						BasicProperties props = delivery.getProperties();
						String corr_id = props.getCorrelationId();
						
						System.out.println(" [x] Received RESPONSE '" + message + "'\n" +
								           "     corr_id=" +corr_id);
						
						
						// done (in this test we are just waiting for a response)
						connection.close();
						break;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}).start();
	}
}
