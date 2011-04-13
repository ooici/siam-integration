package net.ooici.siamci.impl.reqproc;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.Result;
import net.ooici.play.InstrDriverInterface.StringPair;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.play.InstrDriverInterface.SuccessFail.Builder;
import net.ooici.play.InstrDriverInterface.SuccessFail.Item;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;

import com.google.protobuf.GeneratedMessage;

/**
 * set_params processor.
 * 
 * @author carueda
 */
public class SetParamsRequestProcessor extends BaseRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(SetParamsRequestProcessor.class);

	private static final String CMD_NAME = "set_params";

	public GeneratedMessage processRequest(int reqId, Command cmd) {

		if (cmd.getArgsCount() < 2) {
			String msg = _rid(reqId) + CMD_NAME + ": command requires at least two arguments";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			String msg = _rid(reqId) + CMD_NAME + ": first argument must be 'port'";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		final String port = cp.getParameter();

		Map<String, String> params = new HashMap<String, String>();

		for (int i = 1; i < cmd.getArgsCount(); i++) {
			cp = cmd.getArgs(i);
			String ch = cp.getChannel();
			String pr = cp.getParameter();
			params.put(ch, pr);
		}

		final String publishStream = ScUtils.getPublishStreamName(cmd);
		if (publishStream != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getAndPublishResult(reqId, cmd, port, params,
					publishStream);
			return response;
		}
		else {
			// synchronous response.
			//
			try {
				params = siam.setPortProperties(port, params);
			}
			catch (Exception e) {
				log.warn(_rid(reqId) + "setPortProperties exception", e);
				return ScUtils.createFailResponse(_rid(reqId) + e.getClass().getName() + ": "
						+ e.getMessage());
			}

			GeneratedMessage response = _createResultResponse(reqId, cmd, params);
			return response;
		}
	}

	/**
	 * Does the asynchronous dispatch of the get properties operation.
	 * 
	 * @param cmd
	 *            the original command
	 * @param port
	 *            the SIAM port associated with the instrument
	 * @param params
	 * @param publishStream
	 *            the queue (rounting key) to publish the response.
	 * @return The {@link SuccessFail} result of the submission of the request.
	 */
	private GeneratedMessage _getAndPublishResult(final int reqId, final Command cmd,
			final String port, final Map<String, String> params,
			final String publishStream) {
		_checkAsyncSetup();

		//
		// TODO more robust assignment of publish IDs
		//
		final String publishId = CMD_NAME + ";port=" + port;

		asyncSiam.setPortProperties(port, params,
				new AsyncCallback<Map<String, String>>() {

					public void onSuccess(Map<String, String> result) {
						GeneratedMessage response = _createResultResponse(reqId, cmd,
								result);
						publisher.publish(reqId, publishId, response, publishStream);
					}

					public void onFailure(Throwable e) {
						GeneratedMessage response = ScUtils
								.createFailResponse(e.getClass().getName()
										+ ": " + e.getMessage());
						publisher.publish(reqId, publishId, response, publishStream);
					}
				});

		// respond with OK, ie., sucessfully submitted request:
		GeneratedMessage response = ScUtils.createSuccessResponse(null);
		return response;
	}

	/**
	 * Gets the final response based on the command and the properties that have
	 * been set.
	 * 
	 * @param cmd
	 *            the command
	 * @param params
	 *            parameters that have been set.
	 * @return a response
	 */
	private GeneratedMessage _createResultResponse(int reqId, Command cmd,
			Map<String, String> params) {

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
