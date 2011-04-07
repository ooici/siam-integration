package siam;

public class PortItem {
	public String portName;
	public long deviceId;
	public String serviceName;
	public String serviceStatus;
	public int sampleCount;
	public int errorCount;
	public int retryCount;
	
	public String toString() {
		return portName;
	}
}