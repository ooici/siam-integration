package net.ooici.siamci.ion;

import ion.core.BaseProcess;
import ion.core.messaging.IonMessage;
import ion.core.messaging.IonSendMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.ProtoUtils;
import ion.core.utils.StructureManager;

import java.util.Map;

import net.ooici.core.container.Container;
import net.ooici.core.message.IonMessage.IonMsg;
import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;

/**
 * Simple simulation of the ION side to send messages to the SiamCiAdapter
 * 
 * @author carueda
 */
public class IonMock {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IonMock.class);


    private IonMock() {
    }


    public static void main(String[] args) {
        testSendReceive();
    }

    /**
     * As with ProtoUtil.testSendReceive but with some adjustments
     */
    private static void testSendReceive() {
        log.info("\n>>>>>>>>>>>>>>>>>Test Send/Receive<<<<<<<<<<<<<<<<<");
        
        log.info("\n******Make Test Structure******");
        /* Generate the test struct1 */
        Container.Structure structure = makeCommand1();
        log.info("\n*******************************\n\n");

        log.info("\n******Prepare MsgBrokerClient******");
        /* Send the message to the simple_responder service which just replys with the content of the sent message */
        MsgBrokerClient ionClient = new MsgBrokerClient("localhost", com.rabbitmq.client.AMQP.PROTOCOL.PORT, "magnet.topic");
        ionClient.attach();
        BaseProcess baseProcess = new BaseProcess(ionClient);
        baseProcess.spawn();


        log.info("\n******RPC Send******");
        MessagingName simpleResponder = new MessagingName("testing", "siam-ci");
        IonMessage reply = baseProcess.rpcSendContainerContent(simpleResponder, "respond", structure);
//        IonMessage reply = baseProcess.rpcSendContainerContent(simpleResponder, "respond", structure, null);

        log.info("\n******Unpack Message******");
        Object content = reply.getContent();
        log.info(" reply.getContent class = " +content.getClass());
        if ( content instanceof String ) {
        	log.info("!!!!!!!! content is String -- using getBytes  !!!!!");
        	//
        	// NOTE: In my tests with IonSimpleEcho, it happens that the content is a String, not a byte[].
        	// Somehow, the underlying byte[] gets promoted to a String. 
        	// StructureManager.Factory(reply) expects this content to be byte[], so the following
        	// is to make sure the message has the byte[] for the content.
        	// This might be simply something like:
        	//     reply.setContent( ((String) content).getBytes() );
        	// but there is no setContent methid. So, create a "clone" of the reply but with the array of bytes
        	// corresponding to the String for the content:
        	Map<?,?> headers = reply.getIonHeaders();
        	IonSendMessage reply2 = new IonSendMessage(
        			(String) headers.get("sender"), (String) headers.get("receiver"), (String) headers.get("op"),
        			((String) content).getBytes()
        	);
        	reply = reply2;
        }
        
        StructureManager sm = StructureManager.Factory(reply);
        log.info(">>>> Heads:");
        for(String key : sm.getHeadIds()) {
        	log.info(key);
            GPBWrapper<IonMsg> msgWrap = sm.getObjectWrapper(key);
            log.info("msgWrap= " +msgWrap);
//            IonMsg msg = msgWrap.getObjectValue();
        }
        log.info("\n>>>> Items:");
        for(String key : sm.getItemIds()) {
        	log.info(key);
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            log.info("demWrap= " +demWrap);
//            Command dem = demWrap.getObjectValue();
        }

        baseProcess.dispose();
    }
    private static Container.Structure makeCommand1() {
    	
    	Command command = Command.newBuilder()
			.setCommand("get")
			.addArgs(ChannelParameterPair.newBuilder().setChannel("ch1").setParameter("foo").build()
		).build();
		
		
    	log.info("****** Generate message_objects******");
        GPBWrapper<Command> demWrap = GPBWrapper.Factory(command);
        log.info("Command:\n" + demWrap);

        
        // Head is an IonMsg
        IonMsg.Builder ionMsgBldr = IonMsg.newBuilder().setName("Test Message").setIdentity("1");
        //
        // note: type is not a member of IonMsg anymore
        // (try "git diff 1133c22db4..4a8fa097f53f net/ooici/core/message/ion_message.proto" in ion-object-definitions)
        // ionMsgBldr.setType(demWrap.getObjectType());
        //
        /* This object references the dem object via a CASRef */
        ionMsgBldr.setMessageObject(demWrap.getCASRef());
        GPBWrapper<Command> msgWrap = GPBWrapper.Factory(ionMsgBldr.build());
        log.info("IonMsg:\n" + msgWrap);

        /* Add the elements to the Container.Structure.Builder */
        Container.Structure.Builder structBldr = ProtoUtils.addStructureElementToStructureBuilder(null, msgWrap.getStructureElement(), true);
        ProtoUtils.addStructureElementToStructureBuilder(structBldr, demWrap.getStructureElement(), false);

        return structBldr.build();
    }

}
