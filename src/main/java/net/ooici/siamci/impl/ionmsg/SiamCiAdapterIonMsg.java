package net.ooici.siamci.impl.ionmsg;

import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.ISiamCiAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation using the ION messaging APIs.
 * 
 * @author carueda
 */
public class SiamCiAdapterIonMsg implements ISiamCiAdapter {

    private static final Logger log = LoggerFactory.getLogger(SiamCiAdapterIonMsg.class);

    private final String brokerHost;
    private final int brokerPort;
    private final String queueName;
    private final String ionExchange;

    private SiamCiServerIonMsg siamCiProcess;
    private Thread thread;

    private final IRequestProcessors requestProcessors;

    /**
     * Creates the adapter.
     * 
     * @param brokerHost
     * @param brokerPort
     * @param queueName
     * @param ionExchange
     * @param requestProcessors
     * @param dataManagers
     */
    public SiamCiAdapterIonMsg(String brokerHost, int brokerPort,
            String queueName, String ionExchange,
            IRequestProcessors requestProcessors) {

        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.queueName = queueName;
        this.ionExchange = ionExchange;
        this.requestProcessors = requestProcessors;
    }

    public void start() throws Exception {
        siamCiProcess = new SiamCiServerIonMsg(brokerHost,
                brokerPort,
                queueName,
                ionExchange,
                requestProcessors);

        thread = new Thread(siamCiProcess, siamCiProcess.getClass()
                .getSimpleName());

        if (log.isDebugEnabled()) {
            log.debug("Starting thread " + thread.getName());
        }
        thread.start();
    }

    public IPublisher getPublisher() {
        return siamCiProcess;
    }

    /**
     * It requests that the process complete, and makes the current thread wait
     * until that process completes, but only up to a maximum of a few seconds.
     * If after this wait the process seems to be still running, then {@code
     * interrupt()} is called on the associated process' thread.
     */
    public void stop() {
        if (siamCiProcess != null) {
            siamCiProcess.stop();
            _waitAndInterruptIfNecessary();
            siamCiProcess = null;
        }
    }

    /**
     * Allows some time for the process to terminate by itself before
     * interrupting the thread.
     */
    private void _waitAndInterruptIfNecessary() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Waiting for process to complete by itself...");
            }
            // wait a maximum of ~8 seconds
            for (int remaining = 8 * 1000; remaining > 0
                    && siamCiProcess.isRunning(); remaining -= 200) {
                Thread.sleep(200);
            }
        }
        catch (InterruptedException ignore) {
            if (log.isDebugEnabled()) {
                log.debug("Interrupted while waiting for process to complete");
            }
        }

        if (siamCiProcess.isRunning()) {
            if (log.isDebugEnabled()) {
                log.debug("Process still running. Interrupting thread...");
            }
            thread.interrupt();
        }
    }

}
