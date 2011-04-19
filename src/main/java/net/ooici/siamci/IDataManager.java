package net.ooici.siamci;

/**
 * Interface for an instrument data manager.
 * 
 * @author carueda
 */
public interface IDataManager {

    /**
     * Starts a data notifier (if not already running) corresponding to the
     * given parameters.
     * 
     * @param turbineName
     * @param reqId
     * @param publishId
     * @param publishStream
     * @throws Exception
     */
    public void startDataNotifier(String turbineName, int reqId,
            String publishId, String publishStream) throws Exception;

}