package net.ooici.siamci.impl;

import net.ooici.siamci.ISiamCiAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author carueda
 */
class SiamCiAdapterImpl implements ISiamCiAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiAdapterImpl.class);
	
	
	private SiamCiProcess siamCiProcess;
	private Thread thread;
	
	SiamCiAdapterImpl() {
		
	}
	

	public void start() throws Exception {
		siamCiProcess = new SiamCiProcess();	
		thread = new Thread(siamCiProcess);
		log.info("starting process");
		thread.start();
	}

	public void stop() {
		siamCiProcess.stop();
		thread.interrupt();
	}

}
