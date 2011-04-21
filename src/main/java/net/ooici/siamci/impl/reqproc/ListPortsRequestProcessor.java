package net.ooici.siamci.impl.reqproc;

import java.util.List;

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
import siam.PortItem;

import com.google.protobuf.GeneratedMessage;

/**
 * list_ports processor.
 * 
 * @author carueda
 */
public class ListPortsRequestProcessor extends BaseRequestProcessor {

	private static final String CMD_NAME = "list_ports";

	private static final Logger log = LoggerFactory
			.getLogger(ListPortsRequestProcessor.class);

	public GeneratedMessage processRequest(int reqId, Command cmd) {

		final String publishStream = ScUtils.getPublishStreamName(cmd);
		if (publishStream != null) {
			// asynchronous handling.
			//
			GeneratedMessage response = _getAndPublishResult(reqId, publishStream);
			return response;
		}
		else {
			// synchronous response.
			//
			List<PortItem> ports = null;
			try {
				ports = siam.listPorts();
			}
			catch (Exception e) {
				log.warn(_rid(reqId) + "getPortStatus exception", e);
				return ScUtils.createFailResponse(e.getClass().getName() + ": "
						+ e.getMessage());
			}

			GeneratedMessage response = _createResultResponse(reqId, ports);
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
	private GeneratedMessage _getAndPublishResult(final int reqId, final String publishStream) {
		_checkAsyncSetup();

		//
		// TODO more robust assignment of publish IDs
		//
		final String publishId = CMD_NAME + ";";

		asyncSiam.listPorts(new AsyncCallback<List<PortItem>>() {

			public void onSuccess(List<PortItem> result) {
				GeneratedMessage response = _createResultResponse(reqId, result);
				_getPublisher().publish(reqId, publishId, response, publishStream);
			}

			public void onFailure(Throwable e) {
				GeneratedMessage response = ScUtils.createFailResponse(e
						.getClass().getName()
						+ ": " + e.getMessage());
				_getPublisher().publish(reqId, publishId, response, publishStream);
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
	private GeneratedMessage _createResultResponse(int reqId, List<PortItem> result) {
		Builder buildr = SuccessFail.newBuilder().setResult(Result.OK);
		for (PortItem pi : result) {

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

}
