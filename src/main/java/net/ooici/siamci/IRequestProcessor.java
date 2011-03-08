package net.ooici.siamci;

import net.ooici.play.instr.InstrumentDefs.Command;

import com.google.protobuf.GeneratedMessage;

/**
 * Processes requests. Used by the adapter implementation to process a request
 * from a client and provice corresponding response.
 * 
 * <p>
 * Currently, "request" actually refers to Command. The "request" concept is convenient
 * because is generic enough and provides flexibility while the interface gets more stabilized. 
 * 
 * @author carueda
 *
 */
public interface IRequestProcessor {

	/**
	 * Processes the given request

	 * @param cmd The request (at this point, a command).
	 * @return The resulting message.
	 */
	public GeneratedMessage processRequest(Command cmd);
}
