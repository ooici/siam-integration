package net.ooici.siamci;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.ooici.siamci.SiamCi.SiamCiParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.ISiam;
import siam.PortItem;
import siam.SiamUtils;

/**
 * Main SiamCi program. For usage, see {@link #main(String[])}.
 * 
 * @author carueda
 */
public class SiamCiMain {

    private static final Logger log = LoggerFactory.getLogger(SiamCiMain.class);

    /**
     * Gets the parameters for the program.
     */
    private static class Params {
        private static final String DEFAULT_SIAM_NODE_HOST = "localhost";
        private static final String DEFAULT_BROKER_HOST = "localhost";
        private static final int DEFAULT_BROKER_PORT = com.rabbitmq.client.AMQP.PROTOCOL.PORT;
        private static final String DEFAULT_QUEUE_NAME = SiamCiConstants.DEFAULT_QUEUE_NAME;
        private static final String DEFAULT_ION_EXCHANGE = SiamCiConstants.DEFAULT_ION_EXCHANGE;

        private static final OptionParser parser = new OptionParser();

        // siam
        private static final OptionSpec<String> siamHostOpt = parser.accepts("siam",
                "SIAM node host")
                .withRequiredArg()
                .describedAs("host")
                .defaultsTo(DEFAULT_SIAM_NODE_HOST);

        // broker
        private static final OptionSpec<String> brokerHostOpt = parser.accepts("brokerHost",
                "Broker host")
                .withRequiredArg()
                .describedAs("host")
                .defaultsTo(DEFAULT_BROKER_HOST);
        private static final OptionSpec<String> brokerPortOpt = parser.accepts("brokerPort",
                "Broker port")
                .withRequiredArg()
                .describedAs("port")
                .defaultsTo(String.valueOf(DEFAULT_BROKER_PORT));

        // queue
        private static final OptionSpec<String> queueNameOpt = parser.accepts("queue",
                "Queue name")
                .withRequiredArg()
                .describedAs("name")
                .defaultsTo(DEFAULT_QUEUE_NAME);

        // ION exchange
        private static final OptionSpec<String> exchangeNameOpt = parser.accepts("exchange",
                "ION exchange name")
                .withRequiredArg()
                .describedAs("name")
                .defaultsTo(DEFAULT_ION_EXCHANGE);

        private static final OptionSpec<String> helpOpt = parser.acceptsAll(Arrays.asList("help",
                "?"),
                "print help message")
                .withOptionalArg();

        /**
         * Gets the parameters for the program by parsing the given arguments
         * 
         * @param args
         *            the arguments
         * @return the params; null, if the help option was given
         */
        static Params getParams(String[] args) {
            OptionSet options = parser.parse(args);
            if (options.has(helpOpt)) {
                System.out.println("SIAM-CI Adapter Service options:");
                try {
                    parser.printHelpOn(System.out);
                }
                catch (IOException e) {
                    // should not happen
                    e.printStackTrace();
                }
                return null;
            }
            return new Params(options);
        }

        SiamCiParameters siamCiParams;

        private Params(OptionSet options) {
            siamCiParams = new SiamCiParameters(options.valueOf(siamHostOpt),
                    options.valueOf(brokerHostOpt),
                    Integer.parseInt(options.valueOf(brokerPortOpt)),
                    options.valueOf(queueNameOpt),
                    options.valueOf(exchangeNameOpt));
        }
    }

    /**
     * Launches the SIAM-CI adapter program.
     * 
     * @param args
     *            Here is the usage message: (mvn exec:java -Dsiam-ci
     *            -Dexec.args=--help)
     * 
     *            <pre>
     * SIAM-CI Adapter Service options:
     *         Option                                  Description                            
     *         ------                                  -----------                            
     *         -?, --help                              print help message                     
     *         --brokerHost &lt;host>                     Broker host (default: localhost)       
     *         --brokerPort &lt;port>                     Broker port (default: 5672)            
     *         --exchange &lt;name>                       ION exchange name (default: magnet.    
     *                                                   topic)                               
     *         --queue &lt;name>                          Queue name (default: SIAM-CI)          
     *         --siam &lt;host>                           SIAM node host (default: localhost)
     * </pre>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // for debugging/logging purposes
        Thread.currentThread().setName(SiamCiMain.class.getSimpleName());

        Params params = Params.getParams(args);
        if (params != null) {
            new SiamCiMain(params.siamCiParams)._start();
        }
    }

    // /////////////////////////////////////////////////////////
    // instance.
    // /////////////////////////////////////////////////////////

    private SiamCiParameters siamCiParams;

    /**
     * Creates the main object of the SIAM-CI adapter.
     * 
     * @param siamCiParams
     *            the application parameters
     * @throws Exception
     */
    private SiamCiMain(SiamCiParameters siamCiParams) throws Exception {
        this.siamCiParams = siamCiParams;
        SiamCi.createInstance(siamCiParams);
    }

    /**
     * Starts the service.
     */
    private void _start() throws Exception {
        _showServiceParameter();
        _startComponents();
        _setShutdownHook();
        _showPorts();
        _startAdapter();
    }

    private void _showServiceParameter() {
        log.info("Parameters: " + siamCiParams);

    }

    private void _startComponents() throws Exception {
        SiamCi.instance().getSiam().start();
    }

    private void _showPorts() {
        log.info("Listing instruments in the SIAM node:");
        ISiam siam = SiamCi.instance().getSiam();
        List<PortItem> ports;
        try {
            ports = siam.listPorts();
        }
        catch (Exception e) {
            log.warn("Error retrieving list of ports", e);
            return;
        }

        for (PortItem pi : ports) {
            log.info(" * port='" + pi.portName + "' device='" + pi.deviceId
                    + "' service='" + pi.serviceName + "'");
        }

        _checkRbnbConnectionIfNeeded(siam, ports);
    }

    /**
     * In case any of the reported instruments may be reporting data, do a quick
     * check that connection to the RBNB server is OK; log a warning if not.
     */
    private void _checkRbnbConnectionIfNeeded(ISiam siam, List<PortItem> ports) {
        /*
         * just use the first reported instrument with an associated RBNB host.
         */
        String rbnbHost = null;
        String portName = null;
        for (PortItem pi : ports) {
            Map<String, String> props = null;
            try {
                props = siam.getPortProperties(pi.portName);
            }
            catch (Exception e) {
                /*
                 * Ignore the exception here. Any associated error with this
                 * instrument should be handled later by other parts of the
                 * code. A likely situation here is java.rmi.UnmarshalException,
                 * which happens when the reported instrument does not have its
                 * associated JAR in the current JRE.
                 */
                continue;
            }
            rbnbHost = SiamUtils.getRbnbHost(props);
            if (rbnbHost != null) {
                portName = pi.portName;
                break;
            }
        }

        if (rbnbHost == null) {
            // no check necessary
            return;
        }

        /*
         * Do the check.
         */
        if (log.isDebugEnabled()) {
            log.debug("Checking connection with OSDT RBNB server on host '"
                    + rbnbHost + "' ...");
        }
        try {
            SiamUtils.checkConnectionToRbnb(rbnbHost,
                    "client-for-testing-connection");
        }
        catch (Exception e) {
            log.warn("Error while testing connection with RBNB server on host '"
                    + rbnbHost
                    + "', which is a property associated with instrument on port '"
                    + portName
                    + "'. Make sure that server is running, otherwise data "
                    + "acquisition commands on this instrument will fail. "
                    + "[exception message: '" + e.getMessage() + "']");
        }
    }

    private void _startAdapter() throws Exception {
        log.info("Starting SIAM-CI adapter");
        SiamCi.instance().getSiamCiAdapter().start();
    }

    /**
     * For a graceful termination of the adapter.
     */
    private void _setShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown") {
            public void run() {
                log.info("Stopping program ...");
                ISiamCiAdapter siamCiAdapter = SiamCi.instance()
                        .getSiamCiAdapter();

                if (siamCiAdapter != null) {
                    try {
                        siamCiAdapter.stop();
                    }
                    catch (Exception e) {
                        log.warn("exception while stopping adapter", e);
                    }
                }
            }
        });
    }

}
