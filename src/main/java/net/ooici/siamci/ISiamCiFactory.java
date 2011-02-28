package net.ooici.siamci;


/**
 * SiamCi factory.
 * 
 * @author carueda
 */
public interface ISiamCiFactory {

	/**
	 * Gets the SIAM hight-level interface implementation
	 */
	public ISiam createSiam(String host) throws Exception;
	
	/**
	 * Gets the SIAM-CI adapter implementation.
	 */
	public ISiamCiAdapter createSiamCiAdapter(ISiam siam);
}
