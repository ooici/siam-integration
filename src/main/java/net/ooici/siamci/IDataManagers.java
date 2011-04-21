package net.ooici.siamci;

/**
 * Provides data manager instances.
 * 
 * @author carueda
 */
public interface IDataManagers {

    /**
     * Creates the data manager for the given rbrnHost and clientName
     * parameters. If that object already exists, it is returned.
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param clientName
     *            Name for the RBNB client
     */
    public IDataManager createDataManagerIfAbsent(String rbnbHost,
            String clientName);

    /**
     * Gets the data manager for the given rbrnHost and clientName parameters.
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param clientName
     *            Name of the RBNB client
     */
    public IDataManager getDataManager(String rbnbHost, String clientName);

}