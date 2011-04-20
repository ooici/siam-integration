package net.ooici.siamci.impl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ooici.siamci.IDataManager;
import net.ooici.siamci.IPublisher;
import net.ooici.siamci.event.EventMan;
import net.ooici.siamci.event.ReturnEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mycila.event.Event;
import com.mycila.event.Subscriber;

/**
 * Mechanism to dispatch the notification of new data from RBNB channels to
 * registered listeners.
 * 
 * Preliminary implementation, thread-safety is not guaranteed.
 * 
 * @author carueda
 */
public class DataManager implements IDataManager, Subscriber<ReturnEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    private final ExecutorService execService = Executors.newCachedThreadPool();

    /**
     * This map maintains the created DataNotifier objects, where the key is
     * given by {@link #_getNotifierKey(String, int, String, String)}. <br/>
     * key --> DataNotifier <br/>
     * Notifiers that complete execution are removed from this map.
     */
    private final Map<String, DataNotifier> dataNotifiers = new HashMap<String, DataNotifier>();

    private final String rbnbHost;
    private final String baseClientName;

    private final IPublisher publisher;

    /**
     * Creates a data notifier manager.
     * 
     * @param rbnbHost
     *            RBNB server host (and port)
     * @param baseClientName
     *            Base name for the RBNB client names (one client per channel in
     *            this first implementation)
     * @param publisher
     */
    DataManager(String rbnbHost, String baseClientName, IPublisher publisher) {
        super();
        this.rbnbHost = rbnbHost;
        this.baseClientName = baseClientName;
        this.publisher = publisher;

        EventMan.subscribe(ReturnEvent.class, this);

        log.info("instance created: " + this);
    }

    /**
     * {@link Subscriber} operation implementation.
     */
    public void onEvent(Event<ReturnEvent> event) throws Exception {
        ReturnEvent returnEvent = event.getSource();
        String routingKey = returnEvent.getRountingKey();

        if (log.isDebugEnabled()) {
            log.debug("Got ReturnEvent: routingKey='" + routingKey + "'");
        }

        _stopNotifiers(routingKey);
    }

    /**
     * Stops the notifiers associated with the given routingKey (ie.,
     * publish_stream).
     * 
     * @param routingKey
     */
    private void _stopNotifiers(String routingKey) {
        synchronized (dataNotifiers) {
            for (Entry<String, DataNotifier> e : dataNotifiers.entrySet()) {
                DataNotifier dn = e.getValue();
                if (routingKey.equals(dn.getPublishStream())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Stopping notifier key='" + e.getKey() + "'");
                    }
                    dn.stop();
                }
            }
        }
    }

    public void startDataNotifier(String turbineName, int reqId,
            String publishId, String publishStream) throws Exception {

        final String key = _getNotifierKey(turbineName,
                reqId,
                publishId,
                publishStream);

        synchronized (dataNotifiers) {
            DataNotifier dataNotifier = dataNotifiers.get(key);
            if (dataNotifier != null) {
                if (dataNotifier.isRunning()) {
                    if (log.isDebugEnabled()) {
                        log.debug("data notifier already created and running. key='"
                                + key + "'");
                    }
                    return;
                }
                else {
                    if (log.isDebugEnabled()) {
                        log.debug("re-running data notifier already created. key='"
                                + key + "'");
                    }
                }
            }
            else {
                dataNotifier = new DataNotifier(rbnbHost,
                        baseClientName,
                        turbineName,
                        reqId,
                        publishId,
                        publishStream,
                        publisher) {
                    @Override
                    protected void _completed() {
                        log.info("Notifier completed. Removing key='" + key
                                + "'");
                        _removeNotifier(key);
                    }
                };

                dataNotifiers.put(key, dataNotifier);
                log.info("DataNotifier created: key='" + key + "'");
            }
            execService.submit(dataNotifier);
            log.info("DataNotifier started: key='" + key + "'");
        }

    }

    private void _removeNotifier(String key) {
        synchronized (dataNotifiers) {
            dataNotifiers.remove(key);
        }
    }

    private String _getNotifierKey(String turbineName, int reqId,
            String publishId, String publishStream) {

        String key = reqId + "|" + turbineName + "|" + publishId + "|"
                + publishStream;
        return key;
    }

    public String toString() {
        return rbnbHost + "|" + baseClientName;
    }

}
