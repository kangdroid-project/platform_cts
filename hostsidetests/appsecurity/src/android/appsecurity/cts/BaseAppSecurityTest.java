/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.appsecurity.cts;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.TestResult;
import com.android.ddmlib.testrunner.TestRunResult;
import com.android.ddmlib.testrunner.TestResult.TestStatus;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.util.ArrayList;
import java.util.Map;

/**
 * Base class.
 */
public class BaseAppSecurityTest extends DeviceTestCase implements IBuildReceiver {
    protected static final int USER_SYSTEM = 0; // From the UserHandle class.

    private static final String RUNNER = "android.support.test.runner.AndroidJUnitRunner";

    protected IBuildInfo mBuildInfo;

    /** Whether multi-user is supported. */
    protected boolean mSupportsMultiUser;
    protected boolean mIsSplitSystemUser;
    protected int mPrimaryUserId;
    /** Users we shouldn't delete in the tests */
    private ArrayList<Integer> mFixedUsers;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuildInfo = buildInfo;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertNotNull(mBuildInfo); // ensure build has been set before test is run.

        mSupportsMultiUser = getDevice().getMaxNumberOfUsersSupported() > 1;
        mIsSplitSystemUser = checkIfSplitSystemUser();
        mPrimaryUserId = getDevice().getPrimaryUserId();
        mFixedUsers = new ArrayList();
        mFixedUsers.add(mPrimaryUserId);
        if (mPrimaryUserId != USER_SYSTEM) {
            mFixedUsers.add(USER_SYSTEM);
        }
        getDevice().switchUser(mPrimaryUserId);
        removeTestUsers();
    }

    @Override
    protected void tearDown() throws Exception {
        removeTestUsers();
        super.tearDown();
    }

    /**
     * @return the userid of the created user
     */
    protected int createUser() throws DeviceNotAvailableException, IllegalStateException {
        final String command = "pm create-user "
                + "TestUser_" + System.currentTimeMillis();
        CLog.d("Starting command: " + command);
        final String output = getDevice().executeShellCommand(command);
        CLog.d("Output for command " + command + ": " + output);

        if (output.startsWith("Success")) {
            try {
                return Integer.parseInt(output.substring(output.lastIndexOf(" ")).trim());
            } catch (NumberFormatException e) {
                CLog.e("Failed to parse result: %s", output);
            }
        } else {
            CLog.e("Failed to create user: %s", output);
        }
        throw new IllegalStateException();
    }

    private void removeTestUsers() throws Exception {
        for (int userId : getDevice().listUsers()) {
            if (!mFixedUsers.contains(userId)) {
                getDevice().removeUser(userId);
            }
        }
    }

    private boolean checkIfSplitSystemUser() throws DeviceNotAvailableException {
        final String commandOuput = getDevice().executeShellCommand(
                "getprop ro.fw.system_user_split");
        return "y".equals(commandOuput) || "yes".equals(commandOuput)
                || "1".equals(commandOuput) || "true".equals(commandOuput)
                || "on".equals(commandOuput);
    }

    protected void installTestAppForUser(String apk, int userId) throws Exception {
        if (userId < 0) {
            userId = mPrimaryUserId;
        }
        CompatibilityBuildHelper buildHelper = new CompatibilityBuildHelper(mBuildInfo);
        assertNull(getDevice().installPackageForUser(
                buildHelper.getTestFile(apk), true, false, userId, "-t"));
    }

    protected boolean isAppVisibleForUser(String packageName, int userId,
            boolean matchUninstalled) throws DeviceNotAvailableException {
        String command = "cmd package list packages --user " + userId;
        if (matchUninstalled) command += " -u";
        String output = getDevice().executeShellCommand(command);
        return output.contains(packageName);
    }

    private void printTestResult(TestRunResult runResult) {
        for (Map.Entry<TestIdentifier, TestResult> testEntry :
                runResult.getTestResults().entrySet()) {
            TestResult testResult = testEntry.getValue();
            CLog.d("Test " + testEntry.getKey() + ": " + testResult.getStatus());
            if (testResult.getStatus() != TestStatus.PASSED) {
                CLog.d(testResult.getStackTrace());
            }
        }
    }

    protected boolean runDeviceTestsAsUser(String packageName,
            String testClassName, String testMethodName, int userId) throws Exception {
        if (testClassName != null && testClassName.startsWith(".")) {
            testClassName = packageName + testClassName;
        }

        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(
                packageName, RUNNER, getDevice().getIDevice());
        if (testClassName != null && testMethodName != null) {
            testRunner.setMethodName(testClassName, testMethodName);
        } else if (testClassName != null) {
            testRunner.setClassName(testClassName);
        }

        CollectingTestListener listener = new CollectingTestListener();
        assertTrue(getDevice().runInstrumentationTestsAsUser(testRunner, userId, listener));

        TestRunResult runResult = listener.getCurrentRunResults();
        printTestResult(runResult);
        return !runResult.hasFailedTests() && runResult.getNumTestsInState(TestStatus.PASSED) > 0;
    }
}