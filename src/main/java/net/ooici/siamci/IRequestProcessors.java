package net.ooici.siamci;

import siam.IAsyncSiam;



/**
 * Provides {@link IRequestProcessor} instances.
 * 
 * @author carueda
 */
public interface IRequestProcessors {

	/**
	 * Sets the object for making asynchronous calls to SIAM.
	 * 
	 * @param asyncSiam
	 *            object to make async calls to SIAM.
	 */
	public void setAsyncSiam(IAsyncSiam asyncSiam);

	/**
	 * Sets the object for publishing responses for asynchronous requests. Once
	 * an asynchronous result is obtained from the {@link IAsyncSiam} object,
	 * this {@link IPublisher} is used to send the result out.
	 * 
	 * @param publisher
	 *            object to notify the result
	 */
	public void setPublisher(IPublisher publisher);
	
	/**
	 * Sets the given data managers object to thos processors that may use it.
	 * 
	 * @param dataManagers
	 */
	public void setDataManagers(IDataManagers dataManagers);
	
	/**
	 * Returns the request processor for the given request ID
	 * 
	 * @param id
	 *            ID of the desired processor
	 * @return the processor, never null.
	 * @throws IllegalArgumentException
	 *             If the ID is not recognized
	 */
	public IRequestProcessor getRequestProcessor(String id);

}
