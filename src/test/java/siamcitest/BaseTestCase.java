package siamcitest;

import org.testng.SkipException;


/**
 * A base class for tests in this project.
 * 
 * @author carueda
 */
public abstract class BaseTestCase {

	/**
	 * Call this in a unit test to skip it if the condition is true.
	 * This basically throws a {@link SkipException} with the given message if th
	 * condition is true. 
	 * 
	 * @param condition the condition 
	 * @param msg the message
	 */
	protected void skipIf(boolean condition, String msg) {
		if ( condition ) {
			throw new SkipException(msg);
		}
	}
}
