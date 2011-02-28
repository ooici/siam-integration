package net.ooici.siamci.impl.ionmsg;

import net.ooici.siamci.ISiam;
import net.ooici.siamci.ISiamCiAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation using the ION messaging APIs.
 * TODO NOTE: incomplete
 * 
 * @author carueda
 */
public class SiamCiAdapterIonMsg implements ISiamCiAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiAdapterIonMsg.class);
	
	
	private SiamCiServerIonMsg siamCiProcess;
	private Thread thread;
	
	public SiamCiAdapterIonMsg(ISiam siam) {
	}
	

	public void start() throws Exception {
		siamCiProcess = new SiamCiServerIonMsg();	
		thread = new Thread(siamCiProcess);
		log.info("starting process");
		thread.start();
	}

	public void stop() {
		siamCiProcess.stop();
		thread.interrupt();
	}

}
