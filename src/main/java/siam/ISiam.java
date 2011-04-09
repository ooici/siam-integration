package siam;

import java.util.List;
import java.util.Map;


/**
 * This is a high-level API to access SIAM functionality.
 * 
 * <p> See <a href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>Note: this interface is very preliminary; it is just a quick basis for the prototype.
 * 
 * @author carueda
 */
public interface ISiam {

	public long getNodeId() ;
	
	public String getNodeInfo() ;
	
	public List<PortItem> listPorts() throws Exception;
	
	public String getPortStatus(String port) throws Exception;
	
	public Map<String,String> getPortLastSample(String port) throws Exception;
	
	public Map<String,String> getPortProperties(String port) throws Exception;

	public Map<String, String> setPortProperties(String port, Map<String, String> params) throws Exception;
}