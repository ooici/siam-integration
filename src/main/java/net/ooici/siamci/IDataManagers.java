package net.ooici.siamci;

/**
 * Provides data manager instances.
 * 
 * @author carueda
 */
public interface IDataManagers {

    /**
     * Gets the data manager for the given parameters
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param baseClientName
     *            Base name for the RBNB client names (one client per channel in
     *            this first implementation)
     */
    public IDataManager getDataManager(String rbnbHost, String baseClientName);

}