package siam;

import org.mbari.siam.distributed.Device;

/**
 * Some utilities from SIAM code base to faciliate reuse.
 * @author carueda
 */
public class SiamUtils {
	
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

    
	private SiamUtils() {}
    
}

