package net.ooici.siamci.impl.ionmsg;

import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.ProtoUtils;
import ion.core.utils.StructureManager;
import ion.example.IonSimpleEcho;

import java.util.Map;

import net.ooici.core.container.Container;
import net.ooici.core.message.IonMessage.IonMsg;
import net.ooici.play.instr.InstrumentDefs.Command;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.SiamCiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * The actual SIAM-CI service.
 * 
 * It accepts RPC requests on the queue {@link SiamCiConstants#DEFAULT_QUEUE_NAME},
 * uses the ICommandProcessor object to process the incoming command, and
 * replies the resulting response to the routingKey indicated in the "reply-to"
 * property of the request.
 * 
 * <p>
 * Initially based on {@link IonSimpleEcho}, but here with handling of the structure.
 *  
 * @author carueda
 */
class SiamCiServerIonMsg implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiServerIonMsg.class);
	
	// set this to true to verify that the unpacking of the received message works ok
	private static final boolean UNPACK = false;

	
	/** TODO: use some parameter -- currently hard-code for convenience */
	private static final String brokerHost = "localhost";

	/** TODO: use some parameter -- currently hard-code for convenience */
	private static final int brokerPort = com.rabbitmq.client.AMQP.PROTOCOL.PORT;

	/** TODO: use some parameter -- currently hard-code for convenience */
	private static final String exchange = "magnet.topic";
	

	/** The queue this service is accepting requests at */
	private final String queueName = SiamCiConstants.DEFAULT_QUEUE_NAME;
	
	/** The 'from' parameter when replying to a request */
	private final MessagingName from = new MessagingName(queueName);
	
	/** The processor of requests */
	private final IRequestProcessor requestProcessor;
	
	private final MsgBrokerClient ionClient;
	
	private volatile boolean keepRunning;
	

	/**
	 * Creates the Siam-Ci service.
	 * @param requestProcessor   To process incoming requests.
	 * @throws Exception     if something bad happens
	 */
	SiamCiServerIonMsg(IRequestProcessor requestProcessor) throws Exception {
		this.requestProcessor = requestProcessor;
		log.info("Creating SiamCiProcess");
		ionClient = new MsgBrokerClient(brokerHost, brokerPort, exchange);
		ionClient.attach();
        ionClient.declareQueue(queueName);
        
        ionClient.bindQueue(queueName, new MessagingName(queueName), null);
        ionClient.attachConsumer(queueName);
	}
	
	/**
	 * Runs this Siam-Ci service.
	 */
	public void run() {
		log.info("Running " +getClass().getSimpleName());
		keepRunning = true;
		try {
			_run();
		}
		catch ( Throwable e) {
			e.printStackTrace();
		}
		finally {
			log.info("Ending " +getClass().getSimpleName());
			ionClient.detach();
		}
	}

	/**
	 * The dispatch loop.
	 * @throws InvalidProtocolBufferException 
	 */
	private void _run() throws InvalidProtocolBufferException {
		
		while ( keepRunning ) {
			log.info("Waiting for request ...");
			IonMessage msgin = ionClient.consumeMessage(queueName);

			ionClient.ackMessage(msgin);
			
			if ( UNPACK ) {
				_unpack(msgin);
			}

			Map<String,String> headers = _getIonHeaders(msgin);
			//
			// to properly respond, we need the reply-to and conv-id property values:
			//
			final String receiver = (String) headers.get("receiver");
			final String toName = (String) headers.get("reply-to");
			final String convId = (String) headers.get("conv-id");
			
			if ( log.isDebugEnabled() ) {
				log.debug("headers: " + headers);
				log.debug("receiver = " +receiver+ "    reply-to: " + toName+ "    conv-id: " +convId);
			}

			if ( toName == null ) {
				// nobody to reply to?
				log.warn(" [x] NOT REPLYING as reply-to is null");
				continue;
			}
			
			Command cmd = _getCommand(msgin);
			
			if ( log.isDebugEnabled() ) {
				log.debug(_showMessage(cmd, "Command received:"));
			}
			
			GeneratedMessage response = requestProcessor.processRequest(cmd);

			if ( log.isDebugEnabled() ) {
				log.debug(_showMessage(response, "Response to be replied:"));
			}
			
			if ( convId == null ) {
				log.warn(" [x] Will reply but with no conv-id as it was not provided");
				continue;
			}
			
			Container.Structure.Builder structureBuilder = ProtoUtils.addIonMessageContent(null, "myName", "Identity", response);
			_sendReply(toName, convId, structureBuilder.build());
		}
	}
	
	/**
	 * Sends the given content (structure).
	 * 
	 * @param toName where the message is going
	 * @param convId value for the "conv-id" header property; if null, no such property is set
	 * @param structure  See ProtoUtils.addIonMessageContent(Container.Structure.Builder structure, String name, String identity, GeneratedMessage content)
	 */
	private void _sendReply(String toName, String convId, Container.Structure structure) {
		MessagingName to = new MessagingName(toName);
		
		IonMessage msg = ionClient.createMessage(from, to, "noop", structure.toByteArray());
        
        Map<String,String> headers = _getIonHeaders(msg);
        
        if ( convId != null ) {
        	headers.put("conv-id", convId);
        }
		headers.remove("accept-encoding");
		headers.put("encoding", "ION R1 GPB");
		
		// set 'status' -- note that the following error message is printed on the python side
		// if this property is not set:
		//   ERROR:RPC reply is not well formed. Header "status" must be set!
		//
		headers.put("status", "OK");
		
        ionClient.sendMessage(msg);
	}

	
	/**
	 * Requests that the service stop accepting further requests.
	 * Note that it is possible that the processing does not actually terminate.
	 */
	public void stop() {
		keepRunning = false;
	}
	
	/**
	 * Returns a string with contents of the given message using a prefix for each line, like so:
		<pre>
		    --title goes here--
		    class net.ooici.play.instr.InstrumentDefs$Command
		    | command: "echo"
		    | args {
		    |   channel: "myCh1"
		    |   parameter: "myParam1"
		    | }
		</pre>
	 * @param msg
	 * @param title
	 * @return
	 */
	private String _showMessage(Message msg, String title) {
		final String prefix = "    | ";
		return " [x] " +title+ "\n    " +msg.getClass() + "\n" + prefix + msg.toString().trim().replaceAll("\n", "\n"+prefix);
	}

	
	/** Utility to narrow the supress warning item */
	@SuppressWarnings("unchecked")
	private Map<String, String> _getIonHeaders(IonMessage msg) {
		Map<String,String> headers = msg.getIonHeaders();
		return headers;
	}
	
	@SuppressWarnings("unchecked")
	private Command _getCommand(IonMessage reply) {
        StructureManager sm = StructureManager.Factory(reply);
//        System.out.println("\n>>>> Items:");
        for(String key : sm.getItemIds()) {
            System.out.println(key);
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            System.out.println(demWrap);
            Command cmd = demWrap.getObjectValue();
            return cmd;
        }
        return null;
	}

	// from ProtoUtils.testSendReceive()
	@SuppressWarnings("unchecked")
	private void _unpack(IonMessage reply) {
        System.out.println("\n------------------<_unpack>--------------------------------");
        System.out.println(" reply.getContent class = " +reply.getContent().getClass());
        StructureManager sm = StructureManager.Factory(reply);
        System.out.println(">>>> Heads:");
        for(String key : sm.getHeadIds()) {
            System.out.println(key);
            GPBWrapper<IonMsg> msgWrap = sm.getObjectWrapper(key);
            System.out.println(msgWrap);
//            IonMsg msg = msgWrap.getObjectValue();
        }
        System.out.println("\n>>>> Items:");
        for(String key : sm.getItemIds()) {
            System.out.println(key);
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            System.out.println(demWrap);
//            Command dem = demWrap.getObjectValue();
        }
        System.out.println("------------------</_unpack>--------------------------------\n");
	}
	
}
