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
	private Thread thread;
	
	public SiamCiAdapterGpb() {
	}
	

	public void start() throws Exception {
		siamCiProcess = new SiamCiServerGpb();	
		thread = new Thread(siamCiProcess);
		log.info("starting process");
		thread.start();
	}

	public void stop() {
		siamCiProcess.stop();
		thread.interrupt();
	}
}
