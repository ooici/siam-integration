package net.ooici.siamci.impl.ionmsg;

import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.ProtoUtils;
import ion.core.utils.StructureManager;

import java.io.PrintStream;
import java.util.Map;

import net.ooici.core.container.Container;
import net.ooici.core.container.Container.Structure;
import net.ooici.core.message.IonMessage.IonMsg;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.SiamCiConstants;
import net.ooici.siamci.IRequestProcessor.IPublisher;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * The actual SIAM-CI service.
 * 
 * It accepts RPC requests on the queue
 * {@link SiamCiConstants#DEFAULT_QUEUE_NAME}, uses the ICommandProcessor object
 * to process the incoming command, and replies the resulting response to the
 * routingKey indicated in the "reply-to" property of the request.
 * 
 * @author carueda
 */
class SiamCiServerIonMsg implements IPublisher, Runnable {

	private static final Logger log = LoggerFactory
			.getLogger(SiamCiServerIonMsg.class);

	// set this to true to verify that the unpacking of the received message
	// works ok
	private static final boolean UNPACK = false;

	private final String brokerHost;

	private final int brokerPort;

	/** TODO: use some parameter -- currently hard-coded for convenience */
	private static final String exchange = "magnet.topic";

	/**
	 * Timeout for waiting for a request.
	 */
	private static final long TIMEOUT = 3 * 1000;

	/** The queue this service is accepting requests at */
	private final String queueName;

	/** The 'from' parameter when replying to a request */
	private final MessagingName from;

	/** The processor of requests */
	private final IRequestProcessor requestProcessor;

	private final MsgBrokerClient ionClient;

	private volatile boolean keepRunning;
	private volatile boolean isRunning;

	/**
	 * Creates the Siam-Ci service.
	 * 
	 * @param brokerHost
	 * @param brokerPort
	 * @param queueName
	 * @param requestProcessor
	 *            To process incoming requests.
	 * @throws Exception
	 *             if something bad happens
	 */
	SiamCiServerIonMsg(String brokerHost, int brokerPort, String queueName,
			IRequestProcessor requestProcessor) throws Exception {
		this.brokerHost = brokerHost;
		this.brokerPort = brokerPort;
		this.queueName = queueName;
		this.from = new MessagingName(queueName);
		this.requestProcessor = requestProcessor;

		this.requestProcessor.setPublisher(this);

		if (log.isDebugEnabled()) {
			log.debug("Creating SiamCiProcess");
		}
		ionClient = new MsgBrokerClient(this.brokerHost, this.brokerPort,
				exchange);
		ionClient.attach();
		ionClient.declareQueue(queueName);

		ionClient.bindQueue(queueName, new MessagingName(queueName), null);
		ionClient.attachConsumer(queueName);
	}

	/**
	 * Runs this Siam-Ci service.
	 */
	public void run() {
		log.info("Running " + getClass().getSimpleName() + " (" + "broker='"
				+ brokerHost + ":" + brokerPort + "'" + ", queue='" + queueName
				+ "'," + " exchange='" + exchange + "'" + ")");
		keepRunning = true;
		isRunning = true;
		try {
			_run();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		finally {
			log.info("Ending " + getClass().getSimpleName());
			ionClient.detach();
			isRunning = false;
		}
	}

	/**
	 * Requests that the service stop accepting further requests.
	 */
	public void stop() {
		keepRunning = false;
	}

	boolean isRunning() {
		return isRunning;
	}

	/**
	 * The dispatch loop.
	 * 
	 * @throws InvalidProtocolBufferException
	 */
	private void _run() throws InvalidProtocolBufferException {

		for (int r = 0; keepRunning; r++) {
			log.info("[" + r + "] Waiting for request ...");

			IonMessage msgin = null;
			while (keepRunning
					&& null == (msgin = ionClient.consumeMessage(queueName,
							TIMEOUT))) {
				// timeout -- just try again
				//
				// Note: consumeMessage should throw an exception when there is
				// a non-timeout problem with getting the message. Email sent to
				// OOI folks
				// about this (2011-03-22).
			}

			if (keepRunning && msgin != null) {
				_dispatchRequest(msgin);
			}
		}
	}

	/**
	 * Dispatches the incoming request.
	 */
	private void _dispatchRequest(IonMessage msgin) {
		try {
			ionClient.ackMessage(msgin);
		}
		catch (Throwable e) {
			Map<String, String> headers = _getIonHeaders(msgin);
			log.warn("error while acknowledging message. headers = " + headers,
					e);
			return;
		}

		if (UNPACK) {
			_unpack(msgin);
		}

		Map<String, String> receivedHeaders = _getIonHeaders(msgin);
		if (log.isDebugEnabled()) {
			log.debug("headers: " + receivedHeaders);
		}
		final String sender = receivedHeaders.get("sender");
		log.info("Request received from '" + sender + "'");

		//
		// to properly respond, we need the reply-to and conv-id property
		// values:
		//
		final String toName = receivedHeaders.get("reply-to");
		final String convId = receivedHeaders.get("conv-id");

		if (toName == null) {
			// nobody to reply to?
			log.info("NOT REPLYING as reply-to is null");
			return;
		}

		Command cmd = _getCommand(msgin);

		final String publishStreamName = ScUtils.getPublishStreamName(cmd);
		if (publishStreamName != null) {
			log.info("Command with publish stream name: " + publishStreamName);
		}

		if (log.isDebugEnabled()) {
			log.debug(_showMessage(cmd, "Command received:"));
		}

		GeneratedMessage response = requestProcessor.processRequest(cmd);

		if (log.isDebugEnabled()) {
			log.debug(_showMessage(response, "Response to be replied:"));
		}

		if (convId == null) {
			log.info("Will reply but with no conv-id as it was not provided");
		}
		else {
			log.info("convId received: " + convId);
		}

		//
		// 2011-03-22
		// user-id and expiry were not needed initially, but are now required
		// for proper handling on the python side.
		// Use the values in the received message, if given; set some value
		// otherwise.
		// TODO: these props should probably be handled in a more proper way.
		//
		String userId = receivedHeaders.get("user-id");
		String expiry = receivedHeaders.get("expiry");
		if (userId == null) {
			userId = "ANONYMOUS";
		}
		if (expiry == null) {
			expiry = "0";
		}

		Container.Structure.Builder structureBuilder = ProtoUtils
				.addIonMessageContent(null, "myName", "Identity", response);
		_sendReply(toName, convId, userId, expiry, structureBuilder.build());
		log
				.info("Reply sent to '" + toName + "' conv-id: '" + convId
						+ "' user-id: '" + userId + "' expiry: '" + expiry
						+ "'" + "\n");
	}

	/**
	 * Sends the given content (structure).
	 * 
	 * @param toName
	 *            where the message is going
	 * @param convId
	 *            value for the "conv-id" header property; if null, no such
	 *            property is set
	 * @param userId
	 * @param expiry
	 * @param structure
	 *            See
	 *            ProtoUtils.addIonMessageContent(Container.Structure.Builder
	 *            structure, String name, String identity, GeneratedMessage
	 *            content)
	 */
	private void _sendReply(String toName, String convId, String userId,
			String expiry, Container.Structure structure) {

		MessagingName to = new MessagingName(toName);

		IonMessage msg = ionClient.createMessage(from, to, "noop", structure
				.toByteArray());

		Map<String, String> headers = _getIonHeaders(msg);

		if (convId != null) {
			headers.put("conv-id", convId);
		}
		headers.remove("accept-encoding");
		headers.put("encoding", "ION R1 GPB");

		headers.put("user-id", userId);
		headers.put("expiry", expiry);

		// set 'status' -- note that the following error message is printed on
		// the python side if this property is not set:
		// ERROR:RPC reply is not well formed. Header "status" must be set!
		//
		headers.put("status", "OK");
		
		
		
		headers.put("MY_Header", "MY_someValue");

		ionClient.sendMessage(msg);
	}

	/**
	 * {@link IPublisher} operation.
	 */
	public void publish(String publishId, GeneratedMessage response, String streamName) {
		if (log.isDebugEnabled()) {
			log.debug("Publishing to queue='" + streamName + "'" + " reponse='"
					+ response + "'");
		}

		// TODO: "SomeName", "Identity": determine appropriate values.
		Structure structure = ProtoUtils.addIonMessageContent(null, "SomeName",
				"Identity", response).build();

		String toName = streamName;
		MessagingName to = new MessagingName(toName);

		IonMessage msg = ionClient.createMessage(from, to, "acceptResponse",
				structure.toByteArray());

		Map<String, String> headers = _getIonHeaders(msg);
		headers.remove("accept-encoding");
		headers.put("encoding", "ION R1 GPB");

		headers.put("user-id", "ANONYMOUS");
		headers.put("expiry", "0");

		headers.put("status", "OK");
		
		headers.put("publish_id", publishId);

		ionClient.sendMessage(msg);
		if (log.isDebugEnabled()) {
			log.debug("Publish message sent.");
		}
	}

	/**
	 * Returns a string with contents of the given message using a prefix for
	 * each line, like so:
	 * 
	 * <pre>
	 * --title goes here--
	 * 		    class net.ooici.play.InstrDriverInterface$Command
	 * 		    | command: "echo"
	 * 		    | args {
	 * 		    |   channel: "myCh1"
	 * 		    |   parameter: "myParam1"
	 * 		    | }
	 * </pre>
	 * 
	 * @param msg
	 * @param title
	 * @return
	 */
	private String _showMessage(Message msg, String title) {
		final String prefix = "    | ";
		return " [x] " + title + "\n    " + msg.getClass() + "\n" + prefix
				+ msg.toString().trim().replaceAll("\n", "\n" + prefix);
	}

	/** Utility to narrow the supress warning item */
	@SuppressWarnings("unchecked")
	private Map<String, String> _getIonHeaders(IonMessage msg) {
		Map<String, String> headers = msg.getIonHeaders();
		return headers;
	}

	/**
	 * Extracts the Command from the message.
	 */
	@SuppressWarnings("unchecked")
	private Command _getCommand(IonMessage reply) {
		StructureManager sm = StructureManager.Factory(reply);
		for (String key : sm.getItemIds()) {
			GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
			Command cmd = demWrap.getObjectValue();
			return cmd;
		}
		return null;
	}

	// from ProtoUtils.testSendReceive()
	@SuppressWarnings("unchecked")
	private void _unpack(IonMessage reply) {
		log.debug("\n------------------<_unpack>");
		log.debug(" reply.getContent class = " + reply.getContent().getClass());
		StructureManager sm = StructureManager.Factory(reply);
		log.debug(">>>> Heads:");
		for (String key : sm.getHeadIds()) {
			log.debug("  headId = " + key);
			GPBWrapper<IonMsg> msgWrap = sm.getObjectWrapper(key);
			log.debug("  object wrapper = " + msgWrap);
			// IonMsg msg = msgWrap.getObjectValue();
		}
		log.debug("\n>>>> Items:");
		for (String key : sm.getItemIds()) {
			log.debug("  itemId = " + key);
			GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
			log.debug("  object wrapper = " + demWrap);
			// Command dem = demWrap.getObjectValue();
		}
		log.debug("------------------</_unpack>\n");
	}

	static {
		//
		// TODO, once ioncore-java starts using standard logging, eliminate the
		// following, which simply "redirects" System.out messages to a file.
		//
		final String outputFile = "stdout.txt";
		try {
			System.setOut(new PrintStream(outputFile));
		}
		catch (Throwable ex) {
			log.warn("Could not set system out to file " + outputFile, ex);
		}
	}
}
