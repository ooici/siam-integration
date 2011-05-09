package siam;

import java.util.List;
import java.util.Map;

/**
 * This is a corresponding asynchronous interface to the high-level API to
 * access SIAM functionality.
 * 
 * <p>
 * See <a
 * href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>
 * Note: this interface is preliminary.
 * 
 * <p>
 * In general, all methods expecting a callback argument will throw
 * IllegalArgumentException if such argument is null.
 * 
 * @author carueda
 */
public interface IAsyncSiam {

    /**
     * List the ports (ie., instruments) in the SIAM node.
     * 
     * @param callback
     *            Called with the result of the request
     * 
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     */
    public String listPorts(AsyncCallback<List<PortItem>> callback);

    /**
     * Requests the status of an instrument
     * 
     * @param port
     *            The port associated with the instrument
     * @param callback
     *            Called with the result of the request
     * 
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     */
    public String getPortStatus(String port, AsyncCallback<String> callback);

    /**
     * Requests the last sample from an instrument
     * 
     * @param port
     *            The port associated with the instrument
     * @param callback
     *            Called with the result of the request
     * 
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     */
    public String getPortLastSample(String port,
            AsyncCallback<Map<String, String>> callback);

    /**
     * Requests the channels associated with an instrument
     * 
     * @param port
     *            The port associated with the instrument
     * @param callback
     *            Called with the result of the request
     * 
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     */
    public String getPortChannels(final String port,
            final AsyncCallback<List<String>> callback);

    /**
     * Requests the properties of an instrument
     * 
     * @param port
     *            The port associated with the instrument
     * @param callback
     *            Called with the result of the request
     * 
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     */
    public String getPortProperties(String port,
            AsyncCallback<Map<String, String>> callback);

    /**
     * Sets the given properties/
     * 
     * @param port
     *            The port associated with the instrument
     * @param params
     *            the desired parameter/values
     * @param callback
     *            Called with the result of the request
     * @return null if the request was successfully submitted. Otherwise an
     *         error message
     * @throws Exception
     */
    public String setPortProperties(String port, Map<String, String> params,
            AsyncCallback<Map<String, String>> callback);

}
