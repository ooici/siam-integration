package net.ooici.siamci;

import java.util.List;
import java.util.Map;


/**
 * This is a high-level API to access SIAM functionality.
 * 
 * <p> See <a href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>Note: this interface is very preliminary and can certainly be improved; 
 * it is just a quick basis for the prototype.
 * 
 * @author carueda
 */
public interface ISiam {

	public static class PortItem {
		public String portName;
		public long deviceId;
		public String serviceName;
		public String serviceStatus;
		public int sampleCount;
		public int errorCount;
		public int retryCount;
	}

	
	public List<PortItem> listPorts() throws Exception;
	
	public String getPortStatus(String port) throws Exception;
	
	public Map<String,String> getPortLastSample(String port) throws Exception;
	
	public Map<String,String> getPortProperties(String port) throws Exception;

	public Map<String, String> setPortProperties(String port, Map<String, String> params) throws Exception;
}
