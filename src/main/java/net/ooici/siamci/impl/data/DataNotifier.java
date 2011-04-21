package net.ooici.siamci.impl.data;

import net.ooici.siamci.SiamCi;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * Notifies data from a RBNB channel to a publish_stream using a given
 * publisher.
 * 
 * Preliminary implementation, thread-safety is not guaranteed
 * 
 * @author carueda
 */
class DataNotifier implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DataNotifier.class);

    /**
     * Timeout while fetching for data. The timeout allows to periodically check
     * whether the main loop should finish (because of a call to
     * {@link #stop(String)}.
     */
    private static final long FETCH_TIMEOUT = 2 * 1000;

    private final String rbnbHost;
    private final String clientName;
    private final String turbineName;
    private final String prefix;

    private final int reqId;
    private final String publishId;
    private final String publishStream;

    private volatile boolean keepRunning;
    private volatile String stopReason;
    private volatile boolean isRunning = false;

    private volatile Sink rbnbSink = null;
    private final ChannelMap channelMap = new ChannelMap();

    /**
     * Creates an instance. This makes the connection to the RBNB server and
     * subscribes to the channel by the given turbineName.
     * 
     * @param rbnbHost
     *            The RBNB server host
     * @param clientName
     *            name for this RBNB client
     * @param turbineName
     *            qualified name of the RBNB channe
     * @param reqId
     *            Request ID, used to prefix some log information and as the
     *            corresp parameter in the to call the publisher
     * @param publishId
     *            the publish_id
     * @param publishStream
     *            the publish stream name
     * @throws Exception
     *             if something goes wrong, in particular around the RBNB API
     *             calls.
     */
    DataNotifier(String rbnbHost, String clientName, String turbineName,
            int reqId, String publishId, String publishStream) throws Exception {
        super();
        this.rbnbHost = rbnbHost;
        this.clientName = clientName;
        this.turbineName = turbineName;
        this.reqId = reqId;
        this.prefix = ScUtils.formatReqId(reqId)
                + String.format("{%s} ", this.turbineName);

        this.publishId = publishId;
        this.publishStream = publishStream;

        _prepare();
    }

    String getPublishStream() {
        return publishStream;
    }

    private void _prepare() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(prefix + "Connecting to " + rbnbHost + " ...");
        }
        rbnbSink = new Sink();
        rbnbSink.OpenRBNBConnection(rbnbHost, clientName);

        channelMap.Clear();
        channelMap.Add(turbineName);
        if (log.isDebugEnabled()) {
            log.debug(prefix + "Subscribing to channel ...");
        }
        rbnbSink.Subscribe(channelMap); // , 0, 10, "newest");
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Called when the execution has completed. This method does nothing in this
     * class.
     * 
     * @param reason
     *            null unless there has been a call to {@link #stop(String)}
     *            prior to the completion of the execution, in which case the
     *            string given to {@link #stop(String)} will be passed here.
     */
    protected void _completed(String reason) {
        // nothing here
    }

    /**
     * Requests that this process stop accepting further requests. It returns
     * immediately.
     * 
     * @param reason
     *            This string will be passed to {@link #_completed(String)}.
     */
    public void stop(String reason) {
        keepRunning = false;
        stopReason = reason;
    }

    /**
     * Start the loop of fetching data from RBNB and publishing the values
     */
    public void run() {
        stopReason = null;
        keepRunning = true;
        isRunning = true;
        try {
            _run();
        }
        finally {
            isRunning = false;
            // let subclass know execution completed:
            _completed(stopReason);
            if (rbnbSink != null) {
                rbnbSink.CloseRBNBConnection();
            }
            rbnbSink = null;
        }
    }

    private void _run() {

        while (keepRunning) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug(prefix + "Waiting for data ...");
                }

                ChannelMap getmap = null;

                while (keepRunning) {
                    getmap = rbnbSink.Fetch(FETCH_TIMEOUT, channelMap);
                    if (getmap != null && getmap.NumberOfChannels() > 0) {
                        break; // we have data.
                    }
                    else {
                        // surely a timeout -- just try again.
                        // if (log.isDebugEnabled()) {
                        // log.debug("XXXX timeout while fetching data");
                        // }
                    }
                }

                if (keepRunning) {
                    _dispatchGotData(getmap);
                }
            }
            catch (SAPIException e) {
                keepRunning = isRunning = false;
                log.warn(prefix + "Error fetching data: " + e.getMessage(), e);
            }
        }
    }

    private void _dispatchGotData(ChannelMap getmap) {
        int noChannels = getmap.NumberOfChannels();
        assert noChannels == 1; // should be 1 because we are subscribing to one
        // channel

        final int ch = 0;

        // double[] times = getmap.GetTimes(ch);
        // int type = getmap.GetType(ch);

        // value.length should be 1
        double[] values = getmap.GetDataAsFloat64(ch);
        if (values.length > 0) {
            double value = values[0];
            if (log.isDebugEnabled()) {
                log.debug(prefix + " -> " + value);
            }

            _publishData(turbineName, value);
        }
    }

    private void _publishData(String turbineName, double value) {
        if (log.isDebugEnabled()) {
            log.debug(prefix + "_publishData: '" + turbineName + "' = " + value);
        }

        GeneratedMessage response = ScUtils.createSuccessResponse("dataReceived = "
                + value);
        SiamCi.instance().getPublisher().publish(reqId,
                publishId,
                response,
                publishStream);
    }
}
