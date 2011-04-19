package net.ooici.siamci;

/**
 * Provides data manager instances.
 * 
 * @author carueda
 */
public interface IDataManagers {

    /**
     * Gets the data manager for the given rbrnHost and baseClientName
     * parameters. Note, the publisher is not used to differentiate the data
     * manager.
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param baseClientName
     *            Base name for the RBNB client names (one client per channel in
     *            this first implementation)
     * @param publisher
     */
    public IDataManager getDataManager(String rbnbHost, String baseClientName,
            IPublisher publisher);

}