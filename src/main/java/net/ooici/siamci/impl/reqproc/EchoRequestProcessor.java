package net.ooici.siamci.impl.reqproc;

import net.ooici.play.InstrDriverInterface.Command;

import com.google.protobuf.GeneratedMessage;

/**
 * echo processor.
 * 
 * @author carueda
 */
public class EchoRequestProcessor extends BaseRequestProcessor {
	
	public GeneratedMessage processRequest(int reqId, Command cmd) {
		return cmd;
	}

}
