package net.ooici.siamci;

import java.util.List;
import java.util.Map;


/**
 * This is a high-level API to access SIAM functionality.
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
}
