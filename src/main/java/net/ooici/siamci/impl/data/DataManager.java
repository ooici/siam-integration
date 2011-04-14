package net.ooici.siamci.impl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.ooici.siamci.IDataListener;
import net.ooici.siamci.IDataManager;

/**
 * Mechanism to dispatch the notification of new data from RBNB channels to
 * registered listeners.
 * 
 * Very preliminary implementation, thread-safety is not guaranteed.
 * 
 * @author carueda
 */
public class DataManager implements IDataManager {

    private final ExecutorService execService = Executors.newCachedThreadPool();

    private Map<String, DataNotifier> dataNotifers = new HashMap<String, DataNotifier>();

    private final String rbnbHost;
    private final String baseClientName;

    /**
     * Creates a data notifier manager.
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param baseClientName
     *            Base name for the RBNB client names (one client per channel in
     *            this first implementation)
     */
    public DataManager(String rbnbHost, String baseClientName) {
        super();
        this.rbnbHost = rbnbHost;
        this.baseClientName = baseClientName;
    }

    public synchronized boolean isNotifierRunning(String channelName)
            throws Exception {

        DataNotifier dataNotifier = _getDataNotifier(channelName);
        return dataNotifier != null && dataNotifier.isRunning();

    }

    public synchronized Future<?> startDataNotifier(String channelName)
            throws Exception {

        DataNotifier dataNotifier = _getDataNotifier(channelName);
        if (dataNotifier != null && dataNotifier.isRunning()) {
            return null;
        }

        dataNotifier = _createDataNotifier(channelName);
        Future<?> fut = execService.submit(dataNotifier);
        return fut;
    }

    public synchronized boolean addDataListener(String channelName,
            IDataListener listener) {

        DataNotifier dataNotifier = _getDataNotifier(channelName);
        if (dataNotifier != null && dataNotifier.isRunning()) {
            return false;
        }

        return dataNotifier.addDataListener(listener);
    }

    private DataNotifier _getDataNotifier(String channelName) {

        String key = baseClientName + "/" + channelName;
        DataNotifier dataNotifier = dataNotifers.get(key);
        return dataNotifier;
    }

    private DataNotifier _createDataNotifier(String channelName) throws Exception {

        String key = baseClientName + "/" + channelName;
        DataNotifier dataNotifier = new DataNotifier(rbnbHost, baseClientName,
                channelName);
        return dataNotifers.put(key, dataNotifier);
    }

}
