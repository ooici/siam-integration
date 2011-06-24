package siam;

import java.util.Map;

import org.mbari.siam.distributed.Device;

import com.rbnb.sapi.Sink;

/**
 * Some SIAM related utilities
 * 
 * @author carueda
 */
public class SiamUtils {

    /**
     * Gets the value of the "rbnbServer" property.
     * 
     * @param props
     *            the properties associated to a certain instrument
     * @return the value, or null if it is not associated
     */
    public static String getRbnbHost(Map<String, String> props) {
        String rbnbHost = props.get("rbnbServer");
        if (rbnbHost != null && rbnbHost.trim().length() > 0) {
            return rbnbHost.trim();
        }
        else {
            return null;
        }
    }

    /**
     * Connects to the given RBNB server host as a {@link Sink} client, and then
     * disconnects.
     * 
     * @param rbnbHost
     *            RBNB host name
     * @param clientName
     *            Name for the client connecting to the server
     * 
     * @throws Exception
     *             the RBNB exception that might be generated, if any.
     */
    public static void checkConnectionToRbnb(String rbnbHost, String clientName)
            throws Exception {

        Sink rbnbSink = new Sink();
        try {
            rbnbSink.OpenRBNBConnection(rbnbHost,
                    "client-for-testing-connection");
        }
        finally {
            rbnbSink.CloseRBNBConnection();
        }

    }

    /** Return mnemonic string for specified status. */
    // From PortLister
    public static String statusMnem(int status) {
        switch (status) {
        case Device.OK:
            return "OK";

        case Device.ERROR:
            return "ERROR";

        case Device.INITIAL:
            return "INIT";

        case Device.SHUTDOWN:
            return "SHUTDOWN";

        case Device.SUSPEND:
            return "SUSPEND";

        case Device.SAMPLING:
            return "SAMPLING";

        case Device.SLEEPING:
            return "SLEEPING";

        case Device.SAFE:
            return "SAFE";

        case Device.UNKNOWN:
        default:
            return "UNKNOWN!";
        }
    }

    private SiamUtils() {
    }

}
