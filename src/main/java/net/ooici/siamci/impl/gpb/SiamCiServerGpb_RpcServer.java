package net.ooici.siamci.impl.gpb;

import java.io.IOException;

import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.siamci.SiamCiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.RpcServer;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

/**
 * Implementation using {@link RpcServer}.
 * 
 * <p>
 * NOTE: THIS CLASS IS NOT USED ANYMORE. (kept for backup purposes and in case
 * some mechanism here might be useful later on.)
 * 
 * <p>
 * TODO complete use of RpcServer
 * 
 * @author carueda
 */
class SiamCiServerGpb_RpcServer {

	private static final Logger log = LoggerFactory
			.getLogger(SiamCiServerGpb_RpcServer.class);

	private final String queueName = SiamCiConstants.DEFAULT_QUEUE_NAME;

	private Connection connection;
	private Channel channel;

	private MyRpcServer rpcServer;

	private class MyRpcServer extends RpcServer {
		public MyRpcServer(Channel channel) throws IOException {
			super(channel);
		}

		/**
		 * We need to override this because the inherited implementation
		 * requires BOTH the reply-to and correlation-id properties to be given
		 * for handleCall to be called. However, we allow for a missing
		 * correlation-id.
		 */
		@Override
		public void processRequest(QueueingConsumer.Delivery request)
				throws IOException {
			BasicProperties props = request.getProperties();
			if (props.getReplyTo() != null && props.getCorrelationId() == null) {
				props.setCorrelationId("not-provided");
			}
			super.processRequest(request);
		}

		@Override
		public byte[] handleCall(byte[] body, AMQP.BasicProperties props) {
			String replyTo = props.getReplyTo();
			String corr_id = props.getCorrelationId();
			String contentType = props.getContentType();

			assert replyTo != null;
			assert corr_id != null;

			log.debug(" [x] Received body len " + body.length);
			log.debug(" [x]   with replyTo= '" + replyTo + "' corr_id='"
					+ corr_id + "'");
			log.debug(" [x]     contentType= '" + contentType + "'");

			Command cmd;
			try {
				cmd = Command.parseFrom(body);
			}
			catch (InvalidProtocolBufferException e) {
				log.warn("Invalid request: Not a Command.", e);
				return new byte[0]; // TODO notify error appropriately
			}

			_showMessage(cmd, "Command received:");

			byte[] response = _processCommand(cmd, body);

			return response;
		}

		protected byte[] _processCommand(Command cmd, byte[] body) {
			// TODO Auto-generated method stub
			return body;
		}

		@Override
		public void handleCast(AMQP.BasicProperties requestProperties,
				byte[] requestBody) {
			log.warn(" [x] NOT REPLYING as replyTo is null");
		}

		public boolean isRunning() {
			return _mainloopRunning;
		}
	}

	SiamCiServerGpb_RpcServer() throws Exception {
		log.debug("Creating SiamCiProcess");
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();
		DeclareOk declOk = channel.queueDeclare(queueName, true, false, false,
				null);

		log.debug("Queue " + declOk.getQueue() + " declared.");

		rpcServer = new MyRpcServer(channel);
	}

	public void run() throws IOException {
		log.info(" [*] SiamCiServerGpb running. To exit press CTRL+C");
		rpcServer.mainloop();
	}

	private void _showMessage(Message msg, String title) {
		final String prefix = "    | ";
		log.info(" [x] " + title + "\n    " + msg.getClass() + "\n" + prefix
				+ msg.toString().replaceAll("\n", "\n" + prefix));
	}

	public void stop() {
		if (rpcServer.isRunning()) {
			log.info("Ending SiamCiProcess");
		}
		try {
			rpcServer.close();
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}
		catch (IOException e) {
			log.warn("Error closing RPC server", e);
		}
	}
}
