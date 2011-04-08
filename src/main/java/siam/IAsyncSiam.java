package siam;

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
 * Note: this interface is very preliminary; it is just a quick basis for the
 * prototype.
 * 
 * <p>
 * In general, all methods expeecing a callback argument will throw
 * IllegalArgumentException if such argument is null.
 * 
 * @author carueda
 */
public interface IAsyncSiam {

	/**
	 * Requests the status of an instrument
	 * 
	 * @param port
	 *            The port associated with the instrument
	 * @param callback
	 *            Called when the result of the request
	 * 
	 * @return null if the request was successfully submitted. Otherwise an
	 *         error message
	 */
	public String getPortStatus(String port, AsyncCallback<String> callback);

}
