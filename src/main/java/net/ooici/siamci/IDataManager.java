package net.ooici.siamci;

import java.util.concurrent.Future;

/**
 * Interface for an instrument data manager.
 * 
 * @author carueda
 */
public interface IDataManager {
    
    
    public boolean isDataNotifierCreated(String turbineName);

    /**
     * Creates a data notifiers.
     * 
     * @param turbineName
     * @throws Exception
     */
    public void createDataNotifier(String turbineName) throws Exception;

    /**
     * Starts the notifier for the given channel
     * 
     * @param turbineName
     * @return A future for the just submitted task; null if the notifier is
     *         already running
     * @throws Exception
     */
    public Future<?> startDataNotifier(String turbineName) throws Exception;

    /**
     * Adds a listener to the notifier running for the given channel.
     * 
     * @param turbineName
     * @param listener
     * @return true if the listener was added; false if there is no notifier for
     *         this manager of if this notifier is no active.
     */
    public boolean addDataListener(String turbineName, IDataListener listener);

}