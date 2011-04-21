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
