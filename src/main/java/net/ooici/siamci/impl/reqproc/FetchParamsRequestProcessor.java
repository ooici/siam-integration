package net.ooici.siamci.impl.reqproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.StringPair;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;

import com.google.protobuf.GeneratedMessage;

/**
 * fetch_params processor.
 * 
 * @author carueda
 */
public class FetchParamsRequestProcessor extends BaseRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(FetchParamsRequestProcessor.class);

    private static final String CMD_NAME = "fetch_params";

    public GeneratedMessage processRequest(int reqId, Command cmd) {

        if (cmd.getArgsCount() == 0) {
            String msg = _rid(reqId) + CMD_NAME
                    + ": command requires at least an argument";
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
            Map<String, String> props = null;
            try {
                props = siam.getPortProperties(port);
            }
            catch (Exception e) {
                log.warn(_rid(reqId) + "getPortProperties exception", e);
                return ScUtils.createFailResponse(_rid(reqId)
                        + e.getClass().getName() + ": " + e.getMessage());
            }

            GeneratedMessage response = _createResultResponse(reqId, cmd, props);
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

        asyncSiam.getPortProperties(port,
                new AsyncCallback<Map<String, String>>() {

                    public void onSuccess(Map<String, String> result) {
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
     * Gets the final response based on the command and all the properties for
     * the instrument.
     * 
     * @param cmd
     *            the command
     * @param props
     *            all the instrument properties reported by SIAM
     * @return a response
     */
    private GeneratedMessage _createResultResponse(int reqId, Command cmd,
            Map<String, String> props) {

        boolean all_params_requested = false;

        if (cmd.getArgsCount() <= 1) {
            all_params_requested = true;
        }
        else {
            /*
             * all params requested also if the first pair is ('instrument',
             * 'all').
             */
            ChannelParameterPair cp = cmd.getArgs(1);
            final String ch = cp.getChannel();
            final String pr = cp.getParameter();

            if ("instrument".equals(ch) && "all".equals(pr)) {
                all_params_requested = true;
            }

            /*
             * If first pair determined that all params are requested, check
             * that there are NO more pairs:
             */
            if (all_params_requested && cmd.getArgsCount() > 2) {
                String error = _rid(reqId) + CMD_NAME
                        + ": since first pair was ('instrument', 'all'), "
                        + "no more pairs are expected: " + cmd.getArgs(2) + "'";
                log.warn(error);
                return ScUtils.createFailResponse(error);

            }
        }

        if (all_params_requested) {
            /*
             * ALL parameters are being requested.
             */
            Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
            for (Entry<String, String> es : props.entrySet()) {
                buildr.addItem(Item.newBuilder()
                        .setType(Item.Type.PAIR)
                        .setPair(StringPair.newBuilder()
                                .setFirst(es.getKey())
                                .setSecond(es.getValue())));
            }
            SuccessFail response = buildr.build();
            return response;
        }

        /*
         * Specific parameters are being requested. TODO Note that particular
         * channels are NOT yet dispatched.
         */

        // to collect the requested params:
        List<String> requestedParams = new ArrayList<String>();

        for (int i = 1; i < cmd.getArgsCount(); i++) {
            ChannelParameterPair cp = cmd.getArgs(i);
            final String ch = cp.getChannel();
            final String pr = cp.getParameter();

            if ("instrument".equals(ch)) {
                requestedParams.add(pr);
            }
            else {
                String error = _rid(reqId) + CMD_NAME
                        + ": first element in pair must be 'instrument'. "
                        + "Particular channel name is NOT yet implemented: '"
                        + ch + "'";
                log.warn(error);
                return ScUtils.createFailResponse(error);
            }

        }

        // now process the requested params:
        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
        for (String reqParam : requestedParams) {
            String value = props.get(reqParam);

            if (value == null) {
                /*
                 * TODO when the parameter does not exist, notify the
                 * corresponding error in an appropriate way. For now, returning
                 * an overall error response (even if other requested params are
                 * ok).
                 */
                String msg = _rid(reqId) + CMD_NAME + "Requested parameter '"
                        + reqParam + "' not recognized";
                log.warn(msg);
                return ScUtils.createFailResponse(msg);
            }

            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.PAIR)
                    .setPair(StringPair.newBuilder()
                            .setFirst(reqParam)
                            .setSecond(value)));
        }
        SuccessFail response = buildr.build();
        return response;

    }
}
