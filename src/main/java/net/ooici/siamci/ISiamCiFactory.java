package net.ooici.siamci;

import siam.ISiam;


/**
 * SiamCi factory.
 * It provides the implementation of central interfaces.
 * 
 * @author carueda
 */
public interface ISiamCiFactory {

	/**
	 * Gets the SIAM hight-level interface implementation
	 */
	public ISiam createSiam(String host) throws Exception;
	
	/**
	 * Gets the request processor implementation.
	 * 
	 * @param siam interface to SIAM library
	 */
	public IRequestProcessor createRequestProcessor(ISiam siam);
	
	/**
	 * Gets the SIAM-CI adapter implementation.
	 * 
	 * @param siam interface to SIAM library
	 * @param brokerHost
	 * @param brokerPort
	 * @param queueName
	 * @param requestProcessor  processor
	 * @return adapter
	 */
	public ISiamCiAdapter createSiamCiAdapter(String brokerHost, int brokerPort, String queueName,
			IRequestProcessor requestProcessor);
}
