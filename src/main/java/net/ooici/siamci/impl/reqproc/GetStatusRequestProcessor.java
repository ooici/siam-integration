package net.ooici.siamci.impl.reqproc;

import net.ooici.play.InstrDriverInterface.ChannelParameterPair;
import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.play.InstrDriverInterface.SuccessFail;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.AsyncCallback;

import com.google.protobuf.GeneratedMessage;

/**
 * get_status processor.
 * 
 * @author carueda
 */
public class GetStatusRequestProcessor extends BaseRequestProcessor {
	
	private static final String CMD_NAME = "get_status";

	private static final Logger log = LoggerFactory
			.getLogger(GetStatusRequestProcessor.class);

	public GeneratedMessage processRequest(int reqId, Command cmd) {
		if (cmd.getArgsCount() == 0) {
			String msg = _rid(reqId) + CMD_NAME+ ": command requires at least an argument";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			String msg = _rid(reqId) + CMD_NAME+ ": command requires 'port' as first argument";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		final String port = cp.getParameter();

		final String publishStream = ScUtils.getPublishStreamName(cmd);
		if (publishStream != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getAndPublishResult(reqId, port,
					publishStream);
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
				log.warn(_rid(reqId) + "getPortStatus exception", e);
				return ScUtils.createFailResponse(e.getClass().getName() + ": "
						+ e.getMessage());
			}

			GeneratedMessage response = _createResultResponse(reqId, status);
			return response;
		}
	}

	/**
	 * Does the asynchronous dispatch of the get port status operation.
	 * 
	 * @param port
	 *            the SIAM port associated with the instrument
	 * @param publishStream
	 *            the queue (rounting key) to publish the response.
	 * @return The {@link SuccessFail} result of the submission of the request.
	 */
	private GeneratedMessage _getAndPublishResult(final int reqId, final String port,
			final String publishStream) {
		_checkAsyncSetup();

		//
		// TODO more robust assignment of publish IDs
		//
		final String publishId = CMD_NAME+ ";port=" + port;

		asyncSiam.getPortStatus(port, new AsyncCallback<String>() {

			public void onSuccess(String result) {
				GeneratedMessage response = _createResultResponse(reqId, result);
				publisher.publish(reqId, publishId, response, publishStream);
			}

			public void onFailure(Throwable e) {
				GeneratedMessage response = ScUtils.createFailResponse(e
						.getClass().getName()
						+ ": " + e.getMessage());
				publisher.publish(reqId, publishId, response, publishStream);
			}
		});

		// respond with OK, ie., sucessfully submitted request:
		GeneratedMessage response = ScUtils.createSuccessResponse(null);
		return response;
	}

	/**
	 * Creates an "ok" message with a list of pairs corresponding to the result.
	 * TODO currently capturing this result in a {@link SuccessFail} instance;
	 * should probably be a more appropiate GPB.
	 */
	private GeneratedMessage _createResultResponse(int reqId, String result) {
		GeneratedMessage response = ScUtils.createSuccessResponse(result);
		return response;
	}

}
