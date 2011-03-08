package net.ooici.siamci.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.ooici.play.instr.InstrumentDefs.ChannelParameterPair;
import net.ooici.play.instr.InstrumentDefs.Command;
import net.ooici.play.instr.InstrumentDefs.Result;
import net.ooici.play.instr.InstrumentDefs.StringPair;
import net.ooici.play.instr.InstrumentDefs.SuccessFail;
import net.ooici.play.instr.InstrumentDefs.SuccessFail.Builder;
import net.ooici.play.instr.InstrumentDefs.SuccessFail.Item;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.ISiam;
import net.ooici.siamci.ISiam.PortItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

/**
 * Processes requests.
 * 
 * @author carueda
 */
class RequestProcessor implements IRequestProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(RequestProcessor.class);
	
	private final ISiam siam;


	RequestProcessor(ISiam siam) {
		this.siam = siam;
		
		log.debug("CommandProcessor created.");
	}
	
	public GeneratedMessage processRequest(Command cmd) {
		GeneratedMessage response;

		if ( "list_ports".equals(cmd.getCommand()) ) {
			response = _listPorts();
		}
		else if ( "get_status".equals(cmd.getCommand()) ) {
			response = _getStatus(cmd);
		}
		else if ( "get_last_sample".equals(cmd.getCommand()) ) {
			response = _getLastSample(cmd);
		}
		else if ( "ping".equals(cmd.getCommand()) ) {
			SuccessFail sf = SuccessFail.newBuilder().setResult(Result.OK).build();
			response = sf;
		}
		else if ( "echo".equals(cmd.getCommand()) ) {
			// mainly for testing purposes;  return the given command:
			response = cmd;
		}
		else {
			// TODO others
			String description = "Command '" +cmd.getCommand()+ "' not implemented";
			log.debug(description);
			response = _createErrorResponse(description);
		}
		
		return response;
	}

	private GeneratedMessage _listPorts() {
		List<PortItem> list = null;
		try {
			list = siam.listPorts();
		}
		catch (Exception e) {
			log.warn("_listPorts exception", e);
			return _createErrorResponse("Exception: " +e.getMessage());
		}
		
		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for ( PortItem pi : list ) {
			
			buildr.addItem(Item.newBuilder()
					.setType(Item.Type.PAIR)
					.setPair(StringPair.newBuilder().setFirst("portName").setSecond(pi.portName))
			);
			
			buildr.addItem(Item.newBuilder()
					.setType(Item.Type.PAIR)
					.setPair(StringPair.newBuilder().setFirst("deviceId").setSecond(String.valueOf(pi.deviceId)))
			);
		}
		
		SuccessFail response = buildr.build();
		return response;
	}

	private GeneratedMessage _getStatus(Command cmd) {
		if ( cmd.getArgsCount() == 0 ) {
			return _createErrorResponse("get_status command requires at least an argument");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if ( ! "port".equals(cp.getChannel()) ) {
			return _createErrorResponse("get_status command only accepts 'port' argument");
		}
		String port = cp.getParameter();
		
		String status = null;
		try {
			status = siam.getPortStatus(port);
		}
		catch (Exception e) {
			log.warn("_getStatus exception", e);
			return _createErrorResponse("Exception: " +e.getMessage());
		}
		
		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		buildr.addItem(Item.newBuilder()
				.setType(Item.Type.STR)
				.setStr(status)
		);
			
		SuccessFail response = buildr.build();
		return response;
	}
	
	private GeneratedMessage _getLastSample(Command cmd) {
		if ( cmd.getArgsCount() == 0 ) {
			return _createErrorResponse("get_last_sample command requires at least an argument");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if ( ! "port".equals(cp.getChannel()) ) {
			return _createErrorResponse("get_last_sample command only accepts 'port' argument");
		}
		String port = cp.getParameter();
		
		Map<String, String> sample = null;
		try {
			 sample = siam.getPortLastSample(port);
		}
		catch (Exception e) {
			log.warn("_getLastSample exception", e);
			return _createErrorResponse("Exception: " +e.getMessage());
		}

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for ( Entry<String, String> es : sample.entrySet() ) {
			buildr.addItem(Item.newBuilder()
					.setType(Item.Type.PAIR)
					.setPair(StringPair.newBuilder().setFirst(es.getKey()).setSecond(es.getValue()))
			);
		}
		
		SuccessFail response = buildr.build();
		return response;
	}

	
	private GeneratedMessage _createErrorResponse(String description) {
		SuccessFail sf = SuccessFail.newBuilder()
			.setResult(Result.ERROR)
			.addItem(Item.newBuilder()
					.setType(Item.Type.STR)
					.setStr(description)
					.build())
			.build();
		return sf;
	}
	
}
