package net.ooici.siamci;

import net.ooici.play.InstrDriverInterface.Command;
import siam.IAsyncSiam;

import com.google.protobuf.GeneratedMessage;

/**
 * Processes a particular type of request.
 * 
 * @author carueda
 */
public interface IRequestProcessor {

    /**
     * Sets the object for making asynchronous calls to SIAM.
     * 
     * @param asyncSiam
     *            object to make async calls to SIAM.
     */
    public void setAsyncSiam(IAsyncSiam asyncSiam);

    /**
     * Processes the given request. If the request indicates an asynchronous
     * response (ie., the "publish_stream" field is present in the request),
     * then the object given by {@link #setAsyncSiam(IAsyncSiam)} will be used
     * to notify the response (a RuntimeException will be thrown if such object
     * has not been given).
     * 
     * <p>
     * The returned object will be the actual response for the request if no
     * asynchronous style has been indicated. Otherwise, the returned message
     * will be a SuccessFailure status indicating a successful or unsuccessful
     * submission of the request, and the actual response will be published to
     * the queue indicated in the "publish_stream" field of the request.
     * 
     * @param reqId
     *            ID of the request
     * @param cmd
     *            The request (at this point, a command).
     * @return The resulting message.
     */
    public GeneratedMessage processRequest(int reqId, Command cmd);
}
