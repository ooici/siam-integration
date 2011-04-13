package net.ooici.siamci.impl.reqproc;

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
 * get_last_sample processor.
 * 
 * @author carueda
 */
public class GetLastSampleRequestProcessor extends BaseRequestProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(GetLastSampleRequestProcessor.class);
	
	private static final String CMD_NAME = "get_last_sample";

	public GeneratedMessage processRequest(Command cmd) {
		if (cmd.getArgsCount() == 0) {
			String msg = CMD_NAME+ ": command requires at least an argument";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		ChannelParameterPair cp = cmd.getArgs(0);
		if (!"port".equals(cp.getChannel())) {
			String msg = CMD_NAME+ ": command only accepts 'port' argument";
			log.warn(msg);
			return ScUtils.createFailResponse(msg);
		}
		final String port = cp.getParameter();

		final String publishStream = ScUtils.getPublishStreamName(cmd);
		if (publishStream != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getAndPublishResult(port,
					publishStream);
			return response;
		}
		else {
			// synchronous response.
			//
			Map<String, String> sample = null;
			try {
				sample = siam.getPortLastSample(port);
			}
			catch (Exception e) {
				log.warn("getPortLastSample exception", e);
				return ScUtils.createFailResponse(e.getClass().getName() + ": "
						+ e.getMessage());
			}

			GeneratedMessage response = _createResultResponse(sample);
			return response;
		}
	}

	/**
	 * Does the asynchronous dispatch of the get port last sample operation.
	 * 
	 * @param port
	 *            the SIAM port associated with the instrument
	 * @param publishStream
	 *            the queue (rounting key) to publish the response.
	 * @return The {@link SuccessFail} result of the submission of the request.
	 */
	private GeneratedMessage _getAndPublishResult(final String port,
			final String publishStream) {
		_checkAsyncSetup();

		//
		// TODO more robust assignment of publish IDs
		//
		final String publishId = CMD_NAME+ ";port=" + port;

		asyncSiam.getPortLastSample(port,
				new AsyncCallback<Map<String, String>>() {

					public void onSuccess(Map<String, String> result) {
						GeneratedMessage response = _createResultResponse(result);
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
	 * Creates an "ok" message with a list of pairs corresponding to the result.
	 * TODO currently capturing this result in a {@link SuccessFail} instance;
	 * should probably be a more appropiate GPB.
	 */
	private GeneratedMessage _createResultResponse(Map<String, String> result) {

		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for (Entry<String, String> es : result.entrySet()) {
			buildr.addItem(Item.newBuilder().setType(Item.Type.PAIR).setPair(
					StringPair.newBuilder().setFirst(es.getKey()).setSecond(
							es.getValue())));
		}

		SuccessFail response = buildr.build();
		return response;
	}

}
