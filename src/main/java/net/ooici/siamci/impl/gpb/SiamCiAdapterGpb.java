package net.ooici.siamci.impl.gpb;

import net.ooici.siamci.ISiamCiAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation using the GPBs directly.
 * TODO incomplete
 * 
 * @author carueda
 */
public class SiamCiAdapterGpb implements ISiamCiAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiAdapterGpb.class);
	
	
	private SiamCiServerGpb siamCiProcess;
	
	public SiamCiAdapterGpb() {
	}
	

	public void start() throws Exception {
		siamCiProcess = new SiamCiServerGpb();	
		log.info("starting process");
		siamCiProcess.run();
	}

	public void stop() {
		siamCiProcess.stop();
	}
}
