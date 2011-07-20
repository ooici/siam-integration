package net.ooici.siamci;

import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.ProtoUtils;

import java.io.IOException;
import java.util.Map;

import net.ooici.core.container.Container.Structure;
import net.ooici.play.InstrDriverInterface.SuccessFail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import siam.SiamTestCase;
import siamcitest.BaseTestCase;

import com.google.protobuf.GeneratedMessage;
import com.rabbitmq.client.AMQP;

/**
 * a quick and dirty test case based on {@link SiamCiReceiverTest} to replicate
 * the mechanism in
 * {@link SiamCiServerIonMsg#publish(int, String, GeneratedMessage, String)}.
 * 
 * See {@link SiamCiReceiverTest} for how to start first the SIAM-CI receiver
 * service on the python side. Then this test case can be run as follows:
 * 
 * <pre>
 * SIAM_CI_RECEIVER=- mvn test -Dtest=SiamCiReceiverTest -e
 * </pre>
 */
public class SiamCiReceiverTest2 extends BaseTestCase {

    private static final Logger log = LoggerFactory.getLogger(SiamCiReceiverTest2.class);

    /** SIAM_CI_RECEIVER env variable; all tests are skipped if not set. */
    private static final String ENV_VAR = "SIAM_CI_RECEIVER";

    private static final String MESSAGE_SKIPPED = ENV_VAR
            + " environment variable not set.\n"
            + "\n"
            + "You would first start the receiver service on the python side:\n"
            + "  cd python\n"
            + "  workon siamci\n"
            + "  bin/twistd --pidfile=ps1 -n cc -a sysname=siamci -h localhost src/siamci/start_receiver_service.py\n"
            + "\n" + "Then this test can be run as follows:\n"
            + "  SIAM_CI_RECEIVER=- mvn test -Dtest=SiamCiReceiverTest\n";

    private final String envVar = System.getenv(ENV_VAR);

    private MsgBrokerClient ionClient;

    private void _skipIfNotEnvVar() {
        skipIf(envVar == null, "Test requires that " + ENV_VAR + " be set");
    }

    @BeforeClass
    void init() throws Exception {
        if (envVar == null) {
            System.out.println(SiamTestCase.class.getSimpleName()
                    + ": Tests will be skipped. " + MESSAGE_SKIPPED);
            return;
        }
    }

    @BeforeTest
    void initInstance() {
        String hostName = "localhost";
        int portNumber = AMQP.PROTOCOL.PORT;
        String exchange = "magnet.topic";

        // Messaging environment
        ionClient = new MsgBrokerClient(hostName, portNumber, exchange);
        ionClient.attach();
    }

    private MessagingName mProcessId;
    private String mInQueue;

    private void spawn() {
        mProcessId = MessagingName.generateUniqueName();
        mInQueue = ionClient.declareQueue(null);
        ionClient.bindQueue(mInQueue, mProcessId, null);
        ionClient.attachConsumer(mInQueue);
        if (log.isDebugEnabled()) {
            log.debug("Spawned process " + mProcessId);
        }
    }

    /**
     * A test the replicates the mechanism used by
     * {@link SiamCiServerIonMsg#publish(int, String, GeneratedMessage, String)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testSimilarMechanismPublisher() throws IOException {
        _skipIfNotEnvVar();

        spawn();

        String publishId = "test-publish-id";
        String streamName = "siamci.siamci_receiver";

        SuccessFail someSuccessFail = SiamCiReceiverTest._createSomeSuccessFailObject();

        // TODO: "SomeName", "Identity": determine appropriate values.
        Structure structure = ProtoUtils.addIonMessageContent(null,
                "SomeName",
                "Identity",
                someSuccessFail).build();

        String toName = streamName;
        MessagingName to = new MessagingName(toName);

        MessagingName from = MessagingName.generateUniqueName();

        IonMessage msg = ionClient.createMessage(from,
                to,
                "acceptResponse",
                structure.toByteArray());

        Map<String, String> headers = _getIonHeaders(msg);
        headers.remove("accept-encoding");
        headers.put("encoding", "ION R1 GPB");

        // TODO proper values for user-id, expiry
        headers.put("user-id", "ANONYMOUS");

        headers.put("expiry", "0");

        headers.put("status", "OK");

        // include the publish_id as a header:
        headers.put("publish_id", publishId);

        log.info("Setting reply-to = " + mProcessId.getName());
        headers.put("reply-to", mProcessId.getName());
        headers.put("sender-name", mProcessId.getName());

        ionClient.sendMessage(msg, true, true);

        log.info("Waiting message on queue: " + mInQueue);
        IonMessage msgin = ionClient.consumeMessage(mInQueue);

        Object response = msgin;

        log.info("Response: " + response);

        Assert.assertNotNull(response);

    }

    /** Utility to narrow the supress warning item */
    @SuppressWarnings("unchecked")
    private Map<String, String> _getIonHeaders(IonMessage msg) {
        Map<String, String> headers = msg.getIonHeaders();
        return headers;
    }
}
