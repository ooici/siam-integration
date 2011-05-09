package siam;

import java.util.List;
import java.util.Map;
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

    public String listPorts(final AsyncCallback<List<PortItem>> callback) {
        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (log.isDebugEnabled()) {
            log.debug("submitting request for listPorts");
        }

        es.submit(new Runnable() {
            public void run() {
                try {
                    List<PortItem> result = siam.listPorts();
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with list of ports. "
                                + "result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with listPorts failure. "
                                + "exception=" + e, e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
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
                                + port + "  result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port status failure. port="
                                + port + "  exception=" + e,
                                e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
    }

    public String getPortLastSample(final String port,
            final AsyncCallback<InstrumentSample> callback) {

        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (log.isDebugEnabled()) {
            log.debug("submitting request for port last sample. port=" + port);
        }

        es.submit(new Runnable() {
            public void run() {
                try {
                    InstrumentSample result = siam.getPortLastSample(port);
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port last sample. port="
                                + port + "  result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port last sample failure. port="
                                + port + "  exception=" + e,
                                e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
    }

    public String getPortChannels(final String port,
            final AsyncCallback<List<String>> callback) {

        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (log.isDebugEnabled()) {
            log.debug("submitting request for port channels. port=" + port);
        }

        es.submit(new Runnable() {
            public void run() {
                try {
                    List<String> result = siam.getPortChannels(port);
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port channels result. port="
                                + port + "  result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port last sample failure. port="
                                + port + "  exception=" + e,
                                e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
    }

    public String getPortProperties(final String port,
            final AsyncCallback<Map<String, String>> callback) {

        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (log.isDebugEnabled()) {
            log.debug("submitting request for port properties. port=" + port);
        }

        es.submit(new Runnable() {
            public void run() {
                try {
                    Map<String, String> result = siam.getPortProperties(port);
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with properties. port="
                                + port + "  result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with port properties failure. port="
                                + port + "  exception=" + e,
                                e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
    }

    public String setPortProperties(final String port,
            final Map<String, String> params,
            final AsyncCallback<Map<String, String>> callback) {

        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (log.isDebugEnabled()) {
            log.debug("submitting request for setting port properties. port="
                    + port);
        }

        es.submit(new Runnable() {
            public void run() {
                try {
                    Map<String, String> result = siam.setPortProperties(port,
                            params);
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with setted properties. port="
                                + port + "  result=" + result);
                    }
                    callback.onSuccess(result);
                }
                catch (Throwable e) {
                    if (log.isTraceEnabled()) {
                        log.trace("To call callback with set port properties failure. port="
                                + port + "  exception=" + e,
                                e);
                    }
                    callback.onFailure(e);
                }
            }
        });

        // we always return null assuming the submission is always successful.
        return null;
    }

}
