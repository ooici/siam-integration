package net.ooici.siamci;

import java.io.IOException;
import java.util.Arrays;

import net.ooici.siamci.impl.SiamCiFactoryImpl;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

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
	 * Gets the parameters for the program.
	 */
	private static class Params {
		private static final String DEFAULT_SIAM_NODE_HOST = "localhost";
		
		private static final OptionParser parser = new OptionParser();
		private static final OptionSpec<String> siamHostOpt = 
			parser.accepts("siam", "SIAM node host")
			.withRequiredArg()
			.describedAs("host")
			.defaultsTo(DEFAULT_SIAM_NODE_HOST);
		private static final OptionSpec<String> helpOpt = 
			parser.acceptsAll(Arrays.asList("help", "?"), "print help message").withOptionalArg();

		/** 
		 * Gets the parameters for the program by parsing the given arguments
		 * 
		 * @param args the arguments
		 * @return the params;  null, if the help option was given
		 */
		static Params getParams(String[] args) {
			OptionSet options = parser.parse(args);
			if ( options.has(helpOpt) ) {
				try {
					parser.printHelpOn(System.out);
				} 
				catch (IOException e) {
					// should not happen
					e.printStackTrace();
				}
				return null;
			}
			return new Params(options);
		}
		
		String siamHost;
		
		private Params(OptionSet options) {
			siamHost = options.valueOf(siamHostOpt);
		}
	}
	
	/**
	 * Launches the SIAM-CI adapter program.
	 * 
	 * @param args
	 * 		Currently, only the host of the SIAM node is accepted. By default, "localhost".
	 *      --help prints a usage message.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName(SiamCiMain.class.getSimpleName());
		Params params = Params.getParams(args);
		if ( params != null ) {
			new SiamCiMain(params)._run();
		}
	}
	
	
	///////////////////////////////////////////////////////////
	// instance.
	///////////////////////////////////////////////////////////
	
	private Params params;
	private final ISiamCiFactory siamCiFactory;

	private final ISiam siam;
	private final IRequestProcessor requestProcessor;
	private final ISiamCiAdapter siamCiAdapter;

	/**
	 * Creates the main object of the SIAM-CI adapter.
	 * @param params program parameters
	 * @throws Exception 
	 */
	private SiamCiMain(Params params) throws Exception {
		this.params = params;
		siamCiFactory = new SiamCiFactoryImpl();
		siam = siamCiFactory.createSiam(params.siamHost);
		requestProcessor = siamCiFactory.createRequestProcessor(siam);
		siamCiAdapter = siamCiFactory.createSiamCiAdapter(requestProcessor);
	}
	
	private void _run() throws Exception {
		_setShutdownHook();
		_startAdapter();
	}
	
	private void _startAdapter() throws Exception {
		log.info("Starting SIAM-CI adapter. SIAM node host: " +params.siamHost);
		siamCiAdapter.start();
	}
	
	private void _setShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown") {
        	public void run() {
        		log.info("Stopping program ...");
        		if ( siamCiAdapter != null ) {
        			try {
        				siamCiAdapter.stop();
					} 
        			catch (Exception e) {
        				log.warn("exception while stopping adapter", e);
					}
        		}
        	}
        });
	}
	
}
