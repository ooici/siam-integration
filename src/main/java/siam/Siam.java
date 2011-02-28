package siam;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import net.ooici.siamci.ISiam;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Node;


/**
 * Implementation of the high-level API to access SIAM functionality.
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
	 *  test program
	 */
	public static void main(String[] args) throws Exception {
		String host = args.length == 0 ? "localhost" : args[0];
		Siam siam = new Siam(host);
		
		PrintWriter out = new PrintWriter(System.out, true);
		
		
		///////////////////////////////////////////////////////////
		// listPorts:
		out.println("listPorts:");
		List<PortItem> list = siam.listPorts();
		PortLister.list(siam.getNodeId(), list, out);
	}

}
