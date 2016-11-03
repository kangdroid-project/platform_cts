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

package android.server.cts;

import static android.server.cts.ActivityManagerTestBase.FREEFORM_WORKSPACE_STACK_ID;
import static android.server.cts.ActivityManagerTestBase.PINNED_STACK_ID;
import static android.server.cts.StateLogger.log;

import android.server.cts.ActivityManagerState.ActivityStack;
import android.server.cts.ActivityManagerState.ActivityTask;
import android.server.cts.WindowManagerState.WindowStack;
import android.server.cts.WindowManagerState.WindowState;
import android.server.cts.WindowManagerState.WindowTask;

import com.android.tradefed.device.ITestDevice;

import junit.framework.Assert;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Combined state of the activity manager and window manager. */
public class ActivityAndWindowManagersState extends Assert {

    // Clone of android DisplayMetrics.DENSITY_DEFAULT (DENSITY_MEDIUM)
    // (Needed in host-side tests to convert dp to px.)
    private static final int DISPLAY_DENSITY_DEFAULT = 160;

    // Default minimal size of resizable task, used if none is set explicitly.
    // Must be kept in sync with 'default_minimal_size_resizable_task' dimen from frameworks/base.
    private static final int DEFAULT_RESIZABLE_TASK_SIZE_DP = 220;

    private ActivityManagerState mAmState = new ActivityManagerState();
    private WindowManagerState mWmState = new WindowManagerState();

    private final List<WindowManagerState.WindowState> mTempWindowList = new ArrayList<>();

    private boolean mUseActivityNames = true;

    /**
     * Compute AM and WM state of device, check sanity and bounds.
     * WM state will include only visible windows, stack and task bounds will be compared.
     *
     * @param device test device.
     * @param waitForActivitiesVisible array of activity names to wait for.
     */
    public void computeState(ITestDevice device, String[] waitForActivitiesVisible) throws Exception {
        computeState(device, waitForActivitiesVisible, true);
    }

    /**
     * Compute AM and WM state of device, check sanity and bounds.
     * WM state will include only visible windows.
     *
     * @param device test device.
     * @param waitForActivitiesVisible array of activity names to wait for.
     * @param compareTaskAndStackBounds pass 'true' if stack and task bounds should be compared,
     *                                  'false' otherwise.
     */
    void computeState(ITestDevice device, String[] waitForActivitiesVisible,
                      boolean compareTaskAndStackBounds) throws Exception {
        computeState(device, true, waitForActivitiesVisible, compareTaskAndStackBounds);
    }

    /**
     * Compute AM and WM state of device, check sanity and bounds.
     * Stack and task bounds will be compared.
     *
     * @param device test device.
     * @param visibleOnly pass 'true' to include only visible windows in WM state.
     * @param waitForActivitiesVisible array of activity names to wait for.
     */
    void computeState(ITestDevice device, boolean visibleOnly, String[] waitForActivitiesVisible)
            throws Exception {
        computeState(device, visibleOnly, waitForActivitiesVisible, true);
    }

    /**
     * Compute AM and WM state of device, check sanity and bounds.
     *
     * @param device test device.
     * @param visibleOnly pass 'true' if WM state should include only visible windows.
     * @param waitForActivitiesVisible array of activity names to wait for.
     * @param compareTaskAndStackBounds pass 'true' if stack and task bounds should be compared,
     *                                  'false' otherwise.
     */
    void computeState(ITestDevice device, boolean visibleOnly, String[] waitForActivitiesVisible,
                      boolean compareTaskAndStackBounds) throws Exception {
        waitForValidState(device, waitForActivitiesVisible, null, compareTaskAndStackBounds);

        assertSanity();
        assertValidBounds(compareTaskAndStackBounds);
    }

    /**
     * By default computeState allows you to pass only the activity name it and
     * it will generate the full window name for the main activity window. In the
     * case of secondary application windows though this isn't helpful, as they
     * may follow a different format, so this method lets you disable that behavior,
     * prior to calling a computeState variant
     */
    void setUseActivityNamesForWindowNames(boolean useActivityNames) {
        mUseActivityNames = useActivityNames;
    }

    /**
     * Compute AM and WM state of device, wait for the activity records to be added, and
     * wait for debugger window to show up.
     *
     * This should only be used when starting with -D (debugger) option, where we pop up the
     * waiting-for-debugger window, but real activity window won't show up since we're waiting
     * for debugger.
     */
    void waitForDebuggerWindowVisible(
            ITestDevice device, String[] waitForActivityRecords) throws Exception {
        int retriesLeft = 5;
        do {
            mAmState.computeState(device);
            mWmState.computeState(device);
            if (shouldWaitForDebuggerWindow() ||
                    shouldWaitForActivityRecords(waitForActivityRecords)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log(e.toString());
                    // Well I guess we are not waiting...
                }
            } else {
                break;
            }
        } while (retriesLeft-- > 0);
    }

    /**
     * Wait for consistent state in AM and WM.
     *
     * @param device test device.
     * @param waitForActivitiesVisible array of activity names to wait for.
     * @param stackIds ids of stack where provided activities should be found.
     *                 Pass null to skip this check.
     */
    void waitForValidState(ITestDevice device,
                           String[] waitForActivitiesVisible, int[] stackIds,
                           boolean compareTaskAndStackBounds) throws Exception {
        int retriesLeft = 5;
        do {
            // TODO: Get state of AM and WM at the same time to avoid mismatches caused by
            // requesting dump in some intermediate state.
            mAmState.computeState(device);
            mWmState.computeState(device);
            if (shouldWaitForValidStacks(compareTaskAndStackBounds)
                    || shouldWaitForActivities(waitForActivitiesVisible, stackIds)) {
                log("***Waiting for valid stacks and activities states...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log(e.toString());
                    // Well I guess we are not waiting...
                }
            } else {
                break;
            }
        } while (retriesLeft-- > 0);
    }

    void waitForHomeActivityVisible(ITestDevice device) throws Exception {
        waitForWithAmState(device, ActivityManagerState::isHomeActivityVisible,
                "***Waiting for home activity to be visible...");
    }

    void waitForKeyguardShowingAndNotOccluded(ITestDevice device) throws Exception {
        waitForWithAmState(device, state -> state.getKeyguardControllerState().keyguardShowing
                        && !state.getKeyguardControllerState().keyguardOccluded,
                "***Waiting for Keyguard showing...");
    }

    void waitForKeyguardGone(ITestDevice device) throws Exception {
        waitForWithAmState(device, state -> !state.getKeyguardControllerState().keyguardShowing,
                "***Waiting for Keyguard gone...");
    }

    void waitForRotation(ITestDevice device, int rotation) throws Exception {
        waitForWithWmState(device, state -> state.getRotation() == rotation,
                "***Waiting for Rotation: " + rotation);
    }

    void waitForWithAmState(ITestDevice device, Predicate<ActivityManagerState> waitCondition,
            String message) throws Exception{
        waitFor(device, (amState, wmState) -> waitCondition.test(amState), message);
    }

    void waitForWithWmState(ITestDevice device, Predicate<WindowManagerState> waitCondition,
            String message) throws Exception{
        waitFor(device, (amState, wmState) -> waitCondition.test(wmState), message);
    }

    void waitFor(ITestDevice device,
            BiPredicate<ActivityManagerState, WindowManagerState> waitCondition, String message)
            throws Exception {
        int retriesLeft = 5;
        do {
            mAmState.computeState(device);
            mWmState.computeState(device);
            if (!waitCondition.test(mAmState, mWmState)) {
                log(message);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log(e.toString());
                    // Well I guess we are not waiting...
                }
            } else {
                break;
            }
        } while (retriesLeft-- > 0);
    }

    private boolean shouldWaitForValidStacks(boolean compareTaskAndStackBounds) {
        if (!taskListsInAmAndWmAreEqual()) {
            // We want to wait for equal task lists in AM and WM in case we caught them in the
            // middle of some state change operations.
            log("***taskListsInAmAndWmAreEqual=false");
            return true;
        }
        if (!stackBoundsInAMAndWMAreEqual()) {
            // We want to wait a little for the stacks in AM and WM to have equal bounds as there
            // might be a transition animation ongoing when we got the states from WM AM separately.
            log("***stackBoundsInAMAndWMAreEqual=false");
            return true;
        }
        try {
            // Temporary fix to avoid catching intermediate state with different task bounds in AM
            // and WM.
            assertValidBounds(compareTaskAndStackBounds);
        } catch (AssertionError e) {
            log("***taskBoundsInAMAndWMAreEqual=false : " + e.getMessage());
            return true;
        }
        return false;
    }

    private boolean shouldWaitForActivities(String[] waitForActivitiesVisible, int[] stackIds) {
        if (waitForActivitiesVisible == null || waitForActivitiesVisible.length == 0) {
            return false;
        }
        // If the caller is interested in us waiting for some particular activity windows to be
        // visible before compute the state. Check for the visibility of those activity windows
        // and for placing them in correct stacks (if requested).
        boolean allActivityWindowsVisible = true;
        boolean tasksInCorrectStacks = true;
        List<WindowManagerState.WindowState> matchingWindowStates = new ArrayList<>();
        for (int i = 0; i < waitForActivitiesVisible.length; i++) {
            // Check if window is visible - it should be represented as one of the window states.
            final String windowName = mUseActivityNames ?
                    ActivityManagerTestBase.getWindowName(waitForActivitiesVisible[i])
                    : waitForActivitiesVisible[i];
            final String activityComponentName =
                    ActivityManagerTestBase.getActivityComponentName(waitForActivitiesVisible[i]);

            mWmState.getMatchingVisibleWindowState(windowName, matchingWindowStates);
            boolean activityWindowVisible = !matchingWindowStates.isEmpty();
            if (!activityWindowVisible) {
                log("Activity window not visible: " + windowName);
                allActivityWindowsVisible = false;
            } else if (!mAmState.isActivityVisible(activityComponentName)) {
                log("Activity not visible: " + activityComponentName);
                allActivityWindowsVisible = false;
            } else if (stackIds != null) {
                // Check if window is already in stack requested by test.
                boolean windowInCorrectStack = false;
                for (WindowManagerState.WindowState ws : matchingWindowStates) {
                    if (ws.getStackId() == stackIds[i]) {
                        windowInCorrectStack = true;
                        break;
                    }
                }
                if (!windowInCorrectStack) {
                    log("Window in incorrect stack: " + waitForActivitiesVisible[i]);
                    tasksInCorrectStacks = false;
                }
            }
        }
        return !allActivityWindowsVisible || !tasksInCorrectStacks;
    }

    private boolean shouldWaitForDebuggerWindow() {
        List<WindowManagerState.WindowState> matchingWindowStates = new ArrayList<>();
        mWmState.getMatchingVisibleWindowState("android.server.cts", matchingWindowStates);
        for (WindowState ws : matchingWindowStates) {
            if (ws.isDebuggerWindow()) {
                return false;
            }
        }
        log("Debugger window not available yet");
        return true;
    }

    private boolean shouldWaitForActivityRecords(String[] waitForActivityRecords) {
        if (waitForActivityRecords == null || waitForActivityRecords.length == 0) {
            return false;
        }
        // Check if the activity records we're looking for is already added.
        for (int i = 0; i < waitForActivityRecords.length; i++) {
            if (!mAmState.isActivityVisible(waitForActivityRecords[i])) {
                log("ActivityRecord " + waitForActivityRecords[i] + " not visible yet");
                return true;
            }
        }
        return false;
    }

    ActivityManagerState getAmState() {
        return mAmState;
    }

    public WindowManagerState getWmState() {
        return mWmState;
    }

    void assertSanity() throws Exception {
        assertTrue("Must have stacks", mAmState.getStackCount() > 0);
        if (!mAmState.getKeyguardControllerState().keyguardShowing) {
            assertEquals("There should be one and only one resumed activity in the system.",
                    1, mAmState.getResumedActivitiesCount());
        }
        assertNotNull("Must have focus activity.", mAmState.getFocusedActivity());

        for (ActivityStack aStack : mAmState.getStacks()) {
            final int stackId = aStack.mStackId;
            for (ActivityTask aTask : aStack.getTasks()) {
                assertEquals("Stack can only contain its own tasks", stackId, aTask.mStackId);
            }
        }

        assertNotNull("Must have front window.", mWmState.getFrontWindow());
        assertNotNull("Must have focused window.", mWmState.getFocusedWindow());
        assertNotNull("Must have app.", mWmState.getFocusedApp());
    }

    void assertContainsStack(String msg, int stackId) throws Exception {
        assertTrue(msg, mAmState.containsStack(stackId));
        assertTrue(msg, mWmState.containsStack(stackId));
    }

    void assertDoesNotContainStack(String msg, int stackId) throws Exception {
        assertFalse(msg, mAmState.containsStack(stackId));
        assertFalse(msg, mWmState.containsStack(stackId));
    }

    void assertFrontStack(String msg, int stackId) throws Exception {
        assertEquals(msg, stackId, mAmState.getFrontStackId());
        assertEquals(msg, stackId, mWmState.getFrontStackId());
    }

    void assertFocusedStack(String msg, int stackId) throws Exception {
        assertEquals(msg, stackId, mAmState.getFocusedStackId());
    }

    void assertNotFocusedStack(String msg, int stackId) throws Exception {
        if (stackId == mAmState.getFocusedStackId()) {
            failNotEquals(msg, stackId, mAmState.getFocusedStackId());
        }
    }

    void assertFocusedActivity(String msg, String activityName) throws Exception {
        final String componentName = ActivityManagerTestBase.getActivityComponentName(activityName);
        assertEquals(msg, componentName, mAmState.getFocusedActivity());
        assertEquals(msg, componentName, mWmState.getFocusedApp());
    }

    void assertNotFocusedActivity(String msg, String activityName) throws Exception {
        final String componentName = ActivityManagerTestBase.getActivityComponentName(activityName);
        if (mAmState.getFocusedActivity().equals(componentName)) {
            failNotEquals(msg, mAmState.getFocusedActivity(), componentName);
        }
        if (mWmState.getFocusedApp().equals(componentName)) {
            failNotEquals(msg, mWmState.getFocusedApp(), componentName);
        }
    }

    void assertResumedActivity(String msg, String activityName) throws Exception {
        final String componentName = ActivityManagerTestBase.getActivityComponentName(activityName);
        assertEquals(msg, componentName, mAmState.getResumedActivity());
    }

    void assertNotResumedActivity(String msg, String activityName) throws Exception {
        final String componentName = ActivityManagerTestBase.getActivityComponentName(activityName);
        if (mAmState.getResumedActivity().equals(componentName)) {
            failNotEquals(msg, mAmState.getResumedActivity(), componentName);
        }
    }

    void assertFocusedWindow(String msg, String windowName) {
        assertEquals(msg, windowName, mWmState.getFocusedWindow());
    }

    void assertNotFocusedWindow(String msg, String windowName) {
        if (mWmState.getFocusedWindow().equals(windowName)) {
            failNotEquals(msg, mWmState.getFocusedWindow(), windowName);
        }
    }

    void assertFrontWindow(String msg, String windowName) {
        assertEquals(msg, windowName, mWmState.getFrontWindow());
    }

    void assertVisibility(String activityName, boolean visible) {
        final String activityComponentName =
                ActivityManagerTestBase.getActivityComponentName(activityName);
        final String windowName =
                ActivityManagerTestBase.getWindowName(activityName);

        final boolean activityVisible = mAmState.isActivityVisible(activityComponentName);
        final boolean windowVisible = mWmState.isWindowVisible(windowName);

        if (visible) {
            assertTrue("Activity=" + activityComponentName + " must be visible.", activityVisible);
            assertTrue("Window=" + windowName + " must be visible.", windowVisible);
        } else {
            assertFalse("Activity=" + activityComponentName + " must NOT be visible.",
                    activityVisible);
            assertFalse("Window=" + windowName + " must NOT be visible.", windowVisible);
        }
    }

    void assertHomeActivityVisible(boolean visible) {
        final boolean activityVisible = mAmState.isHomeActivityVisible();

        if (visible) {
            assertTrue("Home activity must be visible.", activityVisible);
        } else {
            assertFalse("Home activity must NOT be visible.", activityVisible);
        }
    }

    boolean taskListsInAmAndWmAreEqual() {
        for (ActivityStack aStack : mAmState.getStacks()) {
            final int stackId = aStack.mStackId;
            final WindowStack wStack = mWmState.getStack(stackId);
            if (wStack == null) {
                log("Waiting for stack setup in WM, stackId=" + stackId);
                return false;
            }

            for (ActivityTask aTask : aStack.getTasks()) {
                if (wStack.getTask(aTask.mTaskId) == null) {
                    log("Task is in AM but not in WM, waiting for it to settle, taskId="
                            + aTask.mTaskId);
                    return false;
                }
            }

            for (WindowTask wTask : wStack.mTasks) {
                if (aStack.getTask(wTask.mTaskId) == null) {
                    log("Task is in WM but not in AM, waiting for it to settle, taskId="
                            + wTask.mTaskId);
                    return false;
                }
            }
        }
        return true;
    }

    boolean stackBoundsInAMAndWMAreEqual() {
        for (ActivityStack aStack : mAmState.getStacks()) {
            final int stackId = aStack.mStackId;
            final WindowStack wStack = mWmState.getStack(stackId);
            if (aStack.isFullscreen() != wStack.isFullscreen()) {
                log("Waiting for correct fullscreen state, stackId=" + stackId);
                return false;
            }

            final Rectangle aStackBounds = aStack.getBounds();
            final Rectangle wStackBounds = wStack.getBounds();

            if (aStack.isFullscreen()) {
                if (aStackBounds != null) {
                    log("Waiting for correct stack state in AM, stackId=" + stackId);
                    return false;
                }
            } else if (!Objects.equals(aStackBounds, wStackBounds)) {
                // If stack is not fullscreen - comparing bounds. Not doing it always because
                // for fullscreen stack bounds in WM can be either null or equal to display size.
                log("Waiting for stack bound equality in AM and WM, stackId=" + stackId);
                return false;
            }
        }

        return true;
    }

    /** Check task bounds when docked to top/left. */
    void assertDockedTaskBounds(int taskSize, String activityName) {
        // Task size can be affected by default minimal size.
        int defaultMinimalTaskSize = defaultMinimalTaskSize(
                mAmState.getStackById(ActivityManagerTestBase.DOCKED_STACK_ID).mDisplayId);
        int targetSize = Math.max(taskSize, defaultMinimalTaskSize);

        assertEquals(new Rectangle(0, 0, targetSize, targetSize),
                mAmState.getTaskByActivityName(activityName).getBounds());
    }

    void assertValidBounds(boolean compareTaskAndStackBounds) {
        for (ActivityStack aStack : mAmState.getStacks()) {
            final int stackId = aStack.mStackId;
            final WindowStack wStack = mWmState.getStack(stackId);
            assertNotNull("stackId=" + stackId + " in AM but not in WM?", wStack);

            assertEquals("Stack fullscreen state in AM and WM must be equal stackId=" + stackId,
                    aStack.isFullscreen(), wStack.isFullscreen());

            final Rectangle aStackBounds = aStack.getBounds();
            final Rectangle wStackBounds = wStack.getBounds();

            if (aStack.isFullscreen()) {
                assertNull("Stack bounds in AM must be null stackId=" + stackId, aStackBounds);
            } else {
                assertEquals("Stack bounds in AM and WM must be equal stackId=" + stackId,
                        aStackBounds, wStackBounds);
            }

            for (ActivityTask aTask : aStack.getTasks()) {
                final int taskId = aTask.mTaskId;
                final WindowTask wTask = wStack.getTask(taskId);
                assertNotNull(
                        "taskId=" + taskId + " in AM but not in WM? stackId=" + stackId, wTask);

                final boolean aTaskIsFullscreen = aTask.isFullscreen();
                final boolean wTaskIsFullscreen = wTask.isFullscreen();
                assertEquals("Task fullscreen state in AM and WM must be equal taskId=" + taskId
                        + ", stackId=" + stackId, aTaskIsFullscreen, wTaskIsFullscreen);

                final Rectangle aTaskBounds = aTask.getBounds();
                final Rectangle wTaskBounds = wTask.getBounds();

                if (aTaskIsFullscreen) {
                    assertNull("Task bounds in AM must be null for fullscreen taskId=" + taskId,
                            aTaskBounds);
                } else {
                    assertEquals("Task bounds in AM and WM must be equal taskId=" + taskId
                            + ", stackId=" + stackId, aTaskBounds, wTaskBounds);

                    if (compareTaskAndStackBounds && stackId != FREEFORM_WORKSPACE_STACK_ID) {
                        int aTaskMinWidth = aTask.getMinWidth();
                        int aTaskMinHeight = aTask.getMinHeight();

                        if (aTaskMinWidth == -1 || aTaskMinHeight == -1) {
                            // Minimal dimension(s) not set for task - it should be using defaults.
                            int defaultMinimalSize = defaultMinimalTaskSize(aStack.mDisplayId);

                            if (aTaskMinWidth == -1) {
                                aTaskMinWidth = defaultMinimalSize;
                            }
                            if (aTaskMinHeight == -1) {
                                aTaskMinHeight = defaultMinimalSize;
                            }
                        }

                        if (aStackBounds.getWidth() >= aTaskMinWidth
                                && aStackBounds.getHeight() >= aTaskMinHeight
                                || stackId == PINNED_STACK_ID) {
                            // Bounds are not smaller then minimal possible, so stack and task
                            // bounds must be equal.
                            assertEquals("Task bounds must be equal to stack bounds taskId="
                                    + taskId + ", stackId=" + stackId, aStackBounds, wTaskBounds);
                        } else {
                            // Minimal dimensions affect task size, so bounds of task and stack must
                            // be different - will compare dimensions instead.
                            int targetWidth = (int) Math.max(aTaskMinWidth,
                                    aStackBounds.getWidth());
                            assertEquals("Task width must be set according to minimal width"
                                            + " taskId=" + taskId + ", stackId=" + stackId,
                                    targetWidth, (int) wTaskBounds.getWidth());
                            int targetHeight = (int) Math.max(aTaskMinHeight,
                                    aStackBounds.getHeight());
                            assertEquals("Task height must be set according to minimal height"
                                            + " taskId=" + taskId + ", stackId=" + stackId,
                                    targetHeight, (int) wTaskBounds.getHeight());
                        }
                    }
                }
            }
        }
    }

    static int dpToPx(float dp, int densityDpi){
        return (int) (dp * densityDpi / DISPLAY_DENSITY_DEFAULT + 0.5f);
    }

    int defaultMinimalTaskSize(int displayId) {
        return dpToPx(DEFAULT_RESIZABLE_TASK_SIZE_DP, mWmState.getDisplay(displayId).getDpi());
    }
}
