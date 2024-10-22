/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */
package android.jobscheduler.cts;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.jobscheduler.MockJobService;
import android.jobscheduler.TriggerContentJobService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.android.compatibility.common.util.SystemUtil;

import java.io.IOException;

/**
 * Common functionality from which the other test case classes derive.
 */
@TargetApi(21)
public abstract class ConstraintTest extends InstrumentationTestCase {
    /** Environment that notifies of JobScheduler callbacks. */
    static MockJobService.TestEnvironment kTestEnvironment =
            MockJobService.TestEnvironment.getTestEnvironment();
    static TriggerContentJobService.TestEnvironment kTriggerTestEnvironment =
            TriggerContentJobService.TestEnvironment.getTestEnvironment();
    /** Handle for the service which receives the execution callbacks from the JobScheduler. */
    static ComponentName kJobServiceComponent;
    static ComponentName kTriggerContentServiceComponent;
    JobScheduler mJobScheduler;

    Context mContext;

    static final String MY_PACKAGE = "android.jobscheduler.cts";

    static final String JOBPERM_PACKAGE = "android.jobscheduler.cts.jobperm";
    static final String JOBPERM_AUTHORITY = "android.jobscheduler.cts.jobperm.provider";
    static final String JOBPERM_PERM = "android.jobscheduler.cts.jobperm.perm";

    Uri mFirstUri;
    Bundle mFirstUriBundle;
    Uri mSecondUri;
    Bundle mSecondUriBundle;
    ClipData mFirstClipData;
    ClipData mSecondClipData;

    boolean mStorageStateChanged;

    @Override
    public void injectInstrumentation(Instrumentation instrumentation) {
        super.injectInstrumentation(instrumentation);
        mContext = instrumentation.getContext();
        kJobServiceComponent = new ComponentName(getContext(), MockJobService.class);
        kTriggerContentServiceComponent = new ComponentName(getContext(),
                TriggerContentJobService.class);
        mJobScheduler = (JobScheduler) getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        mFirstUri = Uri.parse("content://" + JOBPERM_AUTHORITY + "/protected/foo");
        mFirstUriBundle = new Bundle();
        mFirstUriBundle.putParcelable("uri", mFirstUri);
        mSecondUri = Uri.parse("content://" + JOBPERM_AUTHORITY + "/protected/bar");
        mSecondUriBundle = new Bundle();
        mSecondUriBundle.putParcelable("uri", mSecondUri);
        mFirstClipData = new ClipData("JobPerm1", new String[] { "application/*" },
                new ClipData.Item(mFirstUri));
        mSecondClipData = new ClipData("JobPerm2", new String[] { "application/*" },
                new ClipData.Item(mSecondUri));
        try {
            SystemUtil.runShellCommand(getInstrumentation(), "cmd activity set-inactive "
                    + mContext.getPackageName() + " false");
        } catch (IOException e) {
            Log.w("ConstraintTest", "Failed setting inactive false", e);
        }
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        kTestEnvironment.setUp();
        kTriggerTestEnvironment.setUp();
        mJobScheduler.cancelAll();
    }

    @Override
    public void tearDown() throws Exception {
        SystemUtil.runShellCommand(getInstrumentation(), "cmd battery reset");
        if (mStorageStateChanged) {
            // Put storage service back in to normal operation.
            SystemUtil.runShellCommand(getInstrumentation(), "cmd devicestoragemonitor reset");
            mStorageStateChanged = false;
        }
    }

    /**
     * The scheduler will usually only flush its queue of unexpired jobs when the device is
     * considered to be on stable power - that is, plugged in for a period of 2 minutes.
     * Rather than wait for this to happen, we cheat and send this broadcast instead.
     */
    protected void sendExpediteStableChargingBroadcast() throws Exception {
        // Faking the device to be 90% charging and then to be 91%, so that it triggers
        // BatteryManager.ACTION_CHARGING in the upward change-level transition logic.
        SystemUtil.runShellCommand(getInstrumentation(), "cmd battery set level 90");
        SystemUtil.runShellCommand(getInstrumentation(), "cmd battery set level 91");
    }

    public void assertHasUriPermission(Uri uri, int grantFlags) {
        if ((grantFlags&Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            assertEquals(PackageManager.PERMISSION_GRANTED,
                    getContext().checkUriPermission(uri, Process.myPid(),
                            Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
        if ((grantFlags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
            assertEquals(PackageManager.PERMISSION_GRANTED,
                    getContext().checkUriPermission(uri, Process.myPid(),
                            Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        }
    }

    void waitPermissionRevoke(Uri uri, int access, long timeout) {
        long startTime = SystemClock.elapsedRealtime();
        while (getContext().checkUriPermission(uri, Process.myPid(), Process.myUid(), access)
                != PackageManager.PERMISSION_DENIED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            if ((SystemClock.elapsedRealtime()-startTime) >= timeout) {
                fail("Timed out waiting for permission revoke");
            }
        }
    }

    // Note we are just using storage state as a way to control when the job gets executed.
    void setStorageState(boolean low) throws Exception {
        mStorageStateChanged = true;
        String res;
        if (low) {
            res = SystemUtil.runShellCommand(getInstrumentation(),
                    "cmd devicestoragemonitor force-low -f");
        } else {
            res = SystemUtil.runShellCommand(getInstrumentation(),
                    "cmd devicestoragemonitor force-not-low -f");
        }
        int seq = Integer.parseInt(res.trim());
        long startTime = SystemClock.elapsedRealtime();

        // Wait for the storage update to be processed by job scheduler before proceeding.
        int curSeq;
        do {
            curSeq = Integer.parseInt(SystemUtil.runShellCommand(getInstrumentation(),
                    "cmd jobscheduler get-storage-seq").trim());
            if (curSeq == seq) {
                return;
            }
        } while ((SystemClock.elapsedRealtime()-startTime) < 1000);

        fail("Timed out waiting for job scheduler: expected seq=" + seq + ", cur=" + curSeq);
    }

    String getJobState(int jobId) throws Exception {
        return SystemUtil.runShellCommand(getInstrumentation(),
                "cmd jobscheduler get-job-state --user cur "
                        + kJobServiceComponent.getPackageName() + " " + jobId).trim();
    }

    void assertJobReady(int jobId) throws Exception {
        String state = getJobState(jobId);
        assertTrue("Job unexpectedly not ready, in state: " + state, state.contains("ready"));
    }

    void assertJobWaiting(int jobId) throws Exception {
        String state = getJobState(jobId);
        assertTrue("Job unexpectedly not waiting, in state: " + state, state.contains("waiting"));
    }

    void assertJobNotReady(int jobId) throws Exception {
        String state = getJobState(jobId);
        assertTrue("Job unexpectedly ready, in state: " + state, !state.contains("ready"));
    }
}
