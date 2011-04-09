package net.ooici.siamci;

import siam.IAsyncSiam;
import net.ooici.play.InstrDriverInterface.Command;

import com.google.protobuf.GeneratedMessage;

/**
 * Processes requests. Used by the adapter implementation to process a request
 * from a client and provide corresponding response.
 * 
 * <p>
 * Currently, "request" actually refers to Command. The "request" concept is
 * convenient because is generic enough and provides flexibility while the
 * interface gets more stabilized.
 * 
 * @author carueda
 * 
 */
public interface IRequestProcessor {

	/**
	 * Interface for objects in charge of publishing responses for asynchronous
	 * requests.
	 * 
	 * @author carueda
	 */
	public interface IPublisher {

		/**
		 * Called to send a reponse, which has been requested to be published
		 * asynchronously.
		 * 
		 * @param response
		 *            The response to be published
		 * @param queue
		 *            The queue (rounting key) where the reponse should be
		 *            published.
		 */
		public void publish(GeneratedMessage response, String queue);

	}

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
	 * Processes the given request. If the request indicates an asynchronous
	 * response (ie., a "publish" argument is present in the request), then the
	 * object given by {@link #setAsyncSiam(IAsyncSiam)} will be used to notify
	 * the response (a RuntimeException will be thrown if such object has not
	 * been given).
	 * 
	 * <p>
	 * The returned object will be the actual response for the request if no
	 * asynchronous style has been indicated. Otherwise, the returned message
	 * will be a SuccessFailure status indicating a successful or unsuccessful
	 * submission of the request, and the actual response will be published to
	 * the queue indicated in the "publish" argument of the request.
	 * 
	 * @param cmd
	 *            The request (at this point, a command).
	 * @return The resulting message.
	 */
	public GeneratedMessage processRequest(Command cmd);
}
