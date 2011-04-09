package net.ooici.siamci.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.StringPair;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;
import siam.IAsyncSiam;
import siam.ISiam;
import siam.PortItem;

import com.google.protobuf.GeneratedMessage;

/**
 * Processes requests.
 * 
 * @author carueda
 */
class RequestProcessor implements IRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(RequestProcessor.class);

	private final ISiam siam;
	private IAsyncSiam asyncSiam;
	private IPublisher respondSender;

	RequestProcessor(ISiam siam) {
		this.siam = siam;

		log.debug("instance created.");
	}

	public void setAsyncSiam(IAsyncSiam asyncSiam) {
		this.asyncSiam = asyncSiam;
	}

	public void setPublisher(IPublisher respondSender) {
		this.respondSender = respondSender;
	}

	public GeneratedMessage processRequest(Command cmd) {
		GeneratedMessage response;

		if ("list_ports".equals(cmd.getCommand())) {
			response = _listPorts();
		}
		else if ("get_status".equals(cmd.getCommand())) {
			response = _getStatus(cmd);
		}
		else if ("get_last_sample".equals(cmd.getCommand())) {
			response = _getLastSample(cmd);
		}
		else if ("fetch_params".equals(cmd.getCommand())) {
			response = _fetchParams(cmd);
		}
		else if ("set_params".equals(cmd.getCommand())) {
			response = _setParams(cmd);
		}
		else if ("ping".equals(cmd.getCommand())) {
			SuccessFail sf = SuccessFail.newBuilder().setResult(Result.OK)
					.build();
			response = sf;
		}
		else if ("echo".equals(cmd.getCommand())) {
			// mainly for testing purposes; return the given command:
			response = cmd;
		}
		else {
			// TODO others
			String description = "Command '" + cmd.getCommand()
					+ "' not implemented";
			log.debug(description);
			response = ScUtils.createFailResponse(description);
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
			return ScUtils.createFailResponse(e.getClass().getName() + ": "
					+ e.getMessage());
		}

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for (PortItem pi : list) {

			buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
					StringPair.newBuilder().setFirst("portName").setSecond(
							pi.portName)));

			buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
					StringPair.newBuilder().setFirst("deviceId").setSecond(
							String.valueOf(pi.deviceId))));
		}

		SuccessFail response = buildr.build();
		return response;
	}

	/**
	 * get status
	 */
	private GeneratedMessage _getStatus(Command cmd) {
		if (cmd.getArgsCount() == 0) {
			return ScUtils.createFailResponse("get_status command requires at least an argument");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			return ScUtils.createFailResponse("get_status command requires 'port' as first argument");
		}
		final String port = cp.getParameter();

		final String publishQueue = ScUtils.getPublishQueue(cmd);
		if (publishQueue != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getPublishStatus(port, publishQueue);
			return response;
		}
		else {
			// synchronous response.
			//
			String status = null;
			try {
				status = siam.getPortStatus(port);
			}
			catch (Exception e) {
				log.warn("_getStatus exception", e);
				return ScUtils.createFailResponse(e.getClass().getName() + ": "
						+ e.getMessage());
			}

			Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
			buildr.addItem(Item.newBuilder().setType(Item.Type.STR).setStr(
					status));

			GeneratedMessage response = buildr.build();
			return response;
		}
	}

	/**
	 * Does the asynchronous dispatch of the get port status operation.
	 * 
	 * @param port
	 *            the SIAM port associated with the instrument
	 * @param publishQueue
	 *            the queue (rounting key) to publish the response.
	 * @return The {@link SuccessFail} result of the submission of the request.
	 */
	private GeneratedMessage _getPublishStatus(final String port,
			final String publishQueue) {
		_checkAsyncSetup();

		asyncSiam.getPortStatus(port, new AsyncCallback<String>() {

			public void onSuccess(String result) {
				GeneratedMessage response = ScUtils.createSuccessResponse(result);
				respondSender.publish(response, publishQueue);
			}

			public void onFailure(Throwable e) {
				GeneratedMessage response = ScUtils.createFailResponse(e.getClass()
						.getName()
						+ ": " + e.getMessage());
				respondSender.publish(response, publishQueue);
			}
		});

		// respond with OK, ie., sucessfully submitted request:
		GeneratedMessage response = ScUtils.createSuccessResponse(null);
		return response;
	}


	/**
	 * throws {@link IllegalStateException} if any required object for
	 * asynchronous handling is missing.
	 */
	private void _checkAsyncSetup() {
		if (asyncSiam == null) {
			throw new IllegalStateException("No "
					+ IAsyncSiam.class.getSimpleName()
					+ " object has been associated");
		}
		if (respondSender == null) {
			throw new IllegalStateException("No "
					+ IPublisher.class.getSimpleName()
					+ " object has been associated");
		}
	}

	/**
	 * Get last sample
	 */
	private GeneratedMessage _getLastSample(Command cmd) {
		if (cmd.getArgsCount() == 0) {
			return ScUtils.createFailResponse("get_last_sample command requires at least an argument");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			return ScUtils.createFailResponse("get_last_sample command only accepts 'port' argument");
		}
		String port = cp.getParameter();

		Map<String, String> sample = null;
		try {
			sample = siam.getPortLastSample(port);
		}
		catch (Exception e) {
			log.warn("_getLastSample exception", e);
			return ScUtils.createFailResponse(e.getClass().getName() + ": "
					+ e.getMessage());
		}

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for (Entry<String, String> es : sample.entrySet()) {
			buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
					StringPair.newBuilder().setFirst(es.getKey()).setSecond(
							es.getValue())));
		}

		SuccessFail response = buildr.build();
		return response;
	}

	/**
	 * fetch params.
	 * 
	 * If only ('port', portName) is given, then all parameters are fetched.
	 * Otherwise, the specific list of parameters are fetched. FIXME: Note that
	 * the value for an invalid parameter will be "ERROR"; this error needs a
	 * more appropriate notification.
	 */
	private GeneratedMessage _fetchParams(Command cmd) {
		if (cmd.getArgsCount() == 0) {
			return ScUtils.createFailResponse("fetch_params command requires at least one argument");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			return ScUtils.createFailResponse("fetch_params: first argument must be 'port'");
		}
		String port = cp.getParameter();

		// get all instrument parameters:
		Map<String, String> params = null;
		try {
			params = siam.getPortProperties(port);
		}
		catch (Exception e) {
			log.warn("fetch_params exception", e);
			return ScUtils.createFailResponse(e.getClass().getName()
					+ ": Could not fetch instrument parameters: "
					+ e.getMessage());
		}

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);

		if (cmd.getArgsCount() > 1) {
			// specific parameters are being requested.
			List<String> requestedParams = new ArrayList<String>();
			for (int i = 1; i < cmd.getArgsCount(); i++) {
				cp = cmd.getArgs(i);
				String ch = cp.getChannel();
				String pr = cp.getParameter();
				if (!"instrument".equals(ch)) {
					String error = "set_params command: first element in tuple must be 'instrument.' "
							+ "Other channel name is NOT yet implemented: '"
							+ ch + "'";
					if (log.isDebugEnabled()) {
						log.debug("_fetchParams: " + error);
					}
					return ScUtils.createFailResponse(error);
				}
				requestedParams.add(pr);

			}
			for (String reqParam : requestedParams) {
				String value = params.get(reqParam);

				if (value == null) {
					//
					// FIXME when the parameter does not exist, notify the
					// corresponding error
					// in an appropriate way. For now, just setting the value to
					// "ERROR"
					value = "ERROR";
				}

				buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR)
						.setPair(
								StringPair.newBuilder().setFirst(reqParam)
										.setSecond(value)));
			}
		}
		else {
			// ALL parameters are being requested.
			for (Entry<String, String> es : params.entrySet()) {
				buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR)
						.setPair(
								StringPair.newBuilder().setFirst(es.getKey())
										.setSecond(es.getValue())));
			}
		}

		SuccessFail response = buildr.build();
		return response;
	}

	/**
	 * set params
	 */
	private GeneratedMessage _setParams(Command cmd) {
		if (cmd.getArgsCount() == 0) {
			return ScUtils.createFailResponse("set_params command requires at least two arguments");
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			return ScUtils.createFailResponse("set_params command: first argument must be 'port'");
		}
		String port = cp.getParameter();

		Map<String, String> params = new HashMap<String, String>();

		for (int i = 1; i < cmd.getArgsCount(); i++) {
			cp = cmd.getArgs(i);
			String ch = cp.getChannel();
			String pr = cp.getParameter();
			params.put(ch, pr);
		}
		try {
			params = siam.setPortProperties(port, params);
		}
		catch (Exception e) {
			log.warn("set_params exception", e);
			return ScUtils.createFailResponse(e.getClass().getName() + ": "
					+ e.getMessage());
		}

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for (Entry<String, String> es : params.entrySet()) {
			buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
					StringPair.newBuilder().setFirst(es.getKey()).setSecond(
							es.getValue())));
		}

		SuccessFail response = buildr.build();
		return response;
	}


}
