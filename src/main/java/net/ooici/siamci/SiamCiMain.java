package net.ooici.siamci;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.ooici.siamci.impl.SiamCiFactoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.IAsyncSiam;
import siam.ISiam;
import siam.PortItem;


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
		private static final String DEFAULT_BROKER_HOST = "localhost";
		private static final int DEFAULT_BROKER_PORT = com.rabbitmq.client.AMQP.PROTOCOL.PORT;
		private static final String DEFAULT_QUEUE_NAME = SiamCiConstants.DEFAULT_QUEUE_NAME;
		
		private static final OptionParser parser = new OptionParser();
		
		// siam
		private static final OptionSpec<String> siamHostOpt = 
			parser.accepts("siam", "SIAM node host")
			.withRequiredArg()
			.describedAs("host")
			.defaultsTo(DEFAULT_SIAM_NODE_HOST);

		// broker
		private static final OptionSpec<String> brokerHostOpt = 
			parser.accepts("brokerHost", "Broker host")
			.withRequiredArg()
			.describedAs("host")
			.defaultsTo(DEFAULT_BROKER_HOST);
		private static final OptionSpec<String> brokerPortOpt = 
			parser.accepts("brokerPort", "Broker port")
			.withRequiredArg()
			.describedAs("port")
			.defaultsTo(String.valueOf(DEFAULT_BROKER_PORT));
		
		// queue
		private static final OptionSpec<String> queueNameOpt = 
			parser.accepts("queue", "Queue name")
			.withRequiredArg()
			.describedAs("name")
			.defaultsTo(DEFAULT_QUEUE_NAME);
		
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
		String brokerHost;
		int brokerPort;
		String queueName;
		
		private Params(OptionSet options) {
			siamHost = options.valueOf(siamHostOpt);
			brokerHost = options.valueOf(brokerHostOpt);
			brokerPort = Integer.parseInt(options.valueOf(brokerPortOpt));
			queueName = options.valueOf(queueNameOpt);
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
	private final IAsyncSiam asyncSiam;
	private final IRequestDispatcher requestDispatcher;
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
		asyncSiam = siamCiFactory.createAsyncSiam(siam);
		requestDispatcher = siamCiFactory.createRequestDispatcher(siam);
		requestDispatcher.setAsyncSiam(asyncSiam);
		siamCiAdapter = _createSiamCiAdapter();
	}
	
	private ISiamCiAdapter _createSiamCiAdapter() {
		return siamCiFactory.createSiamCiAdapter(
				params.brokerHost, params.brokerPort, params.queueName,
				requestDispatcher);
	}

	private void _run() throws Exception {
		_setShutdownHook();
		_showPorts();
		_startAdapter();
	}
	
	private void _showPorts() {
		log.info("Listing instruments in the SIAM node:");
		List<PortItem> ports;
		try {
			ports = siam.listPorts();
		}
		catch (Exception e) {
			log.warn("Error retrieving list of ports", e);
			return;
		}
		
		for ( PortItem pi : ports ) {
			log.info(" * port='" +pi.portName+ "' device='" +pi.deviceId+ "' service='" +pi.serviceName+ "'");
		}
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
