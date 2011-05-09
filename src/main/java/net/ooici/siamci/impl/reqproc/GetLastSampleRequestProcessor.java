package net.ooici.siamci.impl.reqproc;

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
import siam.InstrumentSample;
import siam.InstrumentSample.SampleDatum;

import com.google.protobuf.GeneratedMessage;

/**
 * get_last_sample processor.
 * 
 * @author carueda
 */
public class GetLastSampleRequestProcessor extends BaseRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(GetLastSampleRequestProcessor.class);

    private static final String CMD_NAME = "get_last_sample";

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
                    + ": command only accepts 'port' argument";
            log.warn(msg);
            return ScUtils.createFailResponse(msg);
        }
        final String port = cp.getParameter();

        final String publishStream = ScUtils.getPublishStreamName(cmd);
        if (publishStream != null) {
            // asynchronous handling.
            //
            GeneratedMessage response = _getAndPublishResult(reqId,
                    port,
                    publishStream);
            return response;
        }
        else {
            // synchronous response.
            //
            InstrumentSample sample;
            try {
                sample = siam.getPortLastSample(port);
            }
            catch (Exception e) {
                log.warn(_rid(reqId) + "getPortLastSample exception", e);
                return ScUtils.createFailResponse(_rid(reqId)
                        + e.getClass().getName() + ": " + e.getMessage());
            }

            GeneratedMessage response = _createResultResponse(reqId, sample);
            return response;
        }
    }

    /**
     * Does the asynchronous dispatch of the get port last sample operation.
     * 
     * @param port
     *            the SIAM port associated with the instrument
     * @param publishStream
     *            the queue (rounting key) to publish the response.
     * @return The {@link SuccessFail} result of the submission of the request.
     */
    private GeneratedMessage _getAndPublishResult(final int reqId,
            final String port, final String publishStream) {
        _checkAsyncSetup();

        //
        // TODO more robust assignment of publish IDs
        //
        final String publishId = CMD_NAME + ";port=" + port;

        asyncSiam.getPortLastSample(port,
                new AsyncCallback<InstrumentSample>() {

                    public void onSuccess(InstrumentSample result) {
                        GeneratedMessage response = _createResultResponse(reqId,
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
     * Creates an "ok" message with a list of pairs corresponding to the result.
     * TODO currently capturing this result in a {@link SuccessFail} instance;
     * should probably be a more appropiate GPB.
     */
    private GeneratedMessage _createResultResponse(int reqId,
            InstrumentSample sample) {

        Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);

        /*
         * report sample metadata NOTE: I'm prefixing these with "_" TODO A more
         * proper way to distinguish between metadata and data for a sample.
         */
        Map<String, String> result = sample.getMd();
        for (Entry<String, String> es : result.entrySet()) {
            String name = "_" + es.getKey(); // NOTE the "_" prefix.
            String value = es.getValue();
            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.PAIR)
                    .setPair(StringPair.newBuilder()
                            .setFirst(name)
                            .setSecond(value)));
        }

        /*
         * report sample data, if any
         */
        for (SampleDatum datum : sample.getData()) {
            String name = datum.getName();
            String value = String.valueOf(datum.getValue());
            buildr.addItem(Item.newBuilder()
                    .setType(Item.Type.PAIR)
                    .setPair(StringPair.newBuilder()
                            .setFirst(name)
                            .setSecond(value)));
        }

        SuccessFail response = buildr.build();
        return response;
    }

}
