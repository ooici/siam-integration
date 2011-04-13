package net.ooici.siamci;

import siam.IAsyncSiam;
import net.ooici.play.InstrDriverInterface.Command;

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
     * Sets the object for publishing responses for asynchronous requests. Once
     * an asynchronous result is obtained from the {@link IAsyncSiam} object,
     * this {@link IPublisher} is used to send the result out.
     * 
     * @param publisher
     *            object to notify the result
     */
    public void setPublisher(IPublisher publisher);

    /**
     * Processes the given request.
     * 
     * @param reqId
     *            ID of the request
     * @param cmd
     *            The request (at this point, a command).
     * @return The resulting message.
     */
    public GeneratedMessage processRequest(int reqId, Command cmd);
}
