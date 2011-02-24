package ion.example;

import ion.core.BaseProcess;
import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.StructureManager;

import java.util.Map;

import net.ooici.core.message.IonMessage.IonMsg;
import net.ooici.play.instr.InstrumentDefs.Command;

/**
 * A simple "echo" service. Use {@link EchoClientTest} to test this service.
 * 
 * When launched you shoud see something like:
<pre>
TODO
</pre>
 * 
 * @author carueda
 */
public class IonSimpleEcho {
	
	// set to true to verify that the unpacking of the received message works ok
	private static final boolean UNPACK = false;

	public static void main(String[] args) throws Exception {
		new IonSimpleEcho();
	}

	private void _log(String m) {
		System.out.println(getClass().getSimpleName() + ": " + m);
	}

	private final String queueName = "ECHO";

	private final MsgBrokerClient ionClient;
	private final BaseProcess baseProcess;

	private IonSimpleEcho() throws Exception {

		_log("******START******");
		ionClient = new MsgBrokerClient("localhost", com.rabbitmq.client.AMQP.PROTOCOL.PORT, "magnet.topic");
		ionClient.attach();
		ionClient.attachConsumer(queueName);
		baseProcess = new BaseProcess(ionClient);
		baseProcess.spawn();

		try {
			_run();
		}
		finally {
			baseProcess.dispose();
		}
	}

	/**
	 * A loop to receive a message and reply the received content to the "reply-to"
	 */
	private void _run() {
		
		while ( true ) {
			_log("\n===================================================================\n" +
					"Waiting for message ..."
			);
			IonMessage msgin = ionClient.consumeMessage(queueName);

			ionClient.ackMessage(msgin);
			
			if ( UNPACK ) {
				_unpack(msgin);
			}

			Map headers = msgin.getIonHeaders();
			Object content = msgin.getContent();

			_log("headers: " + headers);

			String receiver = (String) headers.get("receiver");
			String toName = (String) headers.get("reply-to");
			_log("receiver = " +receiver+ "    reply-to: " + toName);
			
			
			MessagingName to = new MessagingName(toName);
			
			baseProcess.send(to, "op-reply", content);
		}
	}
	
	// from ProtoUtils.testSendReceive()
	private void _unpack(IonMessage reply) {
        System.out.println("\n------------------<_unpack>--------------------------------");
        System.out.println(" reply.getContent class = " +reply.getContent().getClass());
        StructureManager sm = StructureManager.Factory(reply);
        System.out.println(">>>> Heads:");
        for(String key : sm.getHeadIds()) {
            System.out.println(key);
            GPBWrapper<IonMsg> msgWrap = sm.getObjectWrapper(key);
            System.out.println(msgWrap);
            IonMsg msg = msgWrap.getObjectValue();
        }
        System.out.println("\n>>>> Items:");
        for(String key : sm.getItemIds()) {
            System.out.println(key);
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            System.out.println(demWrap);
            Command dem = demWrap.getObjectValue();
        }
        System.out.println("------------------</_unpack>--------------------------------\n");
	}
}