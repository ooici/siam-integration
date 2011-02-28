package net.ooici.siamci.impl.gpb;

import java.io.IOException;
import java.util.List;

import net.ooici.play.instr.InstrumentDefs.Command;
import net.ooici.play.instr.InstrumentDefs.Result;
import net.ooici.play.instr.InstrumentDefs.StringPair;
import net.ooici.play.instr.InstrumentDefs.SuccessFail;
import net.ooici.play.instr.InstrumentDefs.SuccessFail.Builder;
import net.ooici.play.instr.InstrumentDefs.SuccessFail.Item;
import net.ooici.siamci.ISiam;
import net.ooici.siamci.SiamCiConstants;
import net.ooici.siamci.ISiam.PortItem;

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
 * TODO 
 * 
 * @author carueda
 */
class SiamCiServerGpb implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiServerGpb.class);
	
	private final ISiam siam;

	private final String queueName = SiamCiConstants.DEFAULT_QUEUE_NAME;
	
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	private volatile boolean keepRunning;

	SiamCiServerGpb(ISiam siam) throws Exception {
		this.siam = siam;
		
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
			
			Message response = _processCommand(cmd);
			
			byte[] replyBody = response.toByteArray();
			
		    channel.basicPublish("", replyTo, props2, replyBody);
		    _showMessage(response, "Response replied:");
		}
	}

	private Message _processCommand(Command cmd) {
		Message response;

		if ( "ping".equals(cmd.getCommand()) ) {
			SuccessFail sf = SuccessFail.newBuilder().setResult(Result.OK).build();
			response = sf;
		}
		else if ( "list_ports".equals(cmd.getCommand()) ) {
			response = _listPorts();
		}
		else {
			// TODO others
			String description = "Command '" +cmd.getCommand()+ "' not implemented";
			log.debug(description);
			response = _createErrorResponse(description);
		}
		
		return response;
	}

	private Message _listPorts() {
		List<PortItem> list = null;
		try {
			list = siam.listPorts();
		}
		catch (Exception e) {
			log.warn("_listPorts exception", e);
			return _createErrorResponse("Exception: " +e.getMessage());
		}
		
		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for ( PortItem pi : list ) {
			
			buildr.addItem(Item.newBuilder()
					.setType(Item.Type.PAIR)
					.setPair(StringPair.newBuilder().setFirst("portName").setSecond(pi.portName)))
			;
			
			buildr.addItem(Item.newBuilder()
					.setType(Item.Type.PAIR)
					.setPair(StringPair.newBuilder().setFirst("deviceId").setSecond(String.valueOf(pi.deviceId))))
					;
		}
		
		SuccessFail response = buildr.build();
		return response;
	}

	private Message _createErrorResponse(String description) {
		SuccessFail sf = SuccessFail.newBuilder()
			.setResult(Result.ERROR)
			.addItem(Item.newBuilder()
					.setType(Item.Type.STR)
					.setStr(description)
					.build())
			.build();
		return sf;
	}
	
	private void _showMessage(Message msg, String title) {
		final String prefix = "    | ";
		log.info(" [x] " +title+ "\n    " +msg.getClass() + "\n" + prefix + msg.toString().trim().replaceAll("\n", "\n"+prefix));
	}

	public void stop() {
		keepRunning = false;
	}
}
