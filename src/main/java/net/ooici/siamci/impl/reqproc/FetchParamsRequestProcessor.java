package net.ooici.siamci.impl.reqproc;

import java.util.ArrayList;
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
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;

import com.google.protobuf.GeneratedMessage;

/**
 * fetch_params processor.
 * 
 * @author carueda
 */
public class FetchParamsRequestProcessor extends BaseRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(FetchParamsRequestProcessor.class);

	private static final String CMD_NAME = "fetch_params";

	public GeneratedMessage processRequest(Command cmd) {

		if (cmd.getArgsCount() == 0) {
			String msg = CMD_NAME + ": command requires at least an argument";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			String msg = CMD_NAME + ": first argument must be 'port'";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		final String port = cp.getParameter();

		final String publishStream = ScUtils.getPublishStreamName(cmd);
		if (publishStream != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getAndPublishResult(cmd, port,
					publishStream);
			return response;
		}
		else {
			// synchronous response.
			//
			Map<String, String> props = null;
			try {
				props = siam.getPortProperties(port);
			}
			catch (Exception e) {
				log.warn("getPortProperties exception", e);
				return ScUtils.createFailResponse(e.getClass().getName() + ": "
						+ e.getMessage());
			}

			GeneratedMessage response = _createResultResponse(cmd, props);
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
	 * @param publishStream
	 *            the queue (rounting key) to publish the response.
	 * @return The {@link SuccessFail} result of the submission of the request.
	 */
	private GeneratedMessage _getAndPublishResult(final Command cmd,
			final String port, final String publishStream) {
		_checkAsyncSetup();

		//
		// TODO more robust assignment of publish IDs
		//
		final String publishId = CMD_NAME + ";port=" + port;

		asyncSiam.getPortProperties(port,
				new AsyncCallback<Map<String, String>>() {

					public void onSuccess(Map<String, String> result) {
						GeneratedMessage response = _createResultResponse(cmd,
								result);
						publisher.publish(publishId, response, publishStream);
					}

					public void onFailure(Throwable e) {
						GeneratedMessage response = ScUtils
								.createFailResponse(e.getClass().getName()
										+ ": " + e.getMessage());
						publisher.publish(publishId, response, publishStream);
					}
				});

		// respond with OK, ie., sucessfully submitted request:
		GeneratedMessage response = ScUtils.createSuccessResponse(null);
		return response;
	}

	/**
	 * Gets the final response based on the command and all the properties for
	 * the instrument.
	 * 
	 * @param cmd
	 *            the command
	 * @param props
	 *            all the instrument properties reported by SIAM
	 * @return a response
	 */
	private GeneratedMessage _createResultResponse(Command cmd,
			Map<String, String> props) {

		if (cmd.getArgsCount() <= 1) {
			//
			// ALL parameters are being requested.
			//
			Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
			for (Entry<String, String> es : props.entrySet()) {
				buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR)
						.setPair(
								StringPair.newBuilder().setFirst(es.getKey())
										.setSecond(es.getValue())));
			}
			SuccessFail response = buildr.build();
			return response;
		}
		else {
			// 
			// Specific parameters are being requested.
			//
			List<String> requestedParams = new ArrayList<String>();
			for (int i = 1; i < cmd.getArgsCount(); i++) {
				ChannelParameterPair cp = cmd.getArgs(i);
				String ch = cp.getChannel();
				String pr = cp.getParameter();
				if ("instrument".equals(ch)) {
					requestedParams.add(pr);
				}
				else if ("publish_stream".equals(ch)) {
					// OK. accept but ignore this for the moment
					// TODO the publish_stream should be indicated in another way.
					continue;
				}
				else {
					String error = CMD_NAME
							+ ": first element in tuple must be 'instrument'. "
							+ "Other channel name is NOT yet implemented: '"
							+ ch + "'";
					if (log.isDebugEnabled()) {
						log.debug(CMD_NAME+ ": " + error);
					}
					return ScUtils.createFailResponse(error);
				}

			}
			Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
			for (String reqParam : requestedParams) {
				String value = props.get(reqParam);

				if (value == null) {
					//
					// TODO when the parameter does not exist, notify the
					// corresponding error in an appropriate way. For now,
					// returning an overall error response (even if other
					// requested params are ok).
					String msg = "Requested parameter '" + reqParam
							+ "' not recognized";
					if (log.isDebugEnabled()) {
						log.debug(CMD_NAME+ ": " + msg);
					}
					return ScUtils.createFailResponse(msg);
				}

				buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR)
						.setPair(
								StringPair.newBuilder().setFirst(reqParam)
										.setSecond(value)));
			}
			SuccessFail response = buildr.build();
			return response;
		}

	}

}
