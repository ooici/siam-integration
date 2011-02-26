package net.ooici.siamci.impl.ionmsg;

import ion.core.BaseProcess;
import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;

import java.util.Map;

import net.ooici.siamci.SiamCiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
TODO
 * 
 * @author carueda
 */
class SiamCiServerIonMsg implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiServerIonMsg.class);
	

	private final String queueName = SiamCiConstants.DEFAULT_QUEUE_NAME;
	

	private final MsgBrokerClient ionClient;
	private final BaseProcess baseProcess;
	
	private volatile boolean keepRunning;

	SiamCiServerIonMsg() throws Exception {

		log.info("Creating SiamCiProcess");
		ionClient = new MsgBrokerClient("localhost", com.rabbitmq.client.AMQP.PROTOCOL.PORT, "magnet.topic");
		ionClient.attach();
		ionClient.attachConsumer(queueName);
		baseProcess = new BaseProcess(ionClient);
		baseProcess.spawn();
	}
	
	public void run() {
		log.info("Running SiamCiProcess");
		keepRunning = true;
		try {
			_run();
		}
		finally {
			log.info("Ending SiamCiProcess");
			ionClient.detach();
			baseProcess.dispose();
		}
	}

	/**
	 * The dispatch loop.
	 */
	private void _run() {
		
		while ( keepRunning ) {
			log.info("\n===================================================================\n" +
					"Waiting for message ..."
			);
			IonMessage msgin = ionClient.consumeMessage(queueName);

			ionClient.ackMessage(msgin);
			
			Map<?,?> headers = msgin.getIonHeaders();
			Object content = msgin.getContent();

			log.info("headers: " + headers);
			
			//
			// TODO implement actual logic.  
			// For the moment, just echoing the message..
			//

			String receiver = (String) headers.get("receiver");
			String toName = (String) headers.get("reply-to");
			log.info("receiver = " +receiver+ "    reply-to: " + toName);
			
			
			MessagingName to = new MessagingName(toName);
			
			baseProcess.send(to, "op-reply", content);
		}
	}

	public void stop() {
		keepRunning = false;
	}
}
