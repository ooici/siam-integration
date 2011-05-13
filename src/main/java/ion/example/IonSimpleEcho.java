package ion.example;

import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.StructureManager;

import java.util.Map;

import net.ooici.play.InstrDriverInterface.Command;

/**
 * A simple "echo" service using ION messaging. 
 * 
 * <p>
 * A java client is: {@link EchoClientTest}
 * 
 * <p>
 * A python client is: ion/agents/instrumentagents/test/test_echo_service_in_java.py
 * 
 * @author carueda
 */
public class IonSimpleEcho {
	
	// set this to true to verify that the unpacking of the received message works ok
	private static final boolean UNPACK = false;
	
	public static void main(String[] args) throws Exception {
		new IonSimpleEcho().run();
	}

	private void _log(String m) {
		System.out.println(getClass().getSimpleName() + ": " + m);
	}

	/** The queue this service is accepting requests at */
	private String queueName = "EchoRpc";
	
	/** The 'from' parameter when replying to a request */
    private MessagingName from = new MessagingName(queueName);

    /** basically we only need MsgBrokerClient to implement this service (no need for BaseProcess) */
	private final MsgBrokerClient ionClient;

	/**
	 * Creates this echo service.
	 * @throws Exception
	 */
	private IonSimpleEcho() throws Exception {

		ionClient = new MsgBrokerClient("localhost", com.rabbitmq.client.AMQP.PROTOCOL.PORT, "magnet.topic");
		ionClient.attach();
        ionClient.declareQueue(queueName);
        
        ionClient.bindQueue(queueName, new MessagingName(queueName), null);
        ionClient.attachConsumer(queueName);
	}
	
	/**
	 * Runs this echo service.
	 */
	void run() {
		try {
			_run();
		}
		catch ( Throwable e) {
			e.printStackTrace();
		}
		finally {
			ionClient.detach();
		}
	}

	/**
	 * A loop to receive a message and reply the received content to the "reply-to"
	 */
	private void _run() {
		
		while ( true ) {
			_log("\n===================================================================\n" +
					"Waiting for message on queue " +queueName+ "..."
			);
			IonMessage msgin = ionClient.consumeMessage(queueName);

			ionClient.ackMessage(msgin);
			
			if ( UNPACK ) {
				_unpack(msgin);
			}

			Map<String,String> headers = _getIonHeaders(msgin);
			
			final Object content = msgin.getContent();

			_log("headers: " + headers);

			//
			// to properly respond, we need the reply-to and conv-id property values:
			//
			String receiver = (String) headers.get("receiver");
			String toName = (String) headers.get("reply-to");
			String convId = (String) headers.get("conv-id");
			_log("receiver = " +receiver+ "    reply-to: " + toName);
			
			_sendReply(toName, convId, content);
		}
	}
	
	/**
	 * Sends the given content.
	 * @param toName
	 * @param convId
	 * @param content
	 */
	private void _sendReply(String toName, String convId, Object content) {
		MessagingName to = new MessagingName(toName);
		
		IonMessage msg = ionClient.createMessage(from, to, "noop", content);
        
        Map<String,String> headers = _getIonHeaders(msg);
        
		headers.put("conv-id", convId);
		headers.remove("accept-encoding");
		headers.put("encoding", "ION R1 GPB");
		
		// set 'status' -- note that the following error message is printed on the python side
		// if this property is not set:
		//   ERROR:RPC reply is not well formed. Header "status" must be set!
		//
		headers.put("status", "OK");
		
        ionClient.sendMessage(msg);
	}

	/** Utility to narrow the supress warning item */
	@SuppressWarnings("unchecked")
	private Map<String, String> _getIonHeaders(IonMessage msg) {
		Map<String,String> headers = msg.getIonHeaders();
		return headers;
	}

	// from ProtoUtils.testSendReceive()
	@SuppressWarnings("unchecked")
	private void _unpack(IonMessage reply) {
        System.out.println("\n------------------<_unpack>--------------------------------");
        System.out.println(" reply.getContent class = " +reply.getContent().getClass());
        StructureManager sm = StructureManager.Factory(reply);
        System.out.println(">>>> Head: " + sm.getHeadId());
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