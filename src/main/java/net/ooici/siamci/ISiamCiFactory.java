package net.ooici.siamci;

/**
 * SiamCi factory.
 * 
 * @author carueda
 */
public interface ISiamCiFactory {

	/**
	 * Gets the SIAM-CI adapter implementation.
	 */
	public ISiamCiAdapter createSiamCiAdapter();
}
