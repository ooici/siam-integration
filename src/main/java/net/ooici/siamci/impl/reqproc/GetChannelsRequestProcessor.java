package net.ooici.siamci.impl.reqproc;

import java.util.List;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;

import com.google.protobuf.GeneratedMessage;

/**
 * Gets names of the channels associated with an instrument.
 * 
 * @author carueda
 */
public class GetChannelsRequestProcessor extends BaseRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(GetChannelsRequestProcessor.class);

    private static final String CMD_NAME = "get_channels";

    public GeneratedMessage processRequest(int reqId, Command cmd) {
        
        if (cmd.getArgsCount() == 0) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": command requires at least the 'port' argument";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }
        ChannelParameterPair cp = cmd.getArgs(0);
        if (!"port".equals(cp.getChannel())) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": first argument must be 'port'";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }
        final String port = cp.getParameter();

        if (cmd.getArgsCount() > 1) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": command only expects one argument";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }

        final String publishStream = ScUtils.getPublishStreamName(cmd);
        if (publishStream != null) {
            /*
             * asynchronous handling.
             */
            GeneratedMessage response = _getAndPublishResult(reqId,
                    cmd,
                    port,
                    publishStream);
            return response;
        }
        else {
            /*
             * synchronous response.
             */
            List<String> channels = null;
            try {
                channels = siam.getPortChannels(port);
            }
            catch (Exception e) {
                log.warn(_rid(reqId) + "getPortChannels exception", e);
                return ScUtils.createFailResponse(_rid(reqId)
                        + e.getClass().getName() + ": " + e.getMessage());
            }

            GeneratedMessage response = _createResultResponse(reqId,
                    cmd,
                    channels);
            return response;
        }
    }

    /**
     * Does the asynchronous dispatch of the get properties operation.
     * 
     * @param cmd
     *            the original command
     * @param port
     *            the SIAM port associated with the instrument
     * @param publishStream
     *            the queue (rounting key) to publish the response.
     * @return The {@link SuccessFail} result of the submission of the request.
     */
    private GeneratedMessage _getAndPublishResult(final int reqId,
            final Command cmd, final String port, final String publishStream) {
        _checkAsyncSetup();

        /*
         * TODO more robust assignment of publish IDs
         */
        final String publishId = CMD_NAME + ";port=" + port;

        asyncSiam.getPortChannels(port, new AsyncCallback<List<String>>() {

            public void onSuccess(List<String> result) {
                GeneratedMessage response = _createResultResponse(reqId,
                        cmd,
                        result);

                _getPublisher().publish(reqId,
                        publishId,
                        response,
                        publishStream);
            }

            public void onFailure(Throwable e) {
                GeneratedMessage response = ScUtils.createFailResponse(e.getClass()
                        .getName()
                        + ": " + e.getMessage());

                _getPublisher().publish(reqId,
                        publishId,
                        response,
                        publishStream);
            }
        });

        // respond with OK, ie., sucessfully submitted request:
        GeneratedMessage response = ScUtils.createSuccessResponse(null);
        return response;
    }

    /**
     * Gets the final response based on the command and all the channels for the
     * instrument.
     * 
     * @param cmd
     *            the command
     * @param channels
     *            all the channels reported by SIAM
     * @return a response
     */
    private GeneratedMessage _createResultResponse(int reqId, Command cmd,
            List<String> channels) {

        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);

        for (String channel : channels) {
            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.STR)
                    .setStr(channel));
        }
        SuccessFail response = buildr.build();
        return response;

    }
}
