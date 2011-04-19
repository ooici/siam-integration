package net.ooici.siamci.impl.data;

import net.ooici.siamci.IPublisher;
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
 * Very preliminary implementation, thread-safety is not guaranteed
 * 
 * @author carueda
 */
class DataNotifier implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DataNotifier.class);

    private final String rbnbHost;
    private final String clientName;
    private final String turbineName;
    private final String prefix;

    final int reqId;
    final String publishId;
    final String publishStream;

    private final IPublisher publisher;

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
            int reqId, String publishId, String publishStream,
            IPublisher publisher) throws Exception {
        super();
        this.rbnbHost = rbnbHost;
        this.clientName = clientName;
        this.turbineName = turbineName;
        this.reqId = reqId;
        this.prefix = ScUtils.formatReqId(reqId)
                + String.format("{%s} ", this.turbineName);

        // //////////
        this.publishId = publishId;
        this.publishStream = publishStream;
        this.publisher = publisher;
        // //////////

        _prepare();
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
     * Called when the execution has completed.
     */
    protected void _completed() {
        // nothing here
    }

    /**
     * Start the loop of fetching data from RBNB and publishing the values
     */
    public void run() {
        isRunning = true;
        try {
            _run();
        }
        finally {
            isRunning = false;
            _completed();
            if (rbnbSink != null) {
                rbnbSink.CloseRBNBConnection();
            }
            rbnbSink = null;
        }
    }

    private void _run() {

        while (isRunning) {
            try {
                log.info(prefix + "Waiting for data ...");
                //
                // TODO the following blocks until data is fetched or error;
                // change
                // to include a timeout so it is easier to gracefully stop the
                // loop.
                //
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
                        if (log.isDebugEnabled()) {
                            log.debug(prefix + " -> " + value);
                        }

                        _publishData(turbineName, value);

                        /*
                         * TODO overall control for publishing data including
                         * termination if there is nobody receiving the
                         * notifications.
                         */

//                        /*
//                         * For now, we 'break' here, that is, just send a single
//                         * publish so the basic test on the python side can
//                         * complete.
//                         */
//                        log.info(prefix
//                                + "NOTE: ONLY one publish done at the moment, so completing loop.");
//                        break;
                    }
                }
            }
            catch (SAPIException e) {
                isRunning = false;
                log.warn(prefix + "Error fetching data: " + e.getMessage(), e);
            }
        }
    }

    private void _publishData(String fullChannelName, double value) {
        log.info(prefix + "_publishData: '" + fullChannelName + "' = " + value);

        GeneratedMessage response = ScUtils.createSuccessResponse("dataReceived = "
                + value);
        publisher.publish(reqId, publishId, response, publishStream);
    }
}
