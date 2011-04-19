package net.ooici.siamci.impl.reqproc;

import java.util.Map;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.siamci.IDataListener;
import net.ooici.siamci.IDataManager;
import net.ooici.siamci.utils.ScUtils;

import org.mbari.siam.core.BaseInstrumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

/**
 * StartAcquisition command processor.
 * 
 * TODO NOT IMPLEMENTED YET ! (still preparing supporting stuff, eg, data
 * management)
 * 
 * @author carueda
 */
public class StartAcquisitionRequestProcessor extends BaseDataRequestProcessor {

    private static final Logger log = LoggerFactory
            .getLogger(StartAcquisitionRequestProcessor.class);

    //
    // TODO Command naming is rather ad hoc at the moment
    //
    private static final String CMD_NAME = "execute_StartAcquisition";

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

        String rbnbHost = _getRbnbHost(props);
        if (rbnbHost == null) {
            String msg = _rid(reqId)
                    + CMD_NAME
                    + ": no RBNB server associated with the instrument; cannot publish data";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        String turbineName;
        try {
            turbineName = _getTurbineName(props, channel);
        }
        catch (Exception e) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": error composing full channel name: " + e.getMessage();
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        // TODO some of these log.info call to become log.debug

        log.info(_rid(reqId) + "rbnbHost='" + rbnbHost + "' fullChannelName='"
                + turbineName + "'");

        GeneratedMessage response = _getAndPublishResult(
                reqId,
                rbnbHost,
                turbineName,
                port,
                channel,
                publishStream);
        return response;
    }

    private Map<String, String> _getPortProperties(String port) {
        try {
            Map<String, String> props = siam.getPortProperties(port);
            return props;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the value of the "publisherHost" property.
     * 
     * @param port
     *            port identifying the instrument
     * @return the value, or null if it is not associated or cannot be retrieved
     *         due to some error.
     */
    private String _getRbnbHost(Map<String, String> props) {
        String rbnbHost = props.get("publisherHost");
        if (rbnbHost != null && rbnbHost.trim().length() > 0) {
            return rbnbHost.trim();
        }
        else {
            return null;
        }
    }

    /**
     * Gets the full name for the requested channel in a form compliant with the
     * "turbineName" used by SIAM. See {@link BaseInstrumentService#run()}. This
     * name is composed as follows: <br/> {@code serviceName.replace(' ', '_') + "-"
     * + isiID} <br/>
     * where serviceName and isiID are the value of the corresponding properties
     * 'serviceName' and 'isiID'.
     * 
     * @param props
     *            the properties obtained from the intrument.
     * @return the "dataTurbine" name
     * @throws Exception
     *             if any of the required properties is missing.
     */
    private String _getTurbineName(Map<String, String> props, String channelName)
            throws Exception {
        String serviceName = props.get("serviceName");
        String isiID = props.get("isiID");

        if (serviceName == null || serviceName.trim().length() == 0) {
            throw new Exception(
                    "'serviceName' property no associated with instrument");
        }
        if (isiID == null || isiID.trim().length() == 0) {
            throw new Exception(
                    "'isiID' property no associated with instrument");
        }
        serviceName = serviceName.trim();
        isiID = isiID.trim();

        String turbineName = serviceName.replace(' ', '_') + "-" + isiID + "/"
                + channelName;
        return turbineName;

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
     *            the queue (rounting key) to publish the response.
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

        // get the data manager for the given rbnbHost and instrument port
        IDataManager dataManager = dataManagers.getDataManager(rbnbHost, port);

        assert dataManager != null;

        if (!dataManager.isDataNotifierCreated(turbineName)) {
            try {
                dataManager.createDataNotifier(turbineName);
            }
            catch (Exception e) {
                String description = _rid(reqId) + "Error while  data notifier";
                log.warn(description, e);
                GeneratedMessage response = ScUtils
                        .createFailResponse(description);
                return response;
            }
        }

        // add data listener
        IDataListener dataListener = new DataListener(reqId, publishId,
                publishStream);
        boolean added = dataManager.addDataListener(turbineName, dataListener);
        log.info(_rid(reqId) + "Listener added: " + added);

        // start data notifier for the channel
        try {
            log.info(_rid(reqId) + "starting data notifier");
            Object res = dataManager.startDataNotifier(turbineName);
            if (res != null) {
                log.info(_rid(reqId) + "Data notifier started");
            }
            else {
                log.info(_rid(reqId) + "Data notifier already running");
            }
        }
        catch (Exception e) {
            String description = _rid(reqId)
                    + "Error while starting data notifier";
            log.warn(description, e);
            GeneratedMessage response = ScUtils.createFailResponse(description);
            return response;
        }

        // respond with OK, ie., sucessfully submitted request:
        GeneratedMessage response = ScUtils.createSuccessResponse(null);
        return response;
    }

    class DataListener implements IDataListener {

        final int reqId;
        final String publishId;
        final String publishStream;

        /*
         * TODO do actual publish of data to ION. For now, we just send a single
         * publish so the basic test on the python side can complete, during
         * this preliminary set-up. Overall control TBD.
         */
        boolean publishDone = false;

        DataListener(int reqId, String publishId, String publishStream) {
            this.reqId = reqId;
            this.publishId = publishId;
            this.publishStream = publishStream;
        }

        public void dataReceived(String fullChannelName, double value) {
            log.info(_rid(reqId) + "dataReceived: '" + fullChannelName + "' = "
                    + value);

            if (!publishDone) {
                publishDone = true;
                GeneratedMessage response = ScUtils
                        .createSuccessResponse("dataReceived = " + value);
                publisher.publish(reqId, publishId, response, publishStream);
            }
        }

    }
}
