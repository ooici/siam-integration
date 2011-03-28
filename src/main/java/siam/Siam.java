package siam;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import net.ooici.siamci.ISiam;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.NodeInfo;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.utils.PrintUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the high-level API to access SIAM functionality.
 * 
 * <p> See <a href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>Note: this implementation is very preliminary and can certainly be improved; 
 * it is just a quick basis for the prototype.
 * 
 * @author carueda
 */
public class Siam implements ISiam {
	
    private static Logger log = LoggerFactory.getLogger(Siam.class);
    
    // Copied verbatim from NodeUtility
	private static final String getNodeURL(String input) {

		if ( log.isDebugEnabled() ) {
			log.debug("getNodeURL(): input=" + input);
		}

		if (input.startsWith("rmi://")) {
			return input;
		}
		else if (input.startsWith("//")) {
			return "rmi:" + input;
		}
		else {
			return "rmi://" + input + "/node";
		}
	}    
    
	private static Node _getNode(String host) throws MalformedURLException, RemoteException, NotBoundException {
		String nodeURL = getNodeURL(host);
		return (Node)Naming.lookup(nodeURL.toString());
	}
	
	
	///////////
	// Instance.
	///////////
	
	
    private final Node node;
    private final long nodeId;
    private final NodeInfo nodeInfo;
	
    /**
     * Creates a point of access to a SIAM node instance.
     * @param host
     * @throws NotBoundException 
     * @throws RemoteException 
     * @throws MalformedURLException 
     */
	public Siam(String host) throws Exception {
		node = _getNode(host);
		nodeId = node.getId();
		nodeInfo = node.getNodeInfo();
	}
	
	public long getNodeId() {
		return nodeId;
	}
	
	public String getNodeInfo() {
		return nodeInfo.toString();
	}
	
	public List<PortItem> listPorts() throws Exception {
		return new PortLister(node).listPorts();
    }
	
	
	/**
	 * helper
	 */
	private Instrument _getInstrument(String portName) throws Exception {
		try {
			Device device = node.getDevice(portName.getBytes());

			if (device instanceof Instrument) {
				Instrument instrument = (Instrument) device;
				return instrument;
			}
			else {
				throw new Exception("Device on port " +portName+ " is not an Instrument");
			}
		}
		catch (PortNotFound e) {
			throw new Exception("Port " + portName + " not found");
		}
		catch (DeviceNotFound e) {
			throw new Exception("Device not found on port " + portName);
		}
		catch (NoDataException e) {
			throw new Exception("No data from instrument on port " + portName);
		}
    }
	

	public String getPortStatus(String port) throws Exception {
		Instrument instrument = _getInstrument(port);
		return SiamUtils.statusMnem(instrument.getStatus());
	}
	
	/**
	 * Adapted from SIAM's GetLastSample
	 */
	public Map<String,String> getPortLastSample(String portName) throws Exception {
		Instrument instrument = _getInstrument(portName);
		SensorDataPacket sdp = instrument.getLastSample();
		HashMap<String, String> result = new HashMap<String,String>();
		result.put("parentId", String.valueOf(sdp.getParentId()));
		result.put("recordType", String.valueOf(sdp.getRecordType()));
		result.put("systemTime", String.valueOf(sdp.systemTime()));
		result.put("seqNo", String.valueOf(sdp.sequenceNo()));
		result.put("mdref", String.valueOf(sdp.metadataRef()));
		result.put("nBytes", String.valueOf(sdp.dataBuffer().length));
		result.put("buffer", PrintUtils.getAscii(sdp.dataBuffer(), 0, 0));
		
		return result;
    }
	
	/**
	 * Adapted from SIAM's PrintInstrumentProperties
	 */
	public Map<String,String> getPortProperties(String portName) throws Exception {
		Instrument instrument = _getInstrument(portName);
		
		HashMap<String, String> result = new LinkedHashMap<String,String>();
		@SuppressWarnings("unchecked")
		Vector<byte[]> properties = instrument.getProperties();
		for (int j = 0; j < properties.size(); j++) {
		    byte[] property = properties.elementAt(j);
		    String entry = new String(property);
		    String[] toks = entry.split("=", 2);
		    String key = toks[0].trim();
		    String value = toks.length == 2 ? toks[1].trim() : "??";
		    result.put(key, value);
		}
		
		return result;
	}
	
	/**
	 * Adapted from SIAM's SetInstrumentProperty
	 */
	public Map<String, String> setPortProperties(String portName, Map<String, String> params) throws Exception {
		Instrument instrument = _getInstrument(portName);
		
		StringBuilder sb = new StringBuilder();
		for ( Entry<String, String> e : params.entrySet() ) {
			sb.append(e.getKey()+ "=" +e.getValue()+ "\n");
		}
		String string = sb.toString();
		
		if ( log.isDebugEnabled() ) {
			log.debug("properties string: " +string);
		}
		
		HashMap<String, String> result = new LinkedHashMap<String,String>();
		
		// TODO: How to (easily) discriminate which properties are actually set and which are not?
		//
		instrument.setProperty(string.getBytes(), new byte[0]);
		
		// everything OK.  For each param, indicate "OK"
		for ( Entry<String, String> e : params.entrySet() ) {
			result.put(e.getKey(), "OK");
		}		
		if ( log.isDebugEnabled() ) {
			log.debug("Everything OK: " +result);
		}
		
		return result;
		
	}
	
	/**
	 *  test program:
	 *  @param args   [host  [port]]
	 */
	public static void main(String[] args) throws Exception {
		final String host = args.length >= 1 ? args[0] : "localhost";
		final String port = args.length >= 2 ? args[1] : "testPort";
		
		Siam siam = new Siam(host);
		
		PrintWriter out = new PrintWriter(System.out, true);
		
		
		///////////////////////////////////////////////////////////
		// listPorts:
		out.println("**listPorts:");
		List<PortItem> list = siam.listPorts();
		PortLister.list(siam.getNodeId(), list, out);
		out.println();
		
		
		///////////////////////////////////////////////////////////
		// getPortStatus:
		out.println("**getPortStatus: port=" +port);
		String status = siam.getPortStatus(port);
		out.println("   status: " +status);
		out.println();
		
		///////////////////////////////////////////////////////////
		// getPortLastSample:
		out.println("**getPortLastSample: port=" +port);
		Map<String, String> sample = siam.getPortLastSample(port);
		out.println("   result: " +sample);
		out.println();
		
		///////////////////////////////////////////////////////////
		// getPortLastSample:
		out.println("**getPortProperties: port=" +port);
		Map<String, String> portProps = siam.getPortProperties(port);
		out.println("   result: " +portProps);
		out.println();
		
	}

}
