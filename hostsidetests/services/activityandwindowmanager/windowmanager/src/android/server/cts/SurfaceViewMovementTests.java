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
 * limitations under the License.
 */

package android.server.cts;

import static android.server.cts.StateLogger.logE;

import java.util.List;
import java.util.ArrayList;
import java.awt.Rectangle;

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.CollectingOutputReceiver;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.DeviceTestCase;

import android.server.cts.ActivityManagerTestBase;
import android.server.cts.WindowManagerState.WindowState;

public class SurfaceViewMovementTests extends SurfaceViewTests {
    private List<WindowState> mWindowList = new ArrayList();

    @Override
    String activityName() {
        return "MovingSurfaceViewTestActivity";
    }

    Object monitor = new Object();
    boolean testPassed = false;
    String svName = null;
    String mainName = null;

    SurfaceTraceReceiver.SurfaceObserver observer = new SurfaceTraceReceiver.SurfaceObserver() {
        int transactionCount = 0;
        boolean sawSVMove = false;
        boolean sawMainMove = false;
        int timesSeen = 0;

        @Override
        public void openTransaction() {
            transactionCount++;
            sawSVMove = false;
            sawMainMove = false;
        }

        @Override
        public void closeTransaction() {
            transactionCount--;
            if (transactionCount != 0) {
                return;
            }
            synchronized (monitor) {
                if (sawSVMove ^ sawMainMove ) {
                    monitor.notifyAll();
                    return;
                }
                if (timesSeen > 10) {
                    testPassed = true;
                    monitor.notifyAll();
                }
            }
        }

        @Override
        public void setPosition(String windowName, float x, float y) {
            if (windowName.equals(svName)) {
                sawSVMove = true;
                timesSeen++;
            } else if (windowName.equals(mainName)) {
                sawMainMove = true;
            }
        }
    };

    /**
     * Here we test that a SurfaceView moves in the same transaction
     * as its parent. We launch an activity with a SurfaceView which will
     * move around its own main window. Then we listen to WindowManager transactions.
     * Since the SurfaceView is static within the window, if we ever see one of
     * them move xor the other one we have a problem!
     */
    public void testSurfaceMovesWithParent() throws Exception {
        doFullscreenTest("MovesWithParent",
            (WindowState parent, WindowState sv) -> {
                    svName = sv.getName();
                    mainName = parent.getName();
                    installSurfaceObserver(observer);
                    try {
                        synchronized (monitor) {
                            monitor.wait(5000);
                        }
                    } catch (InterruptedException e) {
                    } finally {
                        assertTrue(testPassed);
                        removeSurfaceObserver();
                    }
            });
    }
}
