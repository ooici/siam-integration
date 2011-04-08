package siam;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the asynchronous interface
 * 
 * @author carueda
 */
public class AsyncSiam implements IAsyncSiam {

	private static Logger log = LoggerFactory.getLogger(AsyncSiam.class);

	private final ISiam siam;
	private final ExecutorService es = Executors.newCachedThreadPool();

	/**
	 * Creates an asynchronous SIAM dispatcher
	 * 
	 * @param siam
	 *            the object to access the SIAM library.
	 * 
	 * @throws Exception
	 */
	public AsyncSiam(ISiam siam) {
		this.siam = siam;
	}

	public String getPortStatus(final String port,
			final AsyncCallback<String> callback) {
		if (callback == null) {
			throw new IllegalArgumentException();
		}

		if (log.isDebugEnabled()) {
			log.debug("submitting request for port status. port=" + port);
		}

		es.submit(new Runnable() {
			public void run() {
				try {
					String result = siam.getPortStatus(port);
					if (log.isTraceEnabled()) {
						log.trace("To call callback with port status. port="
								+ port + "  status=" + result);
					}
					callback.onSuccess(result);
				}
				catch (Throwable e) {
					if (log.isTraceEnabled()) {
						log.trace(
								"To call callback with port status failure. port="
										+ port + "  exception=" + e, e);
					}
					callback.onFailure(e);
				}
			}
		});

		// for the moment, we always return null assuming the submission is
		// always successful.
		return null;
	}

}
