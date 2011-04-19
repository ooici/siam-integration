package siam.test.devices.device1;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;

/**
 * A simple SIAM instrument for basic tests.
 * 
 * @author carueda
 */
public class TestSiamInstrument1 extends PolledInstrumentService implements
        Instrument, Safeable {
    private static final long serialVersionUID = 1L;

    private static int _sequenceNumber = 0;

    protected static Logger log = Logger.getLogger(TestSiamInstrument1.class);

    public TestSiamInstrument1() throws RemoteException {
        super();
    }

    protected ScheduleSpecifier createDefaultSampleSchedule()
            throws ScheduleParseException {
        return new ScheduleSpecifier(10 * 1000);
    }

    // puts in the buffer something like: "nn:mm", where nn is a sequence number
    // and mm
    // is a random double number
    protected int readSample(byte[] sample) throws TimeoutException,
            IOException, Exception {

        byte[] bytes = (_sequenceNumber + ":" + Math.random()).getBytes();
        _sequenceNumber++;
        int numBytes = Math.min(sample.length, bytes.length);
        System.arraycopy(bytes, 0, sample, 0, numBytes);

        return numBytes;
    }

    private static class MyPacketParser extends PacketParser {
        private static final long serialVersionUID = 1L;

        public Field[] parseFields(DevicePacket packet)
                throws NotSupportedException, ParseException {
            if (!(packet instanceof SensorDataPacket)) {
                throw new NotSupportedException("expecting SensorDataPacket");
            }

            SensorDataPacket sensorDataPacket = (SensorDataPacket) packet;
            String buffer = new String(sensorDataPacket.dataBuffer());

            Integer sequenceNumber = new Integer(-1);
            Double val = new Double(0);

            if (buffer.trim().length() > 0) {
                String[] toks = buffer.split(":");
                if (toks.length != 2) {
                    throw new ParseException(
                            "Expecting 2 tokens separated by `:'. String received = '"
                                    + buffer + "'", 0);
                }
                sequenceNumber = new Integer(toks[0]);
                val = new Double(toks[1]);
            }
            PacketParser.Field[] fields = new PacketParser.Field[2];
            fields[0] = new Field("sequenceNumber", sequenceNumber, "count");
            fields[1] = new Field("val", val, "double");
            return fields;
        }
    }

    public PacketParser getParser() throws NotSupportedException {
        return new MyPacketParser();
    }

    protected void requestSample() throws TimeoutException, Exception {
        System.err.println(getClass().getSimpleName()
                + ": requestSample() called, _sequenceNumber = "
                + _sequenceNumber);
        if (_sequenceNumber == 0) {
            return; // no snooze for very first sample
        }
        log.debug("requestSample() - snooze()");
        try {
            snooze(7);
            log.debug("requestSample() - done with snooze()");
        }
        catch (Throwable e) {
            System.err.println(getClass().getSimpleName()
                    + ":  requestSample():  exception: "
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }

    protected int initCurrentLimit() {
        return 500;
    }

    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }

    protected int initInstrumentStartDelay() {
        return 500;
    }

    protected int initMaxSampleBytes() {
        return 32;
    }

    protected byte[] initPromptString() {
        return ">".getBytes();
    }

    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }

    public int test() throws RemoteException {
        return Device.OK;
    }

    public void enterSafeMode() throws Exception {
        log
                .info("enterSafeMode() - Instructing instrument to begin auto-sampling NOW.");
    }

}
