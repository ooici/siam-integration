package net.ooici.siamci.impl.reqproc;

import java.util.Map;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.siamci.IDataManager;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.SiamUtils;

import com.google.protobuf.GeneratedMessage;

/**
 * Request processor for start and stop data acquisition.
 * 
 * TODO complete implementation (still preliminary)
 * 
 * @author carueda
 */
public class StartOrStopAcquisitionRequestProcessor extends
        BaseDataRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(StartOrStopAcquisitionRequestProcessor.class);
    
    private static final String CMD_NAME = "data_acquisition";

    private final boolean start;

    /**
     * Creates an instance for the specific behavior: start or stop data
     * acquisition
     * 
     * @param start
     *            true to start, false to stop acquisition
     */
    public StartOrStopAcquisitionRequestProcessor(boolean start) {
        super();
        this.start = start;
    }

    public GeneratedMessage processRequest(int reqId, Command cmd) {
        if (cmd.getArgsCount() == 0) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": command requires at least a 'port' argument";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        // port
        ChannelParameterPair cp = cmd.getArgs(0);
        if (!"port".equals(cp.getChannel())) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": first argument should be 'port'";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }
        final String port = cp.getParameter();

        // channel
        cp = cmd.getArgs(1);
        if (!"channel".equals(cp.getChannel())) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": channel argument should be 'channel'";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }
        final String channel = cp.getParameter();

        // publish_stream
        final String publishStream = ScUtils.getPublishStreamName(cmd);
        if (publishStream == null) {
            String msg = _rid(reqId)
                    + CMD_NAME
                    + ": no publish_stream, which is required for this operation";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        // properties
        Map<String, String> props = _getPortProperties(port);
        if (props == null) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": cannot retrieve properties for the instrument";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        String rbnbHost = SiamUtils.getRbnbHost(props);
        if (rbnbHost == null) {
            String msg = _rid(reqId)
                    + CMD_NAME
                    + ": no RBNB server associated with the instrument"
                    + (start ? "; cannot publish data"
                            : "; data is not being published");
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        String turbineName;
        try {
            turbineName = siam.getTurbineName(port, channel); 
        }
        catch (Exception e) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": error composing turbineName: " + e.getMessage();
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        // TODO some of these log.info call to become log.debug

        log.info(_rid(reqId) + "rbnbHost='" + rbnbHost + "' turbineName='"
                + turbineName + "'");

        GeneratedMessage response = _getAndPublishResult(reqId,
                rbnbHost,
                turbineName,
                port,
                channel,
                publishStream);
        return response;
    }

    /**
     * Does the asynchronous dispatch of this operation
     * 
     * @param reqId
     * @param rbnbHost
     * @param turbineName
     *            The full channel name associated with the DataTurbine RBNB
     *            server
     * @param port
     *            the SIAM port associated with the instrument
     * @param channel
     *            channel name
     * @param publishStream
     *            the queue (rounting key) to publish the data.
     * @return The {@link SuccessFail} result of the submission of the request.
     */
    private GeneratedMessage _getAndPublishResult(final int reqId,
            String rbnbHost, String turbineName, final String port,
            final String channel, final String publishStream) {

        _checkAsyncSetup();

        //
        // TODO more robust assignment of publish IDs
        //
        final String publishId = CMD_NAME + ";port=" + port + ";channel="
                + channel;

        IDataManager dataManager;

        if (start) {
            /*
             * create (if necessary) the data manager for the given rbnbHost and
             * instrument port
             */
            dataManager = dataManagers.createDataManagerIfAbsent(rbnbHost, port);

            if (dataManager == null) {
                String description = _rid(reqId) + "Cannot create data manager";
                log.warn(description);
                GeneratedMessage response = ScUtils.createFailResponse(description);
                return response;
            }

            try {
                dataManager.startDataNotifier(turbineName,
                        reqId,
                        publishId,
                        publishStream);
            }
            catch (Exception e) {
                String description = _rid(reqId)
                        + "Error while starting data notifier";
                log.warn(description, e);
                GeneratedMessage response = ScUtils.createFailResponse(description);
                return response;
            }

        }
        else {
            /*
             * get the data manager for the given rbnbHost and instrument port
             */
            dataManager = dataManagers.getDataManager(rbnbHost, port);
            if (dataManager == null) {
                String description = _rid(reqId)
                        + "Data acquisition not in progress";
                log.warn(description);
                GeneratedMessage response = ScUtils.createFailResponse(description);
                return response;
            }

            try {
                dataManager.stopDataNotifier(turbineName,
                        reqId,
                        publishId,
                        publishStream);
            }
            catch (Exception e) {
                String description = _rid(reqId)
                        + "Error while trying to stop data acquisition";
                log.warn(description, e);
                GeneratedMessage response = ScUtils.createFailResponse(description);
                return response;
            }

        }

        // respond with OK, ie., sucessfully submitted request:
        GeneratedMessage response = ScUtils.createSuccessResponse(null);
        return response;
    }
}
