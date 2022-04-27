package openjdk.jcov.data.lib;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestStatusListener implements ITestListener {

    public static volatile boolean status = true;

    @Override
    public void onTestStart(ITestResult result) { }

    @Override
    public void onTestSuccess(ITestResult result) { }

    @Override
    public void onTestFailure(ITestResult result) {
        status = false;
    }

    @Override
    public void onTestSkipped(ITestResult result) { }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }

    @Override
    public void onStart(ITestContext context) { status = true; }

    @Override
    public void onFinish(ITestContext context) { }
}

