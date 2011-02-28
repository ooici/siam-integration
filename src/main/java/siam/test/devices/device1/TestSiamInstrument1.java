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
 * @author carueda
 */
public class TestSiamInstrument1 extends PolledInstrumentService implements Instrument, Safeable {
	private static final long serialVersionUID = 1L;
	
	private static int _sequenceNumber = 0;
	
	protected static Logger _log4j = Logger.getLogger(TestSiamInstrument1.class);
    
    
	public TestSiamInstrument1() throws RemoteException {
		super();
	}

	protected ScheduleSpecifier createDefaultSampleSchedule() throws ScheduleParseException {
		return new ScheduleSpecifier(10*1000);
	}
	
	// puts in the buffer something like:  "nn: Hello SIAM(nn)", where nn is a sequence number
	protected int readSample(byte[] sample) throws TimeoutException, IOException, Exception {
		byte[] bytes = (_sequenceNumber + ": Hello SIAM(" + _sequenceNumber + ")").getBytes();
		_sequenceNumber++;
		int numBytes = Math.min(sample.length, bytes.length);
		System.arraycopy(bytes, 0, sample, 0, numBytes);

		return numBytes;
	}
	
	
	/*
	  NOTE: If instead of a proper class (as this one), I use an anonymous class inside getParser(), 
	  the following exception would be thrown when running getLastSample (looks like an RMI peculiarity):
		$ getLastSample localhost testPort -p
		SIAM version $Name:  $
		parentID=999, recordType=1
		devid=1235, t=1298860870948, seqNo=7225, mdref=7224
		nBytes=13
		0: Hello SIAM
		Caught exception: java.lang.ClassCastException: cannot assign instance of siam.test.devices.device1.TestSiamInstrument1_Stub to field siam.test.devices.device1.TestSiamInstrument1$1.this$0 of type siam.test.devices.device1.TestSiamInstrument1 in instance of siam.test.devices.device1.TestSiamInstrument1$1
		java.lang.ClassCastException: cannot assign instance of siam.test.devices.device1.TestSiamInstrument1_Stub to field siam.test.devices.device1.TestSiamInstrument1$1.this$0 of type siam.test.devices.device1.TestSiamInstrument1 in instance of siam.test.devices.device1.TestSiamInstrument1$1
			at java.io.ObjectStreamClass$FieldReflector.setObjFieldValues(ObjectStreamClass.java:2039)
			at java.io.ObjectStreamClass.setObjFieldValues(ObjectStreamClass.java:1212)
			at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1952)
			at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1870)
			at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1752)
			at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1328)
			at java.io.ObjectInputStream.readObject(ObjectInputStream.java:350)
			at sun.rmi.server.UnicastRef.unmarshalValue(UnicastRef.java:306)
			at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:155)
			at siam.test.devices.device1.TestSiamInstrument1_Stub.getParser(Unknown Source)
			at org.mbari.siam.operations.utils.GetLastSample.processPort(GetLastSample.java:82)
			at org.mbari.siam.operations.utils.PortUtility.processNode(PortUtility.java:106)
			at org.mbari.siam.operations.utils.NodeUtility.run(NodeUtility.java:196)
			at org.mbari.siam.operations.utils.GetLastSample.main(GetLastSample.java:40)
	 */
	private static class MyPacketParser extends PacketParser {
		private static final long serialVersionUID = 1L;

		public Field[] parseFields(DevicePacket packet) throws NotSupportedException, ParseException {
			if (!(packet instanceof SensorDataPacket)) {
			    throw new NotSupportedException("expecting SensorDataPacket");
			}

			SensorDataPacket sensorDataPacket = (SensorDataPacket )packet;
			String foo = new String(sensorDataPacket.dataBuffer());
			
			String[] toks = foo.split(":"); 
			if ( toks.length != 2 ) {
				throw new ParseException("Expecting 2 tokens separated by `:'", 0);
			}
			PacketParser.Field[] fields = new PacketParser.Field[toks.length];
			fields[0] = new Field("sequenceNumber", toks[0], "count");
			fields[1] = new Field("foo", toks[1], "string");
			return fields;
		}		
	}
	public PacketParser getParser() throws NotSupportedException {
		return new MyPacketParser();
	}

	protected void requestSample() throws TimeoutException, Exception {
		System.err.println(getClass().getSimpleName()+ ": requestSample() called!");
		if ( _sequenceNumber == 0 ) {
			return;  // no snooze for very first sample
		}
		_log4j.debug("requestSample() - snooze()");
		try {
			snooze(20);
			_log4j.debug("requestSample() - done with snooze()");
		}
		catch (Throwable e) {
			System.err.println(getClass().getSimpleName()+ ":  requestSample():  exception: " +
					e.getClass().getName() + ": " +e.getMessage());			
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
		_log4j.info("enterSafeMode() - Instructing instrument to begin auto-sampling NOW.");		
	}

}
