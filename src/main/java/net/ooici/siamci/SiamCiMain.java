package net.ooici.siamci;

import net.ooici.siamci.impl.SiamCiFactoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main SiamCi program.
 * 
 * TODO preliminary implementation
 * 
 * @author carueda
 */
public class SiamCiMain {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiMain.class);
	
	public static void main(String[] args) throws Exception {
		new SiamCiMain(args);
	}
	
	private final ISiamCiFactory siamCiFactory;
	
	private final ISiamCiAdapter siamCiAdapter;

	SiamCiMain(String[] args) throws Exception {
		siamCiFactory = new SiamCiFactoryImpl();
		siamCiAdapter = siamCiFactory.createSiamCiAdapter();
		
		log.info("siamCiFactory: " +siamCiFactory.getClass().getName());
		log.info("siamCiAdapter: " +siamCiAdapter.getClass().getName());
		

		// start the adapter
		siamCiAdapter.start();
	}
}
