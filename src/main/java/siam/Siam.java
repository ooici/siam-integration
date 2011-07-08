package siam;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.NodeInfo;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser.Field;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.SiamSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.InstrumentSample.SampleDatum;

/**
 * Implementation of the high-level API to access SIAM functionality.
 * 
 * <p>
 * See <a
 * href="http://oidemo.mbari.org:1451/siam-site/content/utilityReference.html"
 * >the SIAM utility reference</a> for a general description of the operations.
 * 
 * <p>
 * Note: this implementation is very preliminary and can certainly be improved;
 * it is just a quick basis for the prototype.
 * 
 * @author carueda
 */
public class Siam implements ISiam {

    private static Logger log = LoggerFactory.getLogger(Siam.class);

    // Copied verbatim from NodeUtility
    private static final String getNodeURL(String input) {

        if (log.isDebugEnabled()) {
            log.debug("getNodeURL(): input=" + input);
        }

        if (input.startsWith("rmi://")) {
            return input;
        }
        else if (input.startsWith("//")) {
            return "rmi:" + input;
        }
        else {
            return "rmi://" + input + "/node";
        }
    }

    private static Node _getNode(String host) throws NotBoundException, IOException {

        String nodeURL = getNodeURL(host);
        RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
        
        log.info("Looking up '" + nodeURL + "' to connect with SIAM node ...");
        Node node = (Node) Naming.lookup(nodeURL.toString());
        log.info("Connected to SIAM node. (" +node.getClass().getName()+ ")");
        return node;
    }

    /**
     * Logs a simplified version of the throwable and its chain of causes
     * without the full stacktrace.
     */
    private static void _logError(Throwable t) {
        String prefix = "";
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(prefix + t.getClass().getName() + ": " + t.getMessage()
                    + "\n");
            t = t.getCause();
            prefix = "caused by: ";
        }
        log.error(sb.toString());
    }

    // /////////
    // Instance.
    // /////////

    private final String host;

    // assigned by start()
    private Node node;
    private long nodeId;
    private NodeInfo nodeInfo;

    /**
     * Creates a point of access to a SIAM node instance. To actually enable the
     * connection, call {@link #start()}.
     * 
     * @param host
     */
    public Siam(String host) {
        this.host = host;
    }

    private void _checkConnection() {
        if (node == null) {
            throw new RuntimeException("start() must be called first");
        }
    }

    public void start() throws Exception {
        if (node != null) {
            throw new RuntimeException("already connected");
        }
        try {
            node = _getNode(host);
            nodeId = node.getId();
            nodeInfo = node.getNodeInfo();
        }
        catch (Exception e) {
            _logError(e);
            throw e;
        }
    }

    public long getNodeId() {
        _checkConnection();
        return nodeId;
    }

    public String getNodeInfo() {
        _checkConnection();
        return nodeInfo.toString();
    }

    public List<PortItem> listPorts() throws Exception {
        _checkConnection();
        return new PortLister(node).listPorts();
    }

    /**
     * helper
     */
    private Instrument _getInstrument(String portName) throws Exception {
        try {
            Device device = node.getDevice(portName.getBytes());

            if (device instanceof Instrument) {
                Instrument instrument = (Instrument) device;
                return instrument;
            }
            else {
                throw new Exception("Device on port " + portName
                        + " is not an Instrument");
            }
        }
        catch (PortNotFound e) {
            throw new Exception("Port " + portName + " not found");
        }
        catch (DeviceNotFound e) {
            throw new Exception("Device not found on port " + portName);
        }
        catch (NoDataException e) {
            throw new Exception("No data from instrument on port " + portName);
        }
    }

    public String getPortStatus(String port) throws Exception {
        _checkConnection();
        Instrument instrument = _getInstrument(port);
        return SiamUtils.statusMnem(instrument.getStatus());
    }

    /**
     * Adapted from SIAM's GetLastSample
     */
    public InstrumentSample getPortLastSample(String portName) throws Exception {
        _checkConnection();
        Instrument instrument = _getInstrument(portName);
        SensorDataPacket sdp = instrument.getLastSample();

        // get metadata
        HashMap<String, String> md = new HashMap<String, String>();
        md.put("parentId", String.valueOf(sdp.getParentId()));
        md.put("recordType", String.valueOf(sdp.getRecordType()));
        md.put("systemTime", String.valueOf(sdp.systemTime()));
        md.put("seqNo", String.valueOf(sdp.sequenceNo()));
        md.put("mdref", String.valueOf(sdp.metadataRef()));
        md.put("nBytes", String.valueOf(sdp.dataBuffer().length));
        md.put("buffer", PrintUtils.getAscii(sdp.dataBuffer(), 0, 0));

        InstrumentSample sample = new InstrumentSample(md);

        // get data
        SensorDataPacket packet = instrument.getLastSample();
        PacketParser parser = instrument.getParser();
        PacketParser.Field[] fields = parser.parseFields(packet);
        for (int j = 0; j < fields.length; j++) {
            if (fields[j] == null) {
                continue;
            }
            String name = fields[j].getName();
            Object value = fields[j].getValue();
            String units = fields[j].getUnits();
            SampleDatum datum = new SampleDatum(name, value, units);
            sample.addDatum(datum);
        }

        return sample;
    }

    /**
     * @return the names of the channels for the given instrument.
     */
    public List<String> getPortChannels(String portName) throws Exception {
        _checkConnection();
        /*
         * Strategy: get last sample, parse that sample, and get the fields from
         * the parser.
         */
        Instrument instrument = _getInstrument(portName);
        SensorDataPacket sdp = instrument.getLastSample();
        Field[] fields = instrument.getParser().parseFields(sdp);
        List<String> result = new ArrayList<String>();
        for (Field field : fields) {
            String name = field.getName();
            result.add(name);
        }

        return result;
    }

    /**
     * Adapted from SIAM's PrintInstrumentProperties
     */
    public Map<String, String> getPortProperties(String portName)
            throws Exception {

        _checkConnection();

        Instrument instrument = _getInstrument(portName);

        HashMap<String, String> result = new LinkedHashMap<String, String>();
        @SuppressWarnings("unchecked")
        Vector<byte[]> properties = instrument.getProperties();
        for (int j = 0; j < properties.size(); j++) {
            byte[] property = properties.elementAt(j);
            String entry = new String(property);
            String[] toks = entry.split("=", 2);
            String key = toks[0].trim();
            String value = toks.length == 2 ? toks[1].trim() : "??";
            result.put(key, value);
        }

        return result;
    }
    
    public String getTurbineName(String portName, String channelName) throws Exception {
        Map<String, String> props = getPortProperties(portName);
        
        String serviceName = props.get("serviceName");
        String isiID = props.get("isiID");

        if (serviceName == null || serviceName.trim().length() == 0) {
            throw new Exception("'serviceName' property no associated with instrument");
        }
        if (isiID == null || isiID.trim().length() == 0) {
            throw new Exception("'isiID' property no associated with instrument");
        }
        serviceName = serviceName.trim();
        isiID = isiID.trim();

        String turbineName = serviceName.replace(' ', '_') + "-" + isiID + "/"
                + channelName;
        return turbineName;        
    }

    /**
     * Adapted from SIAM's SetInstrumentProperty
     */
    public Map<String, String> setPortProperties(String portName,
            Map<String, String> params) throws Exception {

        _checkConnection();

        Instrument instrument = _getInstrument(portName);

        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> e : params.entrySet()) {
            sb.append(e.getKey() + "=" + e.getValue() + "\n");
        }
        String string = sb.toString();

        if (log.isDebugEnabled()) {
            log.debug("properties string: " + string);
        }

        HashMap<String, String> result = new LinkedHashMap<String, String>();

        // TODO: How to (easily) discriminate which properties are actually set
        // and which are not?
        //
        instrument.setProperty(string.getBytes(), new byte[0]);

        // everything OK. For each param, indicate "OK"
        for (Entry<String, String> e : params.entrySet()) {
            result.put(e.getKey(), "OK");
        }
        if (log.isDebugEnabled()) {
            log.debug("Everything OK: " + result);
        }

        return result;

    }

    /**
     * test program:
     * 
     * @param args
     *            [host [port]]
     */
    public static void main(String[] args) throws Exception {
        final String host = args.length >= 1 ? args[0] : "localhost";
        final String port = args.length >= 2 ? args[1] : "testPort";

        Siam siam = new Siam(host);
        siam.start();

        PrintWriter out = new PrintWriter(System.out, true);

        // /////////////////////////////////////////////////////////
        // listPorts:
        out.println("**listPorts:");
        List<PortItem> list = siam.listPorts();
        PortLister.list(siam.getNodeId(), list, out);
        out.println();

        // /////////////////////////////////////////////////////////
        // getPortStatus:
        out.println("**getPortStatus: port=" + port);
        String status = siam.getPortStatus(port);
        out.println("   status: " + status);
        out.println();

        // /////////////////////////////////////////////////////////
        // getPortLastSample:
        out.println("**getPortLastSample: port=" + port);
        InstrumentSample sample = siam.getPortLastSample(port);
        Map<String, String> md = sample.getMd();
        out.println("   result metadata: " + md);
        out.println("   result data: ");
        for (SampleDatum datum : sample.getData()) {
            out.printf("'%s' = %s (%s)%n",
                    datum.getName(),
                    datum.getValue(),
                    datum.getUnits());
        }
        out.println();

        // /////////////////////////////////////////////////////////
        // getPortLastSample:
        out.println("**getPortProperties: port=" + port);
        Map<String, String> portProps = siam.getPortProperties(port);
        out.println("   result: " + portProps);
        out.println();

    }

}
