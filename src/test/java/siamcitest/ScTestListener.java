package siamcitest;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import static siamcitest.ScTestUtils.TC.*;

/**
 * Shows some info while tests are performed somewhat similar to Twisted Trial.
 * 
 * @author carueda
 */
public class ScTestListener extends TestListenerAdapter {

	private static final String OK = green("     [OK]");
	private static final String FAIL = red("   [FAIL]");
	private static final String SKIPPED = blue("[SKIPPED]");
	private static final String FAIL2 = yellow("  [FAIL2]");

	@Override
	public void onTestStart(ITestResult result) {
		System.out.printf("    %-70s", result.getName() + " ...");
		System.out.flush();
		super.onTestStart(result);
	}

	@Override
	public void onTestSuccess(ITestResult tr) {
		System.out.println(OK);
		super.onTestSuccess(tr);
	}

	@Override
	public void onTestFailure(ITestResult tr) {
		System.out.println(FAIL);
		super.onTestFailure(tr);
	}

	@Override
	public void onTestSkipped(ITestResult tr) {
		System.out.println(SKIPPED);
		super.onTestSkipped(tr);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		System.out.println(FAIL2);
		super.onTestFailedButWithinSuccessPercentage(result);
	}

	@Override
	public void onFinish(ITestContext context) {
		System.out.println();
		super.onFinish(context);
	}
}
