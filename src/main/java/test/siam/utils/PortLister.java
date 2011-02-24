package test.siam.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.util.Properties;
import java.text.DateFormat;
import java.rmi.Naming;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;

import org.mbari.siam.core.NodeService;
import org.mbari.siam.operations.utils.NodeUtility;
import org.mbari.siam.utils.PrintfFormat;

public class PortLister extends NodeUtility {
	
    private static Logger _logger = Logger.getLogger(PortLister.class);
	public  static final int JUSTIFY_LEFT=0;
	public static final int JUSTIFY_RIGHT=1;

    private boolean _showStats = false;
	private int _justify=JUSTIFY_RIGHT;
	
	public PortLister(){
		super();
		Properties props=System.getProperties();
		String justify=props.getProperty("portLister.justify","RIGHT");
		if(justify.equalsIgnoreCase("RIGHT")){
			_justify=JUSTIFY_RIGHT;
		}else if(justify.equalsIgnoreCase("LEFT")){
			_justify=JUSTIFY_LEFT;
		}
	}
	
	/** The Field class represents a single column of data to be output.
	    There are width and justification methods that control the
	    formatting of the toString() method.
		
		The Row class manages an array of Fields.
		
	 */
	public class Field{
		
		private String _value;
		private String _name;
		private int _printWidth;
		private String _pad=" ";
		private int _justify=JUSTIFY_RIGHT;
		public Field(String name, String value){
			_name=name;
			_value=value;
			_printWidth=_value.length();
		}
		public void setWidth(int width){
			_printWidth=width;
		}
		public void setPad(String pad){
			_pad=pad;
		}
		public String getName(){
			return _name;
		}
		public String getValue(){
			return _value;
		}
		public void setJustify(int value){
			switch (value) {
				case JUSTIFY_RIGHT:
				case JUSTIFY_LEFT:
					_justify=value;
					break;
				default:
					// should probably throw an exception
					break;
			}
		}
		public String toString(){
			StringBuffer sb=new StringBuffer();
			switch (_justify) {
				case JUSTIFY_RIGHT:
					for(int i=0;i<(_printWidth-_value.length());i++){
						sb.append(_pad);
					}
					sb.append(_value);					
					break;
				case JUSTIFY_LEFT:
					sb.append(_value);					
					for(int i=0;i<(_printWidth-_value.length());i++){
						sb.append(_pad);
					}					
					break;
				default:
					break;
			}
			return sb.toString();
		}
	}
	
	/** The Row class represents a row of data items to be printed in 
		formatted columns. Each column is implemented using a Field instance.
		
	 */
	public class Row{
		Field _fields[];
		public Row(int fields){
			_fields= new Field[fields];
		}
		public void addField(int index,Field field){
			_fields[index]=field;
		}
		public Field getField(int index){
			return _fields[index];
		}
		public Field getField(String name){
			for(int i=0;i<_fields.length;i++){
				if(_fields[i]._name.equals(name)){
					return _fields[i];
				}
			}
			return null;
		}

		public int  getFieldWidth(int field){
			return _fields[field].getValue().length();
		}
		
		public String toString(){
			StringBuffer sb=new StringBuffer();
			for(int i=0;i<_fields.length;i++){
				sb.append(_fields[i].toString()+" ");
			}
			return sb.toString();
		}
		
	}
	
    /** Return mnemonic string for specified status. */
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
	
    public void processCustomOption(String[] args, int index)
	throws InvalidOption {
		
		if (args[index].equals("-stats")) {
			_showStats = true;
		} else {
			throw new InvalidOption("unknown option");
		}
    }
	
	public void processNode(Node node) {
		
		try {
			
			Vector rows=new Vector();
			
			// set number of output columns
			// depending on stats option
			int columns=_showStats?7:4;
			
			// create array to hold the max widths
			// for each field;
			int columnWidths[]=new int[columns];
			
			if (node == null) {
				System.err.println("Got NULL node!");
			}
			
			// get array of Port objects
			Port[] ports = node.getPorts();
			
			if (ports == null) {
				System.err.println("Got NULL ports from node");
			}
			
			// create and populate two header rows
			// one with text and one underline
			Row headerRow=new Row(columns);
			Row header2Row=new Row(columns);
			headerRow.addField(0,new Field("hPortName","Port Name"));
			header2Row.addField(0,new Field("hPortNameUL","---------"));
			headerRow.addField(1,new Field("hPortID","ISI-ID"));
			header2Row.addField(1,new Field("hPortIDUL","------"));
			headerRow.addField(2,new Field("hServiceName","Service"));
			header2Row.addField(2,new Field("hServiceNameUL","-------"));
			headerRow.addField(3,new Field("hServiceStatus","Status"));
			header2Row.addField(3,new Field("hServiceStatusUL","------"));
			if(_showStats){
				headerRow.addField(4,new Field("hSampleCount"," S "));
				header2Row.addField(4,new Field("hSampleCountUL","---"));
				headerRow.addField(5,new Field("hErrorCount"," E "));
				header2Row.addField(5,new Field("hErrorCountUL","---"));
				headerRow.addField(6,new Field("hRetryCount"," R "));
				header2Row.addField(6,new Field("hRetryCountUL","---"));
			}
			
			// add the header rows to the output Vector
			rows.add(headerRow);
			rows.add(header2Row);
			
			// iterate through the ports, adding appropriate
			// lines for each
			for (int i = 0; i < ports.length; i++) {
				
				// set field (column) defaults
				long lDeviceID=-1L;
				String portName=new String(ports[i].getName());
				String deviceID="-";
				String serviceName="-";
				String serviceStatus="-";
				String sampleCount="-";
				String errorCount="-";
				String retryCount="-";
				
				try {
					// get port device ID and get associated Device stub
					lDeviceID=ports[i].getDeviceID();
					Device device = node.getDevice(lDeviceID);
					
					// get field values
					portName=new String(ports[i].getName());
					 deviceID=Long.toString(lDeviceID);
					 serviceName=new String(ports[i].getServiceMnemonic());
					 serviceStatus=statusMnem(device.getStatus());
					 sampleCount=Long.toString(device.getSamplingCount());
					 errorCount=Long.toString(device.getSamplingErrorCount());
					 retryCount=Long.toString(device.getSamplingRetryCount());
					
					// generate a row of output for the device
					Row serviceRow=new Row(columns);
					
					serviceRow.addField(0,new Field("sPortName",portName));
					serviceRow.addField(1,new Field("sPortID",deviceID));
					serviceRow.addField(2,new Field("sServiceName",serviceName));
					serviceRow.addField(3,new Field("sServiceStatus",serviceStatus));
					
					if(_showStats){
						serviceRow.addField(4,new Field("sSampleCount",sampleCount));
						serviceRow.addField(5,new Field("sErrorCount",errorCount));
						serviceRow.addField(6,new Field("sRetryCount",retryCount));
					}
					// add the row to the output Vector
					rows.add(serviceRow);
				}
				catch (DeviceNotFound dnf) {
					Row serviceRow=new Row(columns);
					
					// if there is a port defined without a service installed,
					// catch the exception and add a blank row to the output Vector
					serviceRow.addField(0,new Field("sPortName",portName));
					serviceRow.addField(1,new Field("sPortID","-"));
					serviceRow.addField(2,new Field("sServiceName","-"));
					serviceRow.addField(3,new Field("sServiceStatus","-"));
					
					if(_showStats){
						serviceRow.addField(4,new Field("sSampleCount","-"));
						serviceRow.addField(5,new Field("sErrorCount"," "));
						serviceRow.addField(6,new Field("sRetryCount"," "));
					}
					rows.add(serviceRow);					
				}
				catch (Exception e) {
					System.err.println(e);
				}
			}
			
			// now that all the field widths are known
			// use the max for each field as the field width
			for(int i=0;i<rows.size();i++){
				Row row=(Row)rows.get(i);
				for(int j=0;j<columns;j++){
					if(row.getFieldWidth(j)>columnWidths[j]){
						columnWidths[j]=row.getFieldWidth(j);					
					}
				}
			}

			// Print to console...
			// Number of ports...
			System.out.println("\nNode " + node.getId() + " has " + ports.length
							   + " ports\n");
			
			// output Vector rows of header and port data...
			for(int i=0;i<rows.size();i++){
				Row row=(Row)rows.get(i);
				for(int j=0;j<columns;j++){
					Field field=row.getField(j);
					field.setWidth(columnWidths[j]);
					field.setJustify(_justify);
				}
				System.out.println(row);
			}
			
			// spacer line at end
			System.out.println("");
		} catch (RemoteException e) {
			System.err.println("RemoteException: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Got " + e.getClass().getName() + 
							   ": " + 
							   e.getMessage());
			System.exit(1);
		}
    }
	
    public void printUsage() {
		System.err.println("usage: PortLister nodeURL [-stats]");
    }
	
    public static void main(String[] args) {
		
    	PortLister lister = new PortLister();
		lister.processArguments(args);
		lister.run();
    }
	
}
