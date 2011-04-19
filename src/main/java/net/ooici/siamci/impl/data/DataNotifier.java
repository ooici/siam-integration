package net.ooici.siamci.impl.data;

import java.util.LinkedHashSet;
import java.util.Set;

import net.ooici.siamci.IDataListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * Notifies data from a RBNB channel to a set of listeners.
 * 
 * Very preliminary implementation, thread-safety is not guaranteed
 * 
 * @author carueda
 */
class DataNotifier implements Runnable {

    private static final Logger log = LoggerFactory
            .getLogger(DataNotifier.class);

    private final String rbnbHost;
    private final String clientName;
    private final String turbineName;
    private final String prefix;

    private final Set<IDataListener> listeners = new LinkedHashSet<IDataListener>();

    private volatile boolean active = false;
    private volatile boolean isRunning = false;
    private volatile Sink rbnbSink = null;
    private final ChannelMap channelMap = new ChannelMap();

    /**
     * Creates an instance
     * 
     * @param rbnbHost
     * @param clientName
     * @param turbineName
     * @throws Exception
     */
    DataNotifier(String rbnbHost, String clientName, String turbineName)
            throws Exception {
        super();
        this.rbnbHost = rbnbHost;
        this.clientName = clientName;
        this.turbineName = turbineName;
        this.prefix = String.format("[%s] ", this.turbineName);
        
        log.info(prefix + "instance created.");

        _prepare();
    }

    private void _prepare() throws Exception {
        log.info(prefix + "Connecting to " + rbnbHost + " ...");
        rbnbSink = new Sink();
        rbnbSink.OpenRBNBConnection(rbnbHost, clientName);

        channelMap.Clear();
        channelMap.Add(turbineName);
        log.info(prefix + "Subscribing to channel ...");
        rbnbSink.Subscribe(channelMap); // , 0, 10, "newest");
        
        active = true;
    }

    public boolean addDataListener(IDataListener listener) {
        log.info("Adding listener: " +listener);
        if (!active) {
            return false;
        }
        listeners.add(listener);
        return true;
    }

    private void _notifyListeners(double value) {
        log.info("Notifying " +listeners.size()+ " listeners");
        for (IDataListener listener : listeners) {
            listener.dataReceived(turbineName, value);
        }
    }
    
    public void run() {
        isRunning = true;
        try {
            _run();
        }
        finally {
            active = false;
            isRunning = false;
        }
    }

    private void _run() {
        while (rbnbSink != null) {
            try {
                log.info(prefix + "Waiting for data ...");
                ChannelMap getmap = rbnbSink.Fetch(-1, channelMap);
                if (getmap == null || getmap.NumberOfChannels() == 0) {
                    log.warn(prefix + "No data fetched");
                    continue;
                }

                // should be 1 because we are subscribing to one channel
                int noChannels = getmap.NumberOfChannels();

                if (noChannels > 0) {
                    final int ch = 0;

                    // double[] times = getmap.GetTimes(ch);
                    // int type = getmap.GetType(ch);

                    // value.length should be 1
                    double[] values = getmap.GetDataAsFloat64(ch);
                    if (values.length > 0) {
                        double value = values[0];

                        log.info(prefix + " -> " + value);

                        _notifyListeners(value);
                    }
                }
            }
            catch (SAPIException e) {
                log.warn(prefix + "Error fetching data: " + e.getMessage(), e);
                if (rbnbSink != null) {
                    rbnbSink.CloseRBNBConnection();
                }
                rbnbSink = null;
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

}
