package net.ooici.siamci;

/**
 * Abstracts the operations for the SIAM-CI interaction.
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
