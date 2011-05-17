package siam;

import java.util.List;
import java.util.Map;

import net.ooici.siamci.IDataManager;

import org.mbari.siam.core.BaseInstrumentService;

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

    /**
     * Gets the full name for the requested instrument's channel in a form that
     * is compliant with the "turbineName" used by SIAM. See
     * {@link BaseInstrumentService#run()}. This name is in particular used for
     * data acquisition operations. See {@link IDataManager}.
     * 
     * <p>
     * The returned name is composed as follows: <br/>
     * {@code serviceName.replace(' ', '_') + "-" + isiID} <br/>
     * where serviceName and isiID are the value of the corresponding properties
     * 'serviceName' and 'isiID'.
     * 
     * @param portName
     *            The instrument's port
     * @param channelName
     *            The name of desired channel in the given instrument
     * @return the "turbine" name for the channel
     * @throws Exception
     *             if any of the required properties is missing.
     */
    public String getTurbineName(String portName, String channelName)
            throws Exception;

}
