package siam;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ooici.siamci.ISiam;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.utils.PrintUtils;


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
	
    private static Logger _logger = Logger.getLogger(Siam.class);
    
    // Copied verbatim from NodeUtility
	private static final String getNodeURL(String input) {

		_logger.debug("getNodeURL(): input=" + input);

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
	
    /**
     * Creates a point of access to a SIAM node instance.
     * @param host
     * @throws NotBoundException 
     * @throws RemoteException 
     * @throws MalformedURLException 
     */
	public Siam(String host) throws MalformedURLException, RemoteException, NotBoundException{
		node = _getNode(host);
		nodeId = node.getId();
	}
	
	public long getNodeId() {
		return nodeId;
	}
	
	public List<PortItem> listPorts() throws Exception {
		return new PortLister(node).listPorts();
    }
	
	
	/**
	 * heloer
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
		
	}

}
