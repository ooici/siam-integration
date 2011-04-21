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
