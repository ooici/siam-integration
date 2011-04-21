package net.ooici.siamci.impl.gpb;

import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestDispatcher;
import net.ooici.siamci.ISiamCiAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation using the GPBs directly.
 * 
 * @author carueda
 */
public class SiamCiAdapterGpb implements ISiamCiAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiAdapterGpb.class);
	
	private final IRequestDispatcher requestProcessor;
	private SiamCiServerGpb siamCiProcess;
	
	public SiamCiAdapterGpb(IRequestDispatcher requestProcessor) {
		this.requestProcessor = requestProcessor;
	}
	

	public void start() throws Exception {
		siamCiProcess = new SiamCiServerGpb(requestProcessor);	
		log.info("starting process");
		siamCiProcess.run();
	}

	public void stop() {
		siamCiProcess.stop();
	}


    public IPublisher getPublisher() {
        // TODO Auto-generated method stub
        return null;
    }
}
