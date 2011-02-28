package test.siam.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple utility dispatcher.
 * @author carueda
 */
public class UtilRunner {
	
	/**
	 * Main dispatcher program.
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		new UtilRunner()._run(args);
	}
	
	/** Never returns */
	private void _usage() {
		System.out.printf(
				"USAGE: program [arguments]%n" +
				"  where program is one of %s or a fully qualified class name%n" +
				"Example: listPorts localhost -stats%n",
				_shortNames.keySet()
		);
		System.exit(0);
	}
	
	/** 
	 * Runs a program.
	 * First argument is the name of the program (a known short name, or a fully qualified
	 * class name, and the remaining arguments to be passed to the main method of the
	 * indicated class.
	 */
	private void _run(String[] args) throws Exception {
		if ( args.length == 0 || args[0].matches(".*help") ) {
			_usage();
		}
		
		String programName = args[0];
		String[] programArgs = Arrays.copyOfRange(args, 1, args.length);
		
		Class<?> clazz = _findClass(programName);
	    Method main = clazz.getDeclaredMethod("main", String[].class);

		assert main != null;
		main.invoke(null, (Object) programArgs);
	}

	/** gets the class corresponding to the given name, which may be one of the
	 *  known (hard-coded) short names, or a fully qualified class name.
	 */
	private Class<?> _findClass(String programName) throws ClassNotFoundException {
		Class<?> clazz = _shortNames.get(programName);
		if ( clazz != null ) {
			return clazz;
		}
		clazz = Class.forName(programName);
		return clazz;
	}
	
	/** some known "main" classes */
	private static Map<String, Class<?>> _shortNames = new HashMap<String, Class<?>>();
	static {
		_shortNames.put("listPorts", org.mbari.siam.operations.utils.PortLister.class);
		_shortNames.put("samplePort", org.mbari.siam.operations.utils.PortSampler.class);
		_shortNames.put("getLastSample", org.mbari.siam.operations.utils.GetLastSample.class);
	}

}