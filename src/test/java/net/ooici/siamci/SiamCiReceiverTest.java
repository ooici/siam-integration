package net.ooici.siamci;

import ion.core.BaseProcess;
import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.ProtoUtils;
import net.ooici.core.container.Container;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.StringPair;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import siam.SiamTestCase;
import siamcitest.BaseTestCase;

import com.rabbitmq.client.AMQP;

/**
 * Tests the SIAM-CI receiver service on the python side. That service can be
 * started as follows:
 * 
 * <pre>
 * $ cd python      # base directory of the SIAM-CI python code
 *     $ workon siamci  # or your corresponding virtenv
 *     $ bin/twistd --pidfile=ps1 -n cc -a sysname=siamci -h localhost src/siamci/start_receiver_service.py
 *     2011-05-12 16:31:37-0700 [-] Log opened.
 *     2011-05-12 16:31:37-0700 [-] twistd 11.0.0 (/Users/carueda/ooici3/Dev/virtenvs/siamci/bin/python 2.5.4) starting up.
 *     2011-05-12 16:31:37-0700 [-] reactor class: twisted.internet.selectreactor.SelectReactor.
 *     2011-05-12 16:31:37.663 [start_receiver_service: 55] INFO :Starting siamci_receiver service ...
 *     ...
 *     ION Python Capability Container (version 0.4.13)
 *     [env: /Users/carueda/ooici3/Dev/virtenvs/siamci/lib/python2.5/site-packages] 
 *     [container id: carueda@carueda.local.53913] 
 *     
 *     ><> 2011-05-12 16:31:45.095 [receiver_service:111] WARNING:op_acceptResponse: publish_id not given
 * </pre>
 * 
 * The last line is generated when you run this test:
 * 
 * <pre>
 * SIAM_CI_RECEIVER=- mvn test -Dtest=SiamCiReceiverTest
 * </pre>
 * 
 * @author carueda
 */
public class SiamCiReceiverTest extends BaseTestCase {

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

    /**
     * It sends a message in RPC style (ie., with an expected reply), but the
     * general scheme is that that service is actually receiving a response from
     * a request originated by the SiamCiProxy class (on the the python side).
     */
    @Test
    public void testSiamCiReceiver() {
        _skipIfNotEnvVar();

        String hostName = "localhost";
        int portNumber = AMQP.PROTOCOL.PORT;
        String exchange = "magnet.topic";

        // Messaging environment
        MsgBrokerClient ionClient = new MsgBrokerClient(hostName,
                portNumber,
                exchange);
        ionClient.attach();

        BaseProcess baseProcess = new BaseProcess(ionClient);
        baseProcess.spawn();

        SuccessFail someSuccessFail = _createSomeSuccessFailObject();
        Container.Structure.Builder structureBuilder = ProtoUtils.addIonMessageContent(null,
                "SomeName",
                "Identity",
                someSuccessFail);

        MessagingName mn = new MessagingName("siamci.siamci_receiver");

        String userId = "ANONYMOUS";
        String expiry = String.valueOf(System.currentTimeMillis() + 30 * 1000);

        IonMessage msgin = baseProcess.rpcSendContainerContent(mn,
                "acceptResponse",
                structureBuilder.build(),
                userId,
                expiry);

        Object response = msgin.getContent();
        // System.out.println("Response: " + response);

        Assert.assertNotNull(response);
    }

    static SuccessFail _createSomeSuccessFailObject() {
        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);

        buildr.addItem(Item.newBuilder()
                .setType(Item.Type.PAIR)
                .setPair(StringPair.newBuilder()
                        .setFirst("portName")
                        .setSecond("some-port-name")));

        buildr.addItem(Item.newBuilder()
                .setType(Item.Type.PAIR)
                .setPair(StringPair.newBuilder()
                        .setFirst("deviceId")
                        .setSecond(String.valueOf("some-device-id"))));

        return buildr.build();
    }
}
