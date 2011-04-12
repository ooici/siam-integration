package net.ooici.siamci;

import siam.IAsyncSiam;
import siam.ISiam;

/**
 * SiamCi factory. It provides the implementation of central interfaces.
 * 
 * @author carueda
 */
public interface ISiamCiFactory {

	/**
	 * Gets the SIAM hight-level interface implementation
	 */
	public ISiam createSiam(String host) throws Exception;

	/**
	 * Gets the SIAM hight-level asynchronous interface implementation
	 */
	public IAsyncSiam createAsyncSiam(ISiam siam) throws Exception;


	/**
	 * Gets the request processors implementation.
	 * 
	 * @param siam
	 *            interface to SIAM library
	 */
	public IRequestProcessors createRequestProcessors(ISiam siam);

	
	/**
	 * Gets the SIAM-CI adapter implementation.
	 * 
	 * @param siam
	 *            interface to SIAM library
	 * @param brokerHost
	 * @param brokerPort
	 * @param queueName
	 * @param requestDispatcher
	 * @return adapter
	 */
	public ISiamCiAdapter createSiamCiAdapter(String brokerHost,
			int brokerPort, String queueName,
			IRequestProcessors requestProcessors);
}
