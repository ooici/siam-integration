package net.ooici.siamci.impl.reqproc;

import java.util.Map;

import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.SiamCi;
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

    public void setAsyncSiam(IAsyncSiam asyncSiam) {
        this.asyncSiam = asyncSiam;
    }

    public void setSiam(ISiam siam) {
        this.siam = siam;
    }

    protected IPublisher _getPublisher() {
        return SiamCi.instance().getPublisher();
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
        if (_getPublisher() == null) {
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

    /**
     * Gets the port properties for the given instrument
     * 
     * @param port
     *            the port associated with the desired instrumetn
     * @return
     */
    protected Map<String, String> _getPortProperties(String port) {
        try {
            Map<String, String> props = siam.getPortProperties(port);
            return props;
        }
        catch (Exception e) {
            return null;
        }
    }

}
