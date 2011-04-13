package test.siam.utils;

import java.io.IOException;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * A simple test program that receives DataTurbine notifications about new data
 * in given channels.
 * 
 * @author carueda
 */
public class RbnbSinkProgram implements Runnable {

    private static final Logger log = LoggerFactory
            .getLogger(RbnbSinkProgram.class);

    /**
     * Gets the parameters for the program.
     */
    private static class Params {
        private static final String DEFAULT_RBNB_HOST = "localhost";
        private static final int DEFAULT_RBNB_PORT = 3333;
        private static final String DEFAULT_CLIENT_NAME = "TEstSiamInstrument-1235";
        private static final String DEFAULT_CHANNELS = "sequenceNumber,val";

        private static final OptionParser parser = new OptionParser();

        // rbnb
        private static final OptionSpec<String> rbnbHostOpt = parser.accepts(
                "host",
                "RBNB server host").withRequiredArg().describedAs("host")
                .defaultsTo(DEFAULT_RBNB_HOST);

        private static final OptionSpec<String> rbnbPortOpt = parser.accepts(
                "port",
                "RBNB server port").withRequiredArg().describedAs("port")
                .defaultsTo(String.valueOf(DEFAULT_RBNB_PORT));

        // client name
        private static final OptionSpec<String> clientNameOpt = parser.accepts(
                "client",
                "Client name").withRequiredArg().describedAs("host")
                .defaultsTo(DEFAULT_CLIENT_NAME);

        // channels
        private static final OptionSpec<String> channelsOpt = parser.accepts(
                "channels",
                "Channels separated by comma").withRequiredArg().describedAs(
                "channels").defaultsTo(DEFAULT_CHANNELS);

        private static final OptionSpec<String> helpOpt = parser.acceptsAll(
                Arrays.asList("help", "?"),
                "print help message").withOptionalArg();

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
                try {
                    System.out.println(RbnbSinkProgram.class.getSimpleName()
                            + " usage:");
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

        String rbnbHost;
        int rbnbPort;
        String clientName;
        String[] channels;

        private Params(OptionSet options) {
            rbnbHost = options.valueOf(rbnbHostOpt);
            rbnbPort = Integer.parseInt(options.valueOf(rbnbPortOpt));
            clientName = options.valueOf(clientNameOpt);
            channels = options.valueOf(channelsOpt).split("\\s*,\\s*");
        }
    }

    /**
     * Runs the program
     * 
     * @param args
     *            As shown by the --help option:
     * 
     *            <pre>
     * RbnbSinkProgram usage:
     *         Option                                  Description                            
     *         ------                                  -----------                            
     *         -?, --help                              print help message                     
     *         --channels &lt;channels>                   Channels separated by comma (default:  
     *                                                   sequenceNumber,val)                  
     *         --client &lt;host>                         Client name (default:                  
     *                                                   TEstSiamInstrument-1235)             
     *         --host &lt;host>                           RBNB server host (default: localhost)  
     *         --port &lt;port>                           RBNB server port (default: 3333)
     * </pre>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Params params = Params.getParams(args);
        if (params == null) {
            return; // help message output
        }
        Thread.currentThread().setName("main");
        Runnable prg = new RbnbSinkProgram(params.rbnbHost + ":"
                + params.rbnbPort, params.clientName, params.channels);

        new Thread(prg, prg.getClass().getSimpleName()).start();
    }

    // ///////////////
    // instance

    private final String rbnbHost;
    private final String clientName;
    private final String[] channelNames;

    private volatile Sink rbnbSink = null;
    private final ChannelMap channelMap = new ChannelMap();

    public RbnbSinkProgram(String rbnbHost, String clientName,
            String... channelNames) throws Exception {

        this.rbnbHost = rbnbHost;
        this.clientName = clientName;
        this.channelNames = channelNames;

        _prepare();
    }

    private void _prepare() throws Exception {
        log.info("Connecting to " + rbnbHost + " (clientName='" + clientName
                + "') ...");
        rbnbSink = new Sink();
        rbnbSink.OpenRBNBConnection(rbnbHost, clientName);

        channelMap.Clear();
        for (String channelName : channelNames) {
            channelMap.Add(clientName + "/" + channelName);
        }
        log.info("Subscribing to channels " + Arrays.asList(channelNames));
        rbnbSink.Subscribe(channelMap); // , 0, 10, "newest");
    }

    public void run() {
        while (rbnbSink != null) {
            try {
                log.info("Waiting for data ...");
                ChannelMap getmap = rbnbSink.Fetch(-1, channelMap);
                if (getmap == null || getmap.NumberOfChannels() == 0) {
                    log.warn("No data fetched");
                    continue;
                }

                int noChannels = getmap.NumberOfChannels();

                log.info("Fetched " + noChannels+ " channels");
                for (int ch = 0; ch < noChannels; ch++) {

                    String chName = getmap.GetName(ch);
                    // double[] times = getmap.GetTimes(ch);
                    int type = getmap.GetType(ch);

                    double[] value = getmap.GetDataAsFloat64(ch);

                    log.info("  '" + chName + "' (type=" + type + ")  -> "
                            + value[0]);
                }
            }
            catch (SAPIException e) {
                log.warn("Error fetching data: " + e.getMessage(), e);
                if (rbnbSink != null) {
                    rbnbSink.CloseRBNBConnection();
                }
                rbnbSink = null;
            }
        }
    }
}
