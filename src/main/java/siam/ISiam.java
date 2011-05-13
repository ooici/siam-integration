package siam;

import java.util.List;
import java.util.Map;

/**
 * This is a high-level API to access SIAM functionality.
 * 
 * <p>
 * See <a
 * href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>
 * Note: this interface is preliminary.
 * 
 * @author carueda
 */
public interface ISiam {

    /**
     * Call this first to enable the access to the SIAM node.
     * 
     * @throws Exception
     *             if some error occurs connecting
     * @throws RuntimeException
     *             if already connected
     */
    public void start() throws Exception;

    public long getNodeId();

    public String getNodeInfo();

    public List<PortItem> listPorts() throws Exception;

    public String getPortStatus(String port) throws Exception;

    public InstrumentSample getPortLastSample(String port) throws Exception;

    /**
     * @return the names of the channels for the given instrument.
     */
    public List<String> getPortChannels(String portName) throws Exception;

    public Map<String, String> getPortProperties(String port) throws Exception;

    public Map<String, String> setPortProperties(String port,
            Map<String, String> params) throws Exception;
}
