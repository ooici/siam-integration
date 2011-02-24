
/**
 * @__opt attributes
 * @opt operations
 * @hidden
 */
class UMLOptions {}


/**
 * Parent represents the processing/management environment into which an
 * Instrument is installed. Parent exposes some functionality of a 
 * an implementation (e.g. NodeService) to Instrument.
 * 
 * @author Kent Headley
 */
interface Parent {}

/**
 * Interface to an ISI Node, which hosts devices.
 * 
 * @author Tom O'Reilly
 */
interface Node {
	///** Get all device service proxies. */
    //public Device[] getDevices();
}


///**
// * DeviceServiceIF contains methods that must be implemented by every
// * DeviceService. This interface exists so that classes in
// * org.mbari.isi.interfaces need not rely on the moos.deployed package org.mbari.siam.distributed
// */
//interface DeviceServiceIF {}

/**
 * Interface to a device that can be remotely controlled.
 * 
 * @author Tom O'Reilly
 */
interface Device {}

/**
 * An Instrument is a Device that can acquire data.
 * 
 * @author Tom O'Reilly
 */
interface Instrument extends Device {}



class NodeService 
	//extends UnicastRemoteObject 
    implements Node
    	, PowerListener
    {}

    
/**
NodeManager implements node functionality, and instantiates component 
objects including NodeService, PortManager, etc.

	@navassoc - <creates> - NodeService
	@navassoc - <creates> - PortManager
*/
class NodeManager 
	implements Parent
		, ServiceListener
	{}

/**
 * PortManager keeps track of devices installed on node ports.
 * 
 * @author Tom O'Reilly
 */
class PortManager
	{}


	
	
/**
Create a NodeManager object.
	@navassoc - <creates> - NodeManager
*/
class NodeMain
	{}
	
	
	
/**
	@navassoc - <parent> - Parent
*/
abstract class DeviceService 
	implements Device
		//, DeviceServiceIF 
	{}   


///**
//Interface for objects that own scheduled tasks 
//@author Kent Headley
//@see ScheduleTask
//*/
//interface ScheduleOwner
//	{}

	
/**
 * BaseInstrumentService is the base class of an "application framework" for
 * instruments which communicate through a serial port
 * ...
 */
abstract class BaseInstrumentService
    extends DeviceService
    implements Instrument 
    	//, DeviceServiceIF, 
    	//, ScheduleOwner 
    {}
  
    
/**
PolledInstrumentService represents an instrument that is synchronously polled
for its data.
 */
abstract class PolledInstrumentService 
    extends BaseInstrumentService 
    //implements Instrument, DeviceServiceIF, ScheduleOwner 
    {}
  
/**
   Base class for RBR data loggers. 
*/
class DataLogger 
    extends PolledInstrumentService 
    //implements Instrument
	{}

abstract class Seabird
    extends PolledInstrumentService
    //implements Instrument
    {}
    
/** ...
	@opt commentname
	@__note several other subclasses
*/
class SeveralOtherPolledSubClasses 
	extends PolledInstrumentService 
	{}
	
	
	
/**
 StreamingInstrumentService represents an instrument that "streams" data 
 asynchronously to its serial port. 
 */    
abstract class StreamingInstrumentService 
	extends BaseInstrumentService 
	//implements Instrument, DeviceServiceIF, ScheduleOwner 
	{}             
	
/** Instrument service for Seabird SBE19plus in profile mode (MP command) */
class StreamingSBE19 
	extends StreamingInstrumentService 
	//implements Instrument      
	{}
	
/** ...
	@opt commentname
	@__note several other subclasses
*/
class SeveralOtherStreamingSubClasses 
	extends StreamingInstrumentService 
	{}
	

