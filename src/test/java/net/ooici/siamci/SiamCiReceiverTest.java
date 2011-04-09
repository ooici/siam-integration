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
 * cd ioncore-python   # base directory of ioncore-pyton
 * workon by_buildout  # or your corresponding virtenv
 * bin/twistd --pidfile=ps1 -n cc -a sysname=siamci -h localhost ion/siamci/start_receiver_service.py
 * </pre>
 * 
 * (which assumes that ion/siam in ioncore-java is a symbolic link to
 * src/main/python/ion/siam in this project).
 * 
 * <p>
 * Then this test can be run as follows:
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
			+ "  cd ioncore-python   # base directory of ioncore-pyton\n"
			+ "  workon by_buildout  # or your corresponding virtenv\n"
			+ "  bin/twistd --pidfile=ps1 -n cc -a sysname=siamci -h localhost ion/siamci/start_receiver_service.py\n"
			+ "(which assumes that ion/siam in ioncore-java is a symbolic link to\n"
			+ "src/main/python/ion/siam in this project).\n" + "\n"
			+ "Then this test can be run as follows:\n"
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
	 * idea is that that service is actually receiving a response from a request
	 * originated by the SiamCiProxy class (on the the python side).
	 */
	@Test
	public void testSiamCiReceiver() {
		_skipIfNotEnvVar();

		String hostName = "localhost";
		int portNumber = AMQP.PROTOCOL.PORT;
		String exchange = "magnet.topic";

		// Messaging environment
		MsgBrokerClient ionClient = new MsgBrokerClient(hostName, portNumber,
				exchange);
		ionClient.attach();

		BaseProcess baseProcess = new BaseProcess(ionClient);
		baseProcess.spawn();

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);

		buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
				StringPair.newBuilder().setFirst("portName").setSecond(
						"some-port-name")));

		buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
				StringPair.newBuilder().setFirst("deviceId").setSecond(
						String.valueOf("some-device-id"))));

		Container.Structure.Builder structureBuilder = ProtoUtils
				.addIonMessageContent(null, "SomeName", "Identity", buildr
						.build());

		MessagingName mn = new MessagingName("siamci", "siamci_receiver");

		String userId = "ANONYMOUS";
		String expiry = String.valueOf(System.currentTimeMillis() + 30 * 1000);

		IonMessage msgin = baseProcess.rpcSendContainerContent(mn,
				"acceptResponse", structureBuilder.build(), userId, expiry);

		Object response = msgin.getContent();
		System.out.println("Response: " + response);

		Assert.assertNotNull(response);
	}
}
