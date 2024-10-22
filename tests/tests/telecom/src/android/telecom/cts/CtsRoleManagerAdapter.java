/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.telecom.cts;

import static android.telecom.cts.TestUtils.WAIT_FOR_STATE_CHANGE_TIMEOUT_MS;
import static android.telecom.cts.TestUtils.executeShellCommand;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
// import android.app.role.RoleManager;
// import android.app.role.RoleManagerCallback;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CtsRoleManagerAdapter {

    private static final String TAG = CtsRoleManagerAdapter.class.getSimpleName();
    private static final String ROLE_COMPANION_APP = "android.app.role.CALL_COMPANION_APP";
    private static final String ROLE_CAR_MODE_DIALER_APP = "android.app.role.CAR_MODE_DIALER_APP";
    private static final String COMMAND_ADD_OR_REMOVE_CALL_COMPANION_APP =
            "telecom add-or-remove-call-companion-app";
    private static final String COMMAND_SET_AUTO_MODE_APP = "telecom set-test-auto-mode-app";

    private Context mContext;
    // private RoleManager mRoleManager;
    private Instrumentation mInstrumentation;
    private ConcurrentHashMap<String, List<String>> mRoleHolders;

    public CtsRoleManagerAdapter(Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
        mContext = instrumentation.getContext();
        // mRoleManager = (RoleManager) mContext.getSystemService(Context.ROLE_SERVICE);
        mRoleHolders = new ConcurrentHashMap<>();
    }

    public void addCompanionAppRoleHolder(String packageName)
            throws Exception {
        // if (mRoleManager != null) {
        //     addRoleHolder(ROLE_COMPANION_APP, packageName);
        // } else {
            String command = String.format("%s %s %d",
                    COMMAND_ADD_OR_REMOVE_CALL_COMPANION_APP, packageName, 1);
            executeShellCommand(mInstrumentation, command);
            addRoleHolderToMap(ROLE_COMPANION_APP, packageName);
        // }
    }

    public void removeCompanionAppRoleHolder(String packageName) throws Exception {
        // if (mRoleManager != null) {
        //     removeRoleHolder(ROLE_COMPANION_APP, packageName);
        // } else {
            String command = String.format("%s %s %d",
                    COMMAND_ADD_OR_REMOVE_CALL_COMPANION_APP, packageName, 0);
            executeShellCommand(mInstrumentation, command);
            removeRoleHolderFromMap(ROLE_COMPANION_APP, packageName);
        // }
    }

    public void addAutomotiveRoleHolder(String packageName)
            throws Exception {
        // if (mRoleManager != null) {
        //     addRoleHolder(ROLE_CAR_MODE_DIALER_APP, packageName);
        // } else {
            String command = String.format("%s %s",
                    COMMAND_SET_AUTO_MODE_APP, packageName);
            executeShellCommand(mInstrumentation, command);
            addRoleHolderToMap(ROLE_CAR_MODE_DIALER_APP, packageName);
        // }
    }

    public void removeAutomotiveRoleHolder(String packageName) throws Exception {
        // if (mRoleManager != null) {
        //     removeRoleHolder(ROLE_CAR_MODE_DIALER_APP, packageName);
        // } else {
            removeRoleHolderFromMap(ROLE_CAR_MODE_DIALER_APP, packageName);

            // Reset the car mode ui to rest of automotive apps assigned. If no other automotive
            // apps are available, set car mode ui back to null.
            if (mRoleHolders.containsKey(ROLE_CAR_MODE_DIALER_APP)) {
                String nextPackage = getRoleHolders(ROLE_CAR_MODE_DIALER_APP).get(0);
                String command = String.format("%s %s",
                        COMMAND_SET_AUTO_MODE_APP, nextPackage);
                executeShellCommand(mInstrumentation, command);
            } else {
                executeShellCommand(mInstrumentation, COMMAND_SET_AUTO_MODE_APP);
            }
        // }
    }

    public List<String> getRoleHolders(String role) {
        // if (mRoleManager != null) {
        //     return getRoleHolder(role);
        // } else {
            return mRoleHolders.containsKey(role) ?
                    mRoleHolders.get(role) : new LinkedList<>();
        // }
    }

    private void addRoleHolderToMap(String role, String packageName) {
        if (!mRoleHolders.containsKey(role)) {
            mRoleHolders.put(role, new LinkedList<>());
        }

        List<String> roleHolders = mRoleHolders.get(role);
        if (!roleHolders.contains(packageName)) {
            roleHolders.add(packageName);
        }
    }

    private void removeRoleHolderFromMap(String role, String packageName) {
        List<String> companionAppRoleHolders = mRoleHolders.get(role);
        if (companionAppRoleHolders == null) {
            return;
        }

        companionAppRoleHolders.remove(packageName);
        if (companionAppRoleHolders.isEmpty()) {
            mRoleHolders.remove(role);
        }
    }

    /**
     * TODO: uncomment when role manager is ready in aosp
    private List<String> getRoleHolder(String roleName) {
        List<String> holders = new ArrayList<>();
        runWithShellPermissionIdentity(() -> {
            List<String> previousHolders = mRoleManager.getRoleHolders(roleName);
            if (previousHolders != null && !previousHolders.isEmpty()) {
                holders.addAll(previousHolders);
            }
        });
        return holders;
    }

    private void addRoleHolder(String roleName, String packageName) throws InterruptedException {
        UserHandle user = Process.myUserHandle();
        Executor executor = mContext.getMainExecutor();
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>(1);
        runWithShellPermissionIdentity(() -> {
            mRoleManager.addRoleHolderAsUser(roleName, packageName, user, executor,
                    new RoleManagerCallback() {
                        @Override
                        public void onSuccess() {
                            q.add(roleName + packageName);
                        }

                        @Override
                        public void onFailure() {
                            Log.e(TAG, "Add role holder failed.");
                        }
                    });
        });
        String res = q.poll(WAIT_FOR_STATE_CHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(roleName + packageName, res);
    }

    private void removeRoleHolder(String roleName, String packageName)
            throws InterruptedException {
        UserHandle user = Process.myUserHandle();
        Executor executor = mContext.getMainExecutor();
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>(1);
        runWithShellPermissionIdentity(() -> {
            mRoleManager.removeRoleHolderAsUser(roleName, packageName, user, executor,
                    new RoleManagerCallback() {
                        @Override
                        public void onSuccess() {
                            q.add(roleName + packageName);
                        }

                        @Override
                        public void onFailure() {
                            Log.e(TAG, "Remove role holder failed.");
                        }
                    });
        });
        String res = q.poll(WAIT_FOR_STATE_CHANGE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(roleName + packageName, res);
    }*/


}
