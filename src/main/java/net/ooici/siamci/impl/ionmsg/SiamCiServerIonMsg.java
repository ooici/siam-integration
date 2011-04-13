package net.ooici.siamci.impl.ionmsg;

import ion.core.messaging.IonMessage;
import ion.core.messaging.MessagingName;
import ion.core.messaging.MsgBrokerClient;
import ion.core.utils.GPBWrapper;
import ion.core.utils.ProtoUtils;
import ion.core.utils.StructureManager;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ooici.core.container.Container;
import net.ooici.core.container.Container.Structure;
import net.ooici.core.message.IonMessage.IonMsg;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.SiamCiConstants;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * The actual SIAM-CI service.
 * 
 * In a nutshell, it accepts RPC requests on the queue
 * {@link SiamCiConstants#DEFAULT_QUEUE_NAME}, uses a {@link IRequestProcessors} object
 * to process the incoming requests, and replies the resulting response to the
 * routingKey indicated in the "reply-to" property of the request.
 * 
 * @author carueda
 */
class SiamCiServerIonMsg implements IPublisher, Runnable {

    private static final Logger log = LoggerFactory
            .getLogger(SiamCiServerIonMsg.class);


    private final String brokerHost;

    private final int brokerPort;

    /** TODO: use some parameter -- currently hard-coded for convenience */
    private static final String exchange = "magnet.topic";

    /**
     * Timeout for waiting for a request.
     */
    private static final long TIMEOUT = 3 * 1000;

    /** The queue this service is accepting requests at */
    private final String queueName;

    /** The 'from' parameter when replying to a request */
    private final MessagingName from;

    /** The processors for the requests */
    private final IRequestProcessors requestProcessors;

    private final MsgBrokerClient ionClient;

    private volatile boolean keepRunning;
    private volatile boolean isRunning;

    /** For the dispatch of requests */
    private final ExecutorService execService = Executors.newCachedThreadPool();

    /**
     * Creates the Siam-Ci service.
     * 
     * @param brokerHost
     * @param brokerPort
     * @param queueName
     * @param requestProcessor
     *            To process incoming requests.
     * @throws Exception
     *             if something bad happens
     */
    SiamCiServerIonMsg(String brokerHost, int brokerPort, String queueName,
            IRequestProcessors requestProcessors) throws Exception {

        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.queueName = queueName;
        this.from = new MessagingName(queueName);
        this.requestProcessors = requestProcessors;

        this.requestProcessors.setPublisher(this);

        if (log.isDebugEnabled()) {
            log.debug("Creating SiamCiProcess");
        }
        ionClient = new MsgBrokerClient(this.brokerHost, this.brokerPort,
                exchange);
        ionClient.attach();
        ionClient.declareQueue(queueName);

        ionClient.bindQueue(queueName, new MessagingName(queueName), null);
        ionClient.attachConsumer(queueName);
    }

    /**
     * Runs this Siam-Ci service.
     */
    public void run() {
        log.info("Running " + getClass().getSimpleName() + " (" + "broker='"
                + brokerHost + ":" + brokerPort + "'" + ", queue='" + queueName
                + "'," + " exchange='" + exchange + "'" + ")");
        keepRunning = true;
        isRunning = true;
        try {
            _run();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        finally {
            log.info("Ending " + getClass().getSimpleName());
            ionClient.detach();
            isRunning = false;
        }
    }

    /**
     * Requests that the service stop accepting further requests.
     */
    public void stop() {
        keepRunning = false;
    }

    boolean isRunning() {
        return isRunning;
    }

    private static String _rid(int reqId) {
        return ScUtils.formatReqId(reqId);
    }

    /**
     * The dispatch loop.
     * 
     * @throws InvalidProtocolBufferException
     */
    private void _run() throws InvalidProtocolBufferException {

        for (int reqId = 0; keepRunning; reqId++) {
            log.info(_rid(reqId) + "Waiting for request ...");

            IonMessage msgin = null;
            while (keepRunning
                    && null == (msgin = ionClient.consumeMessage(
                            queueName,
                            TIMEOUT))) {
                // timeout -- just try again
                //
                // Note: consumeMessage should throw an exception when there is
                // a non-timeout problem with getting the message. Email sent to
                // OOI folks about this (2011-03-22).
            }

            if (keepRunning && msgin != null) {
                _dispatchIncomingRequest(reqId, msgin);
            }
        }
    }

    /**
     * Dispatches the incoming request.
     * 
     * @param reqId
     *            An ID for the request
     * @param msgin
     *            the incoming message
     */
    private void _dispatchIncomingRequest(final int reqId,
            final IonMessage msgin) {

        //
        // Do some immediate steps in current thread, in particular
        // to show some basic info about the incoming request.
        //

        try {
            ionClient.ackMessage(msgin);
        }
        catch (Throwable e) {
            Map<String, String> headers = _getIonHeaders(msgin);
            log.warn(
                    _rid(reqId)
                            + "error while acknowledging message. headers = "
                            + headers,
                    e);
            return;
        }

        final Map<String, String> receivedHeaders = _getIonHeaders(msgin);
        final Command cmd = _getCommand(msgin);

        if (log.isDebugEnabled()) {
            log.debug(_rid(reqId) + "headers: " + receivedHeaders);
        }
        String sender = receivedHeaders.get("sender");
        String convId = receivedHeaders.get("conv-id");

        String cmdName = cmd.hasCommand() ? cmd.getCommand() : "?";

        log.info(_rid(reqId) + "Request <" + cmdName + "> received from '"
                + sender + "' conv-id="
                + (convId != null ? "'" + convId + "'" : "(not given)"));

        String toName = receivedHeaders.get("reply-to");
        if (toName == null) {
            // nobody to reply to?
            log.warn(_rid(reqId) + "NOT REPLYING as reply-to is null");
            return;
        }
        //
        // Dispatch remaining, potentially long-running part in a different
        // thread:
        //
        execService.submit(new Runnable() {
            public void run() {
                _doDispatchIncomingRequest(reqId, msgin, receivedHeaders, cmd);
            }
        });
    }

    private void _doDispatchIncomingRequest(final int reqId, IonMessage msgin,
            Map<String, String> receivedHeaders, final Command cmd) {
        if (log.isTraceEnabled()) {
            _unpack(reqId, msgin);
        }

        final String convId = receivedHeaders.get("conv-id");

        final String publishStreamName = ScUtils.getPublishStreamName(cmd);
        if (publishStreamName != null) {
            log.info(_rid(reqId) + "Command with publish stream name: '"
                    + publishStreamName + "'");
        }

        if (log.isDebugEnabled()) {
            log.debug(_showMessage(cmd, _rid(reqId) + "Command received:"));
        }

        GeneratedMessage response = _dispatchRequest(reqId, cmd);

        if (log.isDebugEnabled()) {
            log.debug(_showMessage(response, _rid(reqId)
                    + "Response to be replied:"));
        }

        if (log.isDebugEnabled()) {
            if (convId == null) {
                log
                        .debug(_rid(reqId)
                                + "Will reply but with no conv-id as it was not provided");
            }
        }

        //
        // 2011-03-22
        // user-id and expiry were not needed initially, but are now required
        // for proper handling on the python side.
        // Use the values in the received message, if given; set some value
        // otherwise.
        // TODO: these props should probably be handled in a more proper way.
        //
        String userId = receivedHeaders.get("user-id");
        String expiry = receivedHeaders.get("expiry");
        if (userId == null) {
            userId = "ANONYMOUS";
        }
        if (expiry == null) {
            expiry = "0";
        }

        final String toName = receivedHeaders.get("reply-to");

        // TODO what name and identity?
        Container.Structure.Builder structureBuilder = ProtoUtils
                .addIonMessageContent(null, "myName", "Identity", response);
        _sendReply(reqId, toName, convId, userId, expiry, structureBuilder
                .build());
        log
                .info(_rid(reqId) + "Reply sent to '" + toName + "' conv-id:'"
                        + convId + "' user-id:'" + userId + "' expiry:"
                        + expiry + "\n");
    }

    private GeneratedMessage _dispatchRequest(int reqId, Command cmd) {
        final String cmdName = cmd.getCommand();
        IRequestProcessor reqProc = requestProcessors
                .getRequestProcessor(cmdName);
        return reqProc.processRequest(reqId, cmd);
    }

    /**
     * Sends the given content (structure).
     * 
     * @param reqId
     * @param toName
     *            where the message is going
     * @param convId
     *            value for the "conv-id" header property; if null, no such
     *            property is set
     * @param userId
     * @param expiry
     * @param structure
     *            See
     *            ProtoUtils.addIonMessageContent(Container.Structure.Builder
     *            structure, String name, String identity, GeneratedMessage
     *            content)
     */
    private void _sendReply(int reqId, String toName, String convId,
            String userId, String expiry, Container.Structure structure) {

        MessagingName to = new MessagingName(toName);

        IonMessage msg = ionClient.createMessage(from, to, "noop", structure
                .toByteArray());

        Map<String, String> headers = _getIonHeaders(msg);

        if (convId != null) {
            headers.put("conv-id", convId);
        }
        headers.remove("accept-encoding");
        headers.put("encoding", "ION R1 GPB");

        headers.put("user-id", userId);
        headers.put("expiry", expiry);

        // set 'status' -- note that the following error message is printed on
        // the python side if this property is not set:
        // ERROR:RPC reply is not well formed. Header "status" must be set!
        //
        headers.put("status", "OK");

        ionClient.sendMessage(msg);

        if (log.isDebugEnabled()) {
            log.debug(_rid(reqId) + "headers of message sent: " + headers);
        }
    }

    /**
     * {@link IPublisher} operation.
     */
    public void publish(int reqId, String publishId, GeneratedMessage response,
            String streamName) {
        if (log.isDebugEnabled()) {
            log.debug(_rid(reqId) + "Publishing with publishId='" + publishId
                    + "' to queue='" + streamName + "'" + " reponse='"
                    + response + "'");
        }

        // TODO: "SomeName", "Identity": determine appropriate values.
        Structure structure = ProtoUtils.addIonMessageContent(
                null,
                "SomeName",
                "Identity",
                response).build();

        String toName = streamName;
        MessagingName to = new MessagingName(toName);

        IonMessage msg = ionClient.createMessage(
                from,
                to,
                "acceptResponse",
                structure.toByteArray());

        Map<String, String> headers = _getIonHeaders(msg);
        headers.remove("accept-encoding");
        headers.put("encoding", "ION R1 GPB");

        // TODO proper values for user-id, expiry
        headers.put("user-id", "ANONYMOUS");
        headers.put("expiry", "0");

        headers.put("status", "OK");

        // include the publish_id as a header:
        headers.put("publish_id", publishId);

        ionClient.sendMessage(msg);

        log.info(_rid(reqId) + "Publish message sent. publishId='" + publishId
                + "' to queue='" + streamName + "'");
    }

    /**
     * Returns a string with contents of the given message using a prefix for
     * each line, like so:
     * 
     * <pre>
     * --title goes here--
     * 		    class net.ooici.play.InstrDriverInterface$Command
     * 		    | command: "echo"
     * 		    | args {
     * 		    |   channel: "myCh1"
     * 		    |   parameter: "myParam1"
     * 		    | }
     * </pre>
     * 
     * @param msg
     * @param title
     * @return
     */
    private static String _showMessage(Message msg, String title) {
        final String prefix = "    | ";
        return " [x] " + title + "\n    " + msg.getClass() + "\n" + prefix
                + msg.toString().trim().replaceAll("\n", "\n" + prefix);
    }

    /** Utility to narrow the supress warning item */
    @SuppressWarnings("unchecked")
    private Map<String, String> _getIonHeaders(IonMessage msg) {
        Map<String, String> headers = msg.getIonHeaders();
        return headers;
    }

    /**
     * Extracts the Command from the message.
     */
    @SuppressWarnings("unchecked")
    private Command _getCommand(IonMessage reply) {
        StructureManager sm = StructureManager.Factory(reply);
        for (String key : sm.getItemIds()) {
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            Command cmd = demWrap.getObjectValue();
            return cmd;
        }
        return null;
    }

    // from ProtoUtils.testSendReceive()
    @SuppressWarnings("unchecked")
    private void _unpack(int reqId, IonMessage reply) {
        log.trace("\n" + _rid(reqId) + "------------------<_unpack>");
        log.trace(_rid(reqId) + " reply.getContent class = "
                + reply.getContent().getClass());
        StructureManager sm = StructureManager.Factory(reply);
        log.trace(">>>> Heads:");
        for (String key : sm.getHeadIds()) {
            log.trace(_rid(reqId) + "  headId = " + key);
            GPBWrapper<IonMsg> msgWrap = sm.getObjectWrapper(key);
            log.trace(_rid(reqId) + "  object wrapper = " + msgWrap);
            // IonMsg msg = msgWrap.getObjectValue();
        }
        log.trace("\n" + _rid(reqId) + ">>>> Items:");
        for (String key : sm.getItemIds()) {
            log.trace(_rid(reqId) + "  itemId = " + key);
            GPBWrapper<Command> demWrap = sm.getObjectWrapper(key);
            log.trace(_rid(reqId) + "  object wrapper = " + demWrap);
            // Command dem = demWrap.getObjectValue();
        }
        log.trace(_rid(reqId) + "------------------</_unpack>\n");
    }

    static {
        //
        // TODO, once ioncore-java starts using standard logging, eliminate the
        // following, which simply "redirects" System.out messages to a file.
        //
        final String outputFile = "stdout.txt";
        try {
            System.setOut(new PrintStream(outputFile));
        }
        catch (Throwable ex) {
            log.warn("Could not set system out to file " + outputFile, ex);
        }
    }
}
