package net.ooici.siamci;

import net.ooici.siamci.impl.SiamCiFactoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Main SiamCi program.
 * 
 * NOTE: preliminary implementation
 * 
 * @author carueda
 */
public class SiamCiMain {
	
	private static final Logger log = LoggerFactory.getLogger(SiamCiMain.class);
	
	/**
	 * Launches the SIAM-CI adapter program.
	 * 
	 * @param args
	 * 		Currently, only the host of the SIAM node is accepted. By default, "localhost"
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String host = args.length == 0 ? "localhost" : args[0];
		new SiamCiMain(host);
	}
	
	
	private final ISiamCiFactory siamCiFactory;

	private final ISiam siam;
	private final IRequestProcessor requestProcessor;
	private final ISiamCiAdapter siamCiAdapter;

	/**
	 * Creates the main object of the SIAM-CI adapter.
	 * @param host
	 * @throws Exception
	 */
	SiamCiMain(String host) throws Exception {
		siamCiFactory = new SiamCiFactoryImpl();
		
		siam = siamCiFactory.createSiam(host);
		requestProcessor = siamCiFactory.createRequestProcessor(siam);

		siamCiAdapter = siamCiFactory.createSiamCiAdapter(requestProcessor);
		
		log.info("siamCiFactory: " +siamCiFactory.getClass().getName());
		log.info("siam: " +siam.getClass().getName());
		log.info("requestProcessor: " +requestProcessor.getClass().getName());
		log.info("siamCiAdapter: " +siamCiAdapter.getClass().getName());
		

		// start the adapter
		siamCiAdapter.start();
	}
}
