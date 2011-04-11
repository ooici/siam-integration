package net.ooici.siamci;

/**
 * High-level SIAM-CI adapter service interface, basically to just start and
 * stop the service.
 * 
 * @author carueda
 */
public interface ISiamCiAdapter {

	/**
	 * Starts the adapter process.
	 */
	public void start() throws Exception;

	/**
	 * Requests that the adapter process terminate.
	 */
	public void stop();

}
