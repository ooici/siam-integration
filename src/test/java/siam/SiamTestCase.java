package siam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ooici.siamci.ISiam.PortItem;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import siamcitest.BaseTestCase;


/**
 * Tests for {@link Siam}.
 * 
 * Currently the tests are controlled by the environment variables SIAM_HOST and SIAM_INSTR_PORT
 * 
 * <ul>
 * <li>SIAM_HOST: SIAM host; all tests are skipped if not set
 * <li>SIAM_INSTR_PORT: SIAM instrument port; instrument specific tests are skipped if not set
 * </ul>
 * 
 * From maven, you can call:
 * <pre>
 *   SIAM_HOST=localhost SIAM_INSTR_PORT=testPort mvn test -Dtest=SiamTestCase
 * </pre>
 * 
 * @author carueda
 */
public class SiamTestCase extends BaseTestCase {
	
	/** SIAM host env variable; all tests are skipped if not set. */
	private static final String SIAM_HOST_PROP = "SIAM_HOST";

	/** SIAM instrument port env variable; instrument specific tests are skipped if not set. */
	private static final String SIAM_INSTRUMENT_PORT_PROP = "SIAM_INSTR_PORT";
	
	
	private final String siamHost = System.getenv(SIAM_HOST_PROP);
	private final String siamInstrumentPort = System.getenv(SIAM_INSTRUMENT_PORT_PROP);
	
	private Siam siam;
	
	
	private void _skipIfNotSiam() {
		skipIf(siam == null, "Siam instance needed when " +SIAM_HOST_PROP+ " is set");
	}

	private void _skipIfNotInstrument() {
		skipIf(siam == null || siamInstrumentPort == null, "Instrument port needed");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	void init() throws Exception {
		StringBuilder sb = new StringBuilder();
		if ( siamHost != null ) {
			sb.append(SIAM_HOST_PROP+ " = '" +siamHost+ "'; ");
			siam = new Siam(siamHost);
			
			if ( siamInstrumentPort != null ) {
				sb.append(SIAM_INSTRUMENT_PORT_PROP+ " = '" +siamInstrumentPort+ "'");
			}
			else {
				sb.append(SIAM_INSTRUMENT_PORT_PROP+ " not set so instrument tests will be skipped");
			}
		}
		else {
			sb.append(SIAM_HOST_PROP+ " not set so tests will be skipped");
		}
		System.out.println(SiamTestCase.class.getSimpleName()+ " (" +sb+ ")");
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
	 * @throws Exception 
	 */
	@Test
	public void testGetPortLastSample() throws Exception {
		_skipIfNotInstrument();
		Map<String, String> sample = siam.getPortLastSample(siamInstrumentPort);
		Assert.assertNotNull(sample);
		Assert.assertTrue(sample.containsKey("parentId"), "sample.containsKey parentId"); 
		
	}

	/**
	 * Test method for {@link siam.Siam#getPortProperties(java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void testGetPortProperties() throws Exception {
		_skipIfNotInstrument();
		Map<String, String> props = siam.getPortProperties(siamInstrumentPort);
		Assert.assertNotNull(props);
	}

	/**
	 * Test method for {@link siam.Siam#setPortProperties(java.lang.String, java.util.Map)}.
	 * 
	 * <p>
	 * TODO Should we add a timeout? sometimes siam.setPortProperties takes a few seconds (~20 sec) 
	 * 
	 * <p>
	 * TODO this test tries to set the property "startDelayMsec", which is specific to "testPort" instrument.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testSetPortProperties() throws Exception {
		_skipIfNotInstrument();
		Map<String, String> params = new HashMap<String, String>();
		params.put("startDelayMsec", "1000");
		Map<String, String> result = siam.setPortProperties(siamInstrumentPort, params);
		Assert.assertNotNull(result);
		Assert.assertEquals(result.get("startDelayMsec"), "OK"); 
	}

}
