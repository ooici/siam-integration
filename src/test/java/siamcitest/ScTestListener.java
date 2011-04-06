package siamcitest;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import static siamcitest.ScTestUtils.TC.*;

/**
 * Prints some info to stdout while tests are performed, somewhat similar to
 * Twisted Trial. This is only enabled if the env variable tcolor=y is set,
 * which may be used when a single class is being tested. For example, from
 * maven:<br/> {@code tcolor=y mvn test -Dtest=SiamTestCase}. <br/>
 * Otherwise, the output will likely be messed up because of the parallel
 * execution of multiple test classes.
 * 
 * @author carueda
 */
public class ScTestListener extends TestListenerAdapter {

	private static final boolean enabled = "y".equals(System.getenv("tcolor"));

	private static final String OK = green("     [OK]");
	private static final String FAIL = red("   [FAIL]");
	private static final String SKIPPED = blue("[SKIPPED]");
	private static final String FAIL2 = yellow("  [FAIL2]");

	@Override
	public void onTestStart(ITestResult result) {
		if (enabled) {
			System.out.printf("    %-70s", result.getName() + " ...");
			System.out.flush();
		}
		super.onTestStart(result);
	}

	@Override
	public void onTestSuccess(ITestResult tr) {
		if (enabled) {
			System.out.println(OK);
		}
		super.onTestSuccess(tr);
	}

	@Override
	public void onTestFailure(ITestResult tr) {
		if (enabled) {
			System.out.println(FAIL);
		}
		super.onTestFailure(tr);
	}

	@Override
	public void onTestSkipped(ITestResult tr) {
		if (enabled) {
			System.out.println(SKIPPED);
		}
		super.onTestSkipped(tr);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		if (enabled) {
			System.out.println(FAIL2);
		}
		super.onTestFailedButWithinSuccessPercentage(result);
	}

	@Override
	public void onFinish(ITestContext context) {
		if (enabled) {
			System.out.println();
		}
		super.onFinish(context);
	}
}
