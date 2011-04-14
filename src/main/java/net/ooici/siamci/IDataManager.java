package net.ooici.siamci;

import java.util.concurrent.Future;

/**
 * Interface for an instrument data manager.
 * 
 * @author carueda
 */
public interface IDataManager {

    /**
     * Is the notifier for the given channel already running?
     * 
     * @param channelName
     * @return
     * @throws Exception
     */
    public boolean isNotifierRunning(String channelName) throws Exception;

    /**
     * Starts the notifier for the given channel
     * 
     * @param channelName
     * @return A future for the just submitted task; null if the notifier is
     *         already running
     * @throws Exception
     */
    public Future<?> startDataNotifier(String channelName) throws Exception;

    /**
     * Adds a listener to the notifier running for the given channel.
     * 
     * @param channelName
     * @param listener
     * @return true is the listener was added; false if there is no notifier
     *         running
     */
    public boolean addDataListener(String channelName, IDataListener listener);

}