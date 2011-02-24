package ion.example;

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
import net.ooici.play.instr.InstrumentDefs.ChannelParameterPair;
import net.ooici.play.instr.InstrumentDefs.Command;

/**
 * Adapted from some of ProtoUtils's tests.
 * See {@link IonSimpleEcho} for a way to run this test.
 * 
 * The output of this program looks so:
<pre>

TODO

</pre>
 */
public class EchoClientTest {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EchoClientTest.class);


    private EchoClientTest() {
    }


    public static void main(String[] args) {
        testSendReceive();
    }

    /**
     * As with ProtoUtil.testSendReceive but with some adjustments
     */
    private static void testSendReceive() {
        System.out.println("\n>>>>>>>>>>>>>>>>>Test Send/Receive<<<<<<<<<<<<<<<<<");
        
        System.out.println("\n******Make Test Structure******");
        /* Generate the test struct1 */
        Container.Structure structure = makeCommand1();
        System.out.println("\n*******************************\n\n");

        System.out.println("\n******Prepare MsgBrokerClient******");
        /* Send the message to the simple_responder service which just replys with the content of the sent message */
        MsgBrokerClient ionClient = new MsgBrokerClient("localhost", com.rabbitmq.client.AMQP.PROTOCOL.PORT, "magnet.topic");
        ionClient.attach();
        BaseProcess baseProcess = new BaseProcess(ionClient);
        baseProcess.spawn();


        System.out.println("\n******RPC Send******");
        MessagingName simpleResponder = new MessagingName("testing", "responder");
        IonMessage reply = baseProcess.rpcSendContainerContent(simpleResponder, "respond", structure);
//        IonMessage reply = baseProcess.rpcSendContainerContent(simpleResponder, "respond", structure, null);

        System.out.println("\n******Unpack Message******");
        Object content = reply.getContent();
        System.out.println(" reply.getContent class = " +content.getClass());
        if ( content instanceof String ) {
        	System.out.println("!!!!!!!! content is String -- using getBytes  !!!!!");
        	//
        	// NOTE: In my tests with IonSimpleEcho, it happens that the content is a String, not a byte[].
        	// Somehow, the underlying byte[] gets promoted to a String. 
        	// StructureManager.Factory(reply) expects this content to be byte[], so the following
        	// is to make sure the message has the byte[] for the content.
        	// This might be simply something like:
        	//     reply.setContent( ((String) content).getBytes() );
        	// but there is no setContent methid. So, create a "clone" of the reply but with the array of bytes
        	// corresponding to the String for the content:
        	Map headers = reply.getIonHeaders();
        	IonSendMessage reply2 = new IonSendMessage(
        			(String) headers.get("sender"), (String) headers.get("receiver"), (String) headers.get("op"),
        			((String) content).getBytes()
        	);
        	reply = reply2;
        }
        
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

        baseProcess.dispose();
    }
    private static Container.Structure makeCommand1() {
    	
    	Command command = Command.newBuilder()
			.setCommand("get")
			.addArgs(ChannelParameterPair.newBuilder().setChannel("ch1").setParameter("foo").build()
		).build();
		
		
        System.out.println("****** Generate message_objects******");
        GPBWrapper<Command> demWrap = GPBWrapper.Factory(command);
        System.out.println("Command:\n" + demWrap);

        
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
        System.out.println("IonMsg:\n" + msgWrap);

        /* Add the elements to the Container.Structure.Builder */
        Container.Structure.Builder structBldr = ProtoUtils.addStructureElementToStructureBuilder(null, msgWrap.getStructureElement(), true);
        ProtoUtils.addStructureElementToStructureBuilder(structBldr, demWrap.getStructureElement(), false);

        return structBldr.build();
    }

}
