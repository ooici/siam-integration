package net.ooici.siamci.impl.reqproc;

import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.utils.ScUtils;
import siam.IAsyncSiam;
import siam.ISiam;

/**
 * A base class for request processors.
 * 
 * @author carueda
 */
public abstract class BaseRequestProcessor implements IRequestProcessor {

	protected ISiam siam;
	protected IAsyncSiam asyncSiam;
	protected IPublisher publisher;

	public void setAsyncSiam(IAsyncSiam asyncSiam) {
		this.asyncSiam = asyncSiam;
	}

	public void setPublisher(IPublisher publisher) {
		this.publisher = publisher;
	}

	public void setSiam(ISiam siam) {
		this.siam = siam;
	}

	/**
	 * throws {@link IllegalStateException} if any required object for
	 * asynchronous handling is missing.
	 */
	protected void _checkAsyncSetup() {
		if (asyncSiam == null) {
			throw new IllegalStateException("No "
					+ IAsyncSiam.class.getSimpleName()
					+ " object has been associated");
		}
		if (publisher == null) {
			throw new IllegalStateException("No "
					+ IPublisher.class.getSimpleName()
					+ " object has been associated");
		}
	}
	
    /**
     * Formats a request id: helps identify the specific request among the
     * various possible concurrent log messages.
     */
	protected static String _rid(int reqId) {
        return ScUtils.formatReqId(reqId);
    }
	
}
