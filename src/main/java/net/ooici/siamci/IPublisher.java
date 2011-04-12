package net.ooici.siamci;

import com.google.protobuf.GeneratedMessage;

/**
 * Interface for objects in charge of publishing responses for asynchronous
 * requests.
 * 
 * @author carueda
 */
public interface IPublisher {

	/**
	 * Called to send a response, which has been requested to be published
	 * asynchronously.
	 * 
	 * @param publishId
	 *            ID to correlate the request and the response
	 * @param response
	 *            The response to be published
	 * @param streamName
	 *            The queue (rounting key) where the response should be
	 *            published.
	 */
	public void publish(String publishId, GeneratedMessage response,
			String streamName);

}