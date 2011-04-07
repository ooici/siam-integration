package siam;

import java.io.PrintWriter;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.List;


import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author carueda
 */
public class PortLister {
	
    private static Logger log = LoggerFactory.getLogger(PortLister.class);
    
    private Node node;
    
	PortLister(Node node) {
		this.node = node;
	}
	
	public List<PortItem> listPorts() throws Exception {
		Port[] ports = node.getPorts();
		if (ports == null) {
			throw new Exception("Got NULL ports from node");
		}
		
		List<PortItem> list = new ArrayList<PortItem>();
		for (int i = 0; i < ports.length; i++) {
			Port port = ports[i];
			
			PortItem pi = new PortItem();
			
			pi.portName=new String(port.getName());
			pi.serviceName= new String(port.getServiceMnemonic());
			
			pi.deviceId = port.getDeviceID();
			Device device = null;
			try {
				device = node.getDevice(pi.deviceId);
				pi.serviceStatus = SiamUtils.statusMnem(device.getStatus());
				pi.sampleCount= device.getSamplingCount();
				pi.errorCount= device.getSamplingErrorCount();
				pi.retryCount= device.getSamplingRetryCount();
			}
			catch (DeviceNotFound dnf) {
				log.warn("device by id " +pi.deviceId+ " not found", dnf);
			}
			catch (UnmarshalException ex) {
				// most likely the instrument jar is not in the classpath. 
				String msg = "UnmarshalException with device by id '" +pi.deviceId+ "'. " +
					"Is the instrument jar in the classpath?";
				if ( log.isDebugEnabled() ) {
					log.debug(msg, ex);
				}
				else {
					log.warn(msg);
				}
			}
			catch (Exception ex) {
				log.warn("Error with device by id " +pi.deviceId, ex);
			}
			
			list.add(pi);
		}
		
		return list;
    }
	

	// helper for list()
	private static String _str(String str) {
		return str != null ? str : "-";
	}
	
	/**
	 * Utility to print port information.
	 * 
	 * <p>This is similar to what the original listPorts utility does but not all features
	 * are replicated.
	 * 
	 * @param list  the list of port items
	 * @param out   where info is printed to
	 * @throws Exception
	 */
	public static void list(long nodeId, List<PortItem> list, PrintWriter out) throws Exception {
		out.println("Node " + nodeId + " has " + list.size()+ " ports\n");
		out.printf("%10s %10s %20s %10s %10s %10s %10s%n", 
				"port name",
				"device id",
				"service",
				"Status",
				"sample",
				"error",
				"retry"
		);
		for ( PortItem pi : list ) {
			out.printf("%10s %10d %20s %10s %10d %10d %10d%n", 
					_str(pi.portName),
					pi.deviceId,
					_str(pi.serviceName),
					_str(pi.serviceStatus),
					pi.sampleCount,
					pi.errorCount,
					pi.retryCount
			);
		}
	}
	
}

