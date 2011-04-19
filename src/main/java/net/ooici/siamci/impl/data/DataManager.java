package net.ooici.siamci.impl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.ooici.siamci.IDataListener;
import net.ooici.siamci.IDataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mechanism to dispatch the notification of new data from RBNB channels to
 * registered listeners.
 * 
 * Very preliminary implementation, thread-safety is not guaranteed.
 * 
 * @author carueda
 */
public class DataManager implements IDataManager {

    private static final Logger log = LoggerFactory
            .getLogger(DataManager.class);

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
        log.info("instance created: " + this);
    }

    public boolean isDataNotifierCreated(String turbineName) {

        DataNotifier dataNotifier = _getDataNotifier(turbineName);
        return dataNotifier != null;
    }

    public void createDataNotifier(String turbineName) throws Exception {
        
        DataNotifier dataNotifier = _getDataNotifier(turbineName);
        if (dataNotifier != null) {
            log.warn("data notifier already created. turbineName='"
                    + turbineName + "'");
            return;
        }
        
        dataNotifier = _createDataNotifier(turbineName);
    }
    
    public Future<?> startDataNotifier(String turbineName) throws Exception {

        DataNotifier dataNotifier = _getDataNotifier(turbineName);
        if (dataNotifier == null) {
            throw new IllegalStateException("data notifier not created");
        }
        if (dataNotifier.isRunning()) {
            return null;  // data notifier is already running
        }

        Future<?> fut = execService.submit(dataNotifier);
        log.info("Notifier just started: " + turbineName);
        return fut;
    }

    public boolean addDataListener(String turbineName, IDataListener listener) {

        DataNotifier dataNotifier = _getDataNotifier(turbineName);
        if (dataNotifier == null) {
            return false;
        }

        return dataNotifier.addDataListener(listener);
    }

    private DataNotifier _getDataNotifier(String turbineName) {

        String key = baseClientName + "/" + turbineName;
        DataNotifier dataNotifier = dataNotifers.get(key);
        return dataNotifier;
    }

    private DataNotifier _createDataNotifier(String turbineName)
            throws Exception {

        String key = baseClientName + "/" + turbineName;
        DataNotifier dataNotifier = new DataNotifier(rbnbHost, baseClientName,
                turbineName);

        dataNotifers.put(key, dataNotifier);
        log.info("DataNotifier created: " + key);
        return dataNotifier;
    }

    public String toString() {
        return rbnbHost + "|" + baseClientName;
    }

}
