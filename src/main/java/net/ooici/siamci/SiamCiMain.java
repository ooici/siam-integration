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
 * For usage, {@link #main(String[])}.
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
		private static final String DEFAULT_ION_EXCHANGE = SiamCiConstants.DEFAULT_ION_EXCHANGE;
		
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
		
		// ION exchange
		private static final OptionSpec<String> exchangeNameOpt = 
		    parser.accepts("exchange", "ION exchange name")
		    .withRequiredArg()
		    .describedAs("name")
		    .defaultsTo(DEFAULT_ION_EXCHANGE);
		
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
			    System.out.println("SIAM-CI Adapter Service options:");
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
		String exchangeName;
		
		private Params(OptionSet options) {
			siamHost = options.valueOf(siamHostOpt);
			brokerHost = options.valueOf(brokerHostOpt);
			brokerPort = Integer.parseInt(options.valueOf(brokerPortOpt));
			queueName = options.valueOf(queueNameOpt);
			exchangeName = options.valueOf(exchangeNameOpt);
		}
		
		public String toString() {
		    return String.format(
		            "siamHost='%s', broker='%s:%s', queueName='%s', exchangeName='%s'", 
		            siamHost, brokerHost, brokerPort, queueName, exchangeName);
		}
	}
	
	/**
	 * Launches the SIAM-CI adapter program.
	 * 
	 * @param args
	 * 		Here is the usage message: (mvn exec:java -Dsiam-ci -Dexec.args=--help)
	 * <pre>
        SIAM-CI Adapter Service options:
        Option                                  Description                            
        ------                                  -----------                            
        -?, --help                              print help message                     
        --brokerHost &lt;host>                     Broker host (default: localhost)       
        --brokerPort &lt;port>                     Broker port (default: 5672)            
        --exchange &lt;name>                       ION exchange name (default: magnet.    
                                                  topic)                               
        --queue &lt;name>                          Queue name (default: SIAM-CI)          
        --siam &lt;host>                           SIAM node host (default: localhost)    
	 * </pre> 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    // for debugging/logging purposes
		Thread.currentThread().setName(SiamCiMain.class.getSimpleName());
		
		Params params = Params.getParams(args);
		if ( params != null ) {
			new SiamCiMain(params)._start();
		}
	}
	
	
	///////////////////////////////////////////////////////////
	// instance.
	///////////////////////////////////////////////////////////
	
	private Params params;
	private final ISiamCiFactory siamCiFactory;

	private final ISiam siam;
	private final IAsyncSiam asyncSiam;
	private final IRequestProcessors requestProcessors;
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
		
		requestProcessors = siamCiFactory.createRequestProcessors(siam);
		requestProcessors.setAsyncSiam(asyncSiam);
		
		siamCiAdapter = _createSiamCiAdapter();
	}
	
	private ISiamCiAdapter _createSiamCiAdapter() {
		return siamCiFactory.createSiamCiAdapter(
				params.brokerHost, params.brokerPort, params.queueName,
				params.exchangeName,
				requestProcessors);
	}

	/**
	 * Starts the service.
	 */
	private void _start() throws Exception {
		_showServiceInfo();
		_setShutdownHook();
		_showPorts();
		_startAdapter();
	}
	
	private void _showServiceInfo() {
        log.info("Parameters: " +params);
        
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
	
	/**
	 * For a graceful termination of the adapter.
	 */
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
