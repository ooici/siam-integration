package net.ooici.siamci;

/**
 * High-level SIAM-CI adapter service interface, basically to just start and
 * stop the service.
 * 
 * @author carueda
 */
public interface ISiamCiAdapter {

    /**
     * Starts the adapter process.
     */
    public void start() throws Exception;

    /**
     * Requests that the adapter process terminate.
     */
    public void stop();

    /**
     * Gets the publisher associated this this adapter service.
     * 
     * <p>
     * NOTE: Should be called after {@link #start()}, otherwise null may be
     * returned.
     * 
     * @return the publisher associated this this adapter service.
     */
    public IPublisher getPublisher();

}
