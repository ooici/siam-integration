package siamcitest;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * Shows some info while tests are performed somewhat similar to Twisted Trial.
 * 
 * @author carueda
 */
public class ScTestListener extends TestListenerAdapter {

	@Override
	public void onTestStart(ITestResult result) {
		System.out.printf("    %-70s", result.getName() + " ...");
		System.out.flush();
		super.onTestStart(result);
	}

	@Override
	public void onTestSuccess(ITestResult tr) {
		System.out.printf("%10s%n", "[OK]");
		super.onTestSuccess(tr);
	}

	@Override
	public void onTestFailure(ITestResult tr) {
		System.out.printf("%10s%n", "[FAIL]");
		super.onTestFailure(tr);
	}

	@Override
	public void onTestSkipped(ITestResult tr) {
		System.out.printf("%10s%n", "[SKIPPED]");
		super.onTestSkipped(tr);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		System.out.printf("%10s%n", "[FAIL2]");
		super.onTestFailedButWithinSuccessPercentage(result);
	}

	@Override
	public void onFinish(ITestContext context) {
		System.out.println();
		super.onFinish(context);
	}
}
