package siam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import siam.InstrumentSample.SampleDatum;
import siamcitest.BaseTestCase;
import siamcitest.ScTestUtils;

/**
 * Tests for {@link Siam}.
 * 
 * Currently the tests are controlled by the environment variable SIAM, for
 * example, from maven, you can call:
 * 
 * <pre>
 *   SIAM="host=localhost port=testPort" mvn test -Dtest=SiamTestCase
 *   SIAM="-" mvn test -Dtest=SiamTestCase
 * </pre>
 * 
 * All tests are skipped if SIAM is not set.
 * 
 * @author carueda
 */
public class SiamTestCase extends BaseTestCase {

    /** SIAM env variable; all tests are skipped if not set. */
    private static final String SIAM_ENV_VAR = "SIAM";

    private static final String DEFAULT_SIAM_HOST = "localhost";
    private static final String DEFAULT_SIAM_INSTR_PORT = "testPort";

    private static final String MESSAGE_SKIPPED = SIAM_ENV_VAR
            + " environment variable not set.\n"
            + "If defined, the value of this variable is parsed for some properties and the tests are run.\n"
            + "Examples:\n"
            + "- With explicit values:\n"
            + "    SIAM=\"host="
            + DEFAULT_SIAM_HOST
            + ", port="
            + DEFAULT_SIAM_INSTR_PORT
            + "\"\n"
            + "- Using default values (values shown in the explicit value example above):\n"
            + "    SIAM=\"-\"\n"
            + "\n"
            + "The properties are:\n"
            + "  host: The host where the SIAM node program is running.\n"
            + "  port: the SIAM instrument port\n"
            + "\n"
            + "See https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+status"
            + "\n";

    private final String siamEnvVar = System.getenv(SIAM_ENV_VAR);

    private String siamHost = null;
    private String siamInstrumentPort = null;

    private Siam siam;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    void init() throws Exception {
        if (siamEnvVar == null) {
            System.out.println(SiamTestCase.class.getSimpleName()
                    + ": Tests will be skipped. " + MESSAGE_SKIPPED);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (siamEnvVar.equals("-")) {
            siamHost = DEFAULT_SIAM_HOST;
            siamInstrumentPort = DEFAULT_SIAM_INSTR_PORT;
        }
        else {
            Map<String, String> props = ScTestUtils.parseString(siamEnvVar);
            if (props.containsKey("host")) {
                siamHost = props.get("host");
                props.remove("host");
            }
            if (props.containsKey("port")) {
                siamInstrumentPort = props.get("port");
                props.remove("port");
            }
            if (props.size() > 0) {
                sb.append("warning: unrecognized keys in " + SIAM_ENV_VAR
                        + " environment variable: " + props + "\n");
            }
        }
        sb.append("SIAM host = '" + siamHost + "'; ");
        siam = new Siam(siamHost);

        if (siamInstrumentPort != null) {
            sb.append("instrument port = '" + siamInstrumentPort + "'");
        }
        else {
            sb.append("port not set so instrument tests will be skipped");
        }
        System.out.println(SiamTestCase.class.getSimpleName() + ": " + sb);
    }

    private void _skipIfNotSiam() {
        skipIf(siam == null, "Siam instance needed when " + SIAM_ENV_VAR
                + " is set");
    }

    private void _skipIfNotInstrument() {
        skipIf(siam == null || siamInstrumentPort == null,
                "Instrument port needed");
    }

    /**
     * Test method for {@link siam.Siam#getNodeId()}.
     */
    @Test
    public void testGetNodeId() {
        _skipIfNotSiam();
        siam.getNodeId();
    }

    /**
     * Test method for {@link siam.Siam#getNodeInfo()}.
     */
    @Test
    public void testGetNodeInfo() {
        _skipIfNotSiam();
        String info = siam.getNodeInfo();
        Assert.assertNotNull(info);
    }

    /**
     * Test method for {@link siam.Siam#listPorts()}.
     * 
     * @throws Exception
     */
    @Test
    public void testListPorts() throws Exception {
        _skipIfNotSiam();
        List<PortItem> ports = siam.listPorts();
        Assert.assertNotNull(ports);
    }

    /**
     * Test method for {@link siam.Siam#getPortStatus(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPortStatus() throws Exception {
        _skipIfNotInstrument();
        String status = siam.getPortStatus(siamInstrumentPort);
        Assert.assertNotNull(status);
    }

    /**
     * Test method for {@link siam.Siam#getPortLastSample(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPortLastSample() throws Exception {
        _skipIfNotInstrument();
        InstrumentSample sample = siam.getPortLastSample(siamInstrumentPort);
        Assert.assertNotNull(sample);
        Map<String, String> md = sample.getMd();
        Assert.assertTrue(md.containsKey("parentId"),
                "sample.containsKey parentId");

//        System.out.println(ScTestUtils.TC.yellow("\n md = " + md));
        for (SampleDatum datum : sample.getData()) {
            String name = datum.getName();
            String value = String.valueOf(datum.getValue());
            String units = datum.getUnits();

//            System.out.println(ScTestUtils.TC.yellow(String.format("\ndatum: '%s' = %s (%s)",
//                    name,
//                    value,
//                    units)));

            Assert.assertNotNull(name);
            Assert.assertNotNull(value);
            Assert.assertNotNull(units);
        }
    }

    /**
     * Test method for {@link siam.Siam#getPortChannels(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPortChannels() throws Exception {
        _skipIfNotInstrument();
        List<String> channels = siam.getPortChannels(siamInstrumentPort);
        Assert.assertNotNull(channels);
        Assert.assertTrue(channels.size() > 0, "channels list not empty");
        // System.out.println(" channels = " +channels);

    }

    /**
     * Test method for {@link siam.Siam#getPortProperties(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPortProperties() throws Exception {
        _skipIfNotInstrument();
        Map<String, String> props = siam.getPortProperties(siamInstrumentPort);
        Assert.assertNotNull(props);
    }

    /**
     * Test method for
     * {@link siam.Siam#setPortProperties(java.lang.String, java.util.Map)}.
     * 
     * <p>
     * TODO Should we add a timeout? sometimes siam.setPortProperties takes a
     * few seconds (~20 sec)
     * 
     * <p>
     * TODO this test tries to set the property "startDelayMsec", which is
     * specific to "testPort" instrument.
     * 
     * @throws Exception
     */
    @Test
    public void testSetPortProperties() throws Exception {
        _skipIfNotInstrument();
        Map<String, String> params = new HashMap<String, String>();
        params.put("startDelayMsec", "1000");
        Map<String, String> result = siam.setPortProperties(siamInstrumentPort,
                params);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.get("startDelayMsec"), "OK");
    }

}
