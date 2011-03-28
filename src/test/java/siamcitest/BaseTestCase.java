package siamcitest;

import org.testng.SkipException;
import org.testng.annotations.Listeners;


/**
 * A base class for tests in this project.
 * 
 * @author carueda
 */
@Listeners( { siamcitest.ScTestListener.class } )
public abstract class BaseTestCase {

	/**
	 * Call this in a unit test to skip it if the condition is true.
	 * This basically throws a {@link SkipException} with the given message if the
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
