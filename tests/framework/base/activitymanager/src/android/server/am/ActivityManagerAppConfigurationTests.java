/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package android.server.am;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD;
import static android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN;
import static android.app.WindowConfiguration.WINDOWING_MODE_SPLIT_SCREEN_PRIMARY;
import static android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY;
import static android.server.am.ActivityAndWindowManagersState.dpToPx;
import static android.server.am.ActivityManagerState.STATE_RESUMED;
import static android.server.am.StateLogger.log;
import static android.server.am.StateLogger.logE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.platform.test.annotations.Presubmit;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Build: mmma -j32 cts/tests/framework/base
 * Run: cts/tests/framework/base/activitymanager/util/run-test CtsActivityManagerDeviceTestCases android.server.am.ActivityManagerAppConfigurationTests
 */
public class ActivityManagerAppConfigurationTests extends ActivityManagerTestBase {
    private static final String RESIZEABLE_ACTIVITY_NAME = "ResizeableActivity";
    private static final String TEST_ACTIVITY_NAME = "TestActivity";
    private static final String PORTRAIT_ACTIVITY_NAME = "PortraitOrientationActivity";
    private static final String LANDSCAPE_ACTIVITY_NAME = "LandscapeOrientationActivity";
    private static final String NIGHT_MODE_ACTIVITY = "NightModeActivity";
    private static final String DIALOG_WHEN_LARGE_ACTIVITY = "DialogWhenLargeActivity";

    private static final String TRANSLUCENT_ACTIVITY =
            "android.server.translucentapp.TranslucentLandscapeActivity";
    private static final String TRANSLUCENT_SDK_26_PACKAGE = "android.server.translucentapp26";

    private static final String TRANSLUCENT_CURRENT_PACKAGE = "android.server.translucentapp";

    private static final String EXTRA_LAUNCH_NEW_TASK = "launch_new_task";

    private static final int SMALL_WIDTH_DP = 426;
    private static final int SMALL_HEIGHT_DP = 320;

    /**
     * Tests that the WindowManager#getDefaultDisplay() and the Configuration of the Activity
     * has an updated size when the Activity is resized from fullscreen to docked state.
     *
     * The Activity handles configuration changes, so it will not be restarted between resizes.
     * On Configuration changes, the Activity logs the Display size and Configuration width
     * and heights. The values reported in fullscreen should be larger than those reported in
     * docked state.
     */
    @Test
    public void testConfigurationUpdatesWhenResizedFromFullscreen() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        String logSeparator = clearLogcat();
        launchActivity(RESIZEABLE_ACTIVITY_NAME, WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY);
        final ReportedSizes fullscreenSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        logSeparator = clearLogcat();
        setActivityTaskWindowingMode(RESIZEABLE_ACTIVITY_NAME, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ReportedSizes dockedSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        assertSizesAreSane(fullscreenSizes, dockedSizes);
    }

    /**
     * Same as {@link #testConfigurationUpdatesWhenResizedFromFullscreen()} but resizing
     * from docked state to fullscreen (reverse).
     */
    // TODO: Flaky, add to presubmit when b/63404575 is fixed.
    @Test
    public void testConfigurationUpdatesWhenResizedFromDockedStack() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        String logSeparator = clearLogcat();
        launchActivity(RESIZEABLE_ACTIVITY_NAME, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ReportedSizes dockedSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        logSeparator = clearLogcat();
        setActivityTaskWindowingMode(RESIZEABLE_ACTIVITY_NAME, WINDOWING_MODE_FULLSCREEN);
        final ReportedSizes fullscreenSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        assertSizesAreSane(fullscreenSizes, dockedSizes);
    }

    /**
     * Tests whether the Display sizes change when rotating the device.
     */
    @Test
    public void testConfigurationUpdatesWhenRotatingWhileFullscreen() throws Exception {
        if (!supportsRotation()) {
            log("Skipping test: no rotation support");
            return;
        }
        setDeviceRotation(0);
        final String logSeparator = clearLogcat();
        launchActivity(RESIZEABLE_ACTIVITY_NAME,
                WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY);
        final ReportedSizes initialSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        rotateAndCheckSizes(initialSizes);
    }

    /**
     * Same as {@link #testConfigurationUpdatesWhenRotatingWhileFullscreen()} but when the Activity
     * is in the docked stack.
     */
    // TODO: Flaky, add to presubmit when b/63404575 is fixed.
    @Test
    public void testConfigurationUpdatesWhenRotatingWhileDocked() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        setDeviceRotation(0);
        final String logSeparator = clearLogcat();
        launchActivityInDockStack(LAUNCHING_ACTIVITY);
        // Launch our own activity to side in case Recents (or other activity to side) doesn't
        // support rotation.
        getLaunchActivityBuilder().setToSide(true).setTargetActivityName(TEST_ACTIVITY_NAME)
                .execute();
        // Launch target activity in docked stack.
        getLaunchActivityBuilder().setTargetActivityName(RESIZEABLE_ACTIVITY_NAME).execute();
        final ReportedSizes initialSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        rotateAndCheckSizes(initialSizes);
    }

    /**
     * Same as {@link #testConfigurationUpdatesWhenRotatingWhileDocked()} but when the Activity
     * is launched to side from docked stack.
     */
    @Test
    public void testConfigurationUpdatesWhenRotatingToSideFromDocked() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        setDeviceRotation(0);

        final String logSeparator = clearLogcat();
        launchActivityInDockStack(LAUNCHING_ACTIVITY);

        getLaunchActivityBuilder().setToSide(true).setTargetActivityName(RESIZEABLE_ACTIVITY_NAME)
                .execute();
        final ReportedSizes initialSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                logSeparator);

        rotateAndCheckSizes(initialSizes);
    }

    private void rotateAndCheckSizes(ReportedSizes prevSizes) throws Exception {
        for (int rotation = 3; rotation >= 0; --rotation) {
            final String logSeparator = clearLogcat();
            final int actualStackId = mAmWmState.getAmState().getTaskByActivityName(
                    RESIZEABLE_ACTIVITY_NAME).mStackId;
            final int displayId = mAmWmState.getAmState().getStackById(actualStackId).mDisplayId;
            setDeviceRotation(rotation);
            final int newDeviceRotation = getDeviceRotation(displayId);
            if (newDeviceRotation == INVALID_DEVICE_ROTATION) {
                logE("Got an invalid device rotation value. "
                        + "Continuing the test despite of that, but it is likely to fail.");
            } else if (rotation != newDeviceRotation) {
                log("This device doesn't support locked user "
                        + "rotation mode. Not continuing the rotation checks.");
                return;
            }

            final ReportedSizes rotatedSizes = getActivityDisplaySize(RESIZEABLE_ACTIVITY_NAME,
                    logSeparator);
            assertSizesRotate(prevSizes, rotatedSizes);
            prevSizes = rotatedSizes;
        }
    }

    /**
     * Tests when activity moved from fullscreen stack to docked and back. Activity will be
     * relaunched twice and it should have same config as initial one.
     */
    @Test
    public void testSameConfigurationFullSplitFullRelaunch() throws Exception {
        moveActivityFullSplitFull(TEST_ACTIVITY_NAME);
    }

    /**
     * Same as {@link #testSameConfigurationFullSplitFullRelaunch} but without relaunch.
     */
    @Presubmit
    @Test
    public void testSameConfigurationFullSplitFullNoRelaunch() throws Exception {
        moveActivityFullSplitFull(RESIZEABLE_ACTIVITY_NAME);
    }

    /**
     * Launches activity in fullscreen stack, moves to docked stack and back to fullscreen stack.
     * Last operation is done in a way which simulates split-screen divider movement maximizing
     * docked stack size and then moving task to fullscreen stack - the same way it is done when
     * user long-presses overview/recents button to exit split-screen.
     * Asserts that initial and final reported sizes in fullscreen stack are the same.
     */
    private void moveActivityFullSplitFull(String activityName) throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        // Launch to fullscreen stack and record size.
        String logSeparator = clearLogcat();
        launchActivity(activityName, WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY);
        final ReportedSizes initialFullscreenSizes = getActivityDisplaySize(activityName,
                logSeparator);
        final Rect displayRect = getDisplayRect(activityName);

        // Move to docked stack.
        logSeparator = clearLogcat();
        setActivityTaskWindowingMode(activityName, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ReportedSizes dockedSizes = getActivityDisplaySize(activityName, logSeparator);
        assertSizesAreSane(initialFullscreenSizes, dockedSizes);
        // Make sure docked stack is focused. This way when we dismiss it later fullscreen stack
        // will come up.
        launchActivity(activityName, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        mAmWmState.computeState(false /* compareTaskAndStackBounds */,
                new WaitForValidActivityState.Builder(activityName).build());
        final ActivityManagerState.ActivityStack stack = mAmWmState.getAmState()
                .getStandardStackByWindowingMode(WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);

        // Resize docked stack to fullscreen size. This will trigger activity relaunch with
        // non-empty override configuration corresponding to fullscreen size.
        logSeparator = clearLogcat();
        mAm.resizeStack(stack.mStackId, displayRect);

        // Move activity back to fullscreen stack.
        setActivityTaskWindowingMode(activityName,
                WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY);
        final ReportedSizes finalFullscreenSizes = getActivityDisplaySize(activityName,
                logSeparator);

        // After activity configuration was changed twice it must report same size as original one.
        assertSizesAreSame(initialFullscreenSizes, finalFullscreenSizes);
    }

    /**
     * Tests when activity moved from docked stack to fullscreen and back. Activity will be
     * relaunched twice and it should have same config as initial one.
     */
    @Test
    public void testSameConfigurationSplitFullSplitRelaunch() throws Exception {
        moveActivitySplitFullSplit(TEST_ACTIVITY_NAME);
    }

    /**
     * Same as {@link #testSameConfigurationSplitFullSplitRelaunch} but without relaunch.
     */
    @Test
    public void testSameConfigurationSplitFullSplitNoRelaunch() throws Exception {
        moveActivitySplitFullSplit(RESIZEABLE_ACTIVITY_NAME);
    }

    /**
     * Tests that an activity with the DialogWhenLarge theme can transform properly when in split
     * screen.
     */
    @Presubmit
    @Test
    public void testDialogWhenLargeSplitSmall() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        launchActivity(DIALOG_WHEN_LARGE_ACTIVITY, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ActivityManagerState.ActivityStack stack = mAmWmState.getAmState()
                .getStandardStackByWindowingMode(WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final WindowManagerState.Display display =
                mAmWmState.getWmState().getDisplay(stack.mDisplayId);
        final int density = display.getDpi();
        final int smallWidthPx = dpToPx(SMALL_WIDTH_DP, density);
        final int smallHeightPx = dpToPx(SMALL_HEIGHT_DP, density);

        mAm.resizeStack(stack.mStackId, new Rect(0, 0, smallWidthPx, smallHeightPx));
        mAmWmState.waitForValidState(DIALOG_WHEN_LARGE_ACTIVITY,
                WINDOWING_MODE_SPLIT_SCREEN_PRIMARY, ACTIVITY_TYPE_STANDARD);
    }

    /**
     * Test that device handles consequent requested orientations and displays the activities.
     */
    @Presubmit
    @Test
    public void testFullscreenAppOrientationRequests() throws Exception {
        launchActivity(PORTRAIT_ACTIVITY_NAME);
        mAmWmState.assertVisibility(PORTRAIT_ACTIVITY_NAME, true /* visible */);
        assertEquals("Fullscreen app requested portrait orientation",
                1 /* portrait */, mAmWmState.getWmState().getLastOrientation());

        launchActivity(LANDSCAPE_ACTIVITY_NAME);
        mAmWmState.assertVisibility(LANDSCAPE_ACTIVITY_NAME, true /* visible */);
        assertEquals("Fullscreen app requested landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());

        launchActivity(PORTRAIT_ACTIVITY_NAME);
        mAmWmState.assertVisibility(PORTRAIT_ACTIVITY_NAME, true /* visible */);
        assertEquals("Fullscreen app requested portrait orientation",
                1 /* portrait */, mAmWmState.getWmState().getLastOrientation());
    }

    public void testNonfullscreenAppOrientationRequests() throws Exception {
        String logSeparator = clearLogcat();
        launchActivity(PORTRAIT_ACTIVITY_NAME);
        final ReportedSizes initialReportedSizes =
                getLastReportedSizesForActivity(PORTRAIT_ACTIVITY_NAME, logSeparator);
        assertEquals("portrait activity should be in portrait",
                1 /* portrait */, initialReportedSizes.orientation);
        logSeparator = clearLogcat();

        launchActivityInComponent(TRANSLUCENT_SDK_26_PACKAGE, TRANSLUCENT_ACTIVITY);
        assertEquals("Legacy non-fullscreen activity requested landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());

        // TODO(b/36897968): uncomment once we can suppress unsupported configurations
        // final ReportedSizes updatedReportedSizes =
        //      getLastReportedSizesForActivity(PORTRAIT_ACTIVITY_NAME, logSeparator);
        // assertEquals("portrait activity should not have moved from portrait",
        //         1 /* portrait */, updatedReportedSizes.orientation);
    }

    public void testNonFullscreenActivityProhibited() throws Exception {
        setComponentName(TRANSLUCENT_CURRENT_PACKAGE);

        // We do not wait for the activity as it should not launch based on the restrictions around
        // specifying orientation. We instead start an activity known to launch immediately after
        // so that we can ensure processing the first activity occurred.
        launchActivityNoWait(TRANSLUCENT_ACTIVITY);
        setDefaultComponentName();
        launchActivity(PORTRAIT_ACTIVITY_NAME);

        assertFalse("target SDK > 26 non-fullscreen activity should not reach onResume",
                mAmWmState.getAmState().containsActivity(
                        ActivityManagerTestBase.getActivityComponentName(
                                TRANSLUCENT_ACTIVITY, TRANSLUCENT_ACTIVITY)));
    }
    public void testNonFullscreenActivityPermitted() throws Exception {
        setComponentName(TRANSLUCENT_SDK_26_PACKAGE);
        setDeviceRotation(0);

        launchActivity(TRANSLUCENT_ACTIVITY);
        mAmWmState.assertResumedActivity(
                "target SDK <= 26 non-fullscreen activity should be allowed to launch",
                TRANSLUCENT_ACTIVITY);
        assertEquals("non-fullscreen activity requested landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());
    }

    /**
     * Test that device handles moving between two tasks with different orientations.
     */
    @Test
    public void testTaskCloseRestoreOrientation() throws Exception {
        // Start landscape activity.
        launchActivity(LANDSCAPE_ACTIVITY_NAME);
        mAmWmState.assertVisibility(LANDSCAPE_ACTIVITY_NAME, true /* visible */);
        assertEquals("Fullscreen app requested landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());

        // Start another activity in a different task.
        launchActivityInNewTask(BROADCAST_RECEIVER_ACTIVITY);

        // Request portrait
        executeShellCommand(getOrientationBroadcast(1 /*portrait*/));
        mAmWmState.waitForRotation(1);

        // Finish activity
        executeShellCommand(FINISH_ACTIVITY_BROADCAST);

        // Verify that activity brought to front is in originally requested orientation.
        mAmWmState.computeState(
            new WaitForValidActivityState.Builder(LANDSCAPE_ACTIVITY_NAME).build());
        assertEquals("Should return to app in landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());
    }

    /**
     * Test that device handles moving between two tasks with different orientations.
     */
    @Presubmit
    @Test
    public void testTaskMoveToBackOrientation() throws Exception {
        // Start landscape activity.
        launchActivity(LANDSCAPE_ACTIVITY_NAME);
        mAmWmState.assertVisibility(LANDSCAPE_ACTIVITY_NAME, true /* visible */);
        assertEquals("Fullscreen app requested landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());

        // Start another activity in a different task.
        launchActivityInNewTask(BROADCAST_RECEIVER_ACTIVITY);

        // Request portrait
        executeShellCommand(getOrientationBroadcast(1 /*portrait*/));
        mAmWmState.waitForRotation(1);

        // Finish activity
        executeShellCommand(MOVE_TASK_TO_BACK_BROADCAST);

        // Verify that activity brought to front is in originally requested orientation.
        mAmWmState.waitForValidState(LANDSCAPE_ACTIVITY_NAME);
        assertEquals("Should return to app in landscape orientation",
                0 /* landscape */, mAmWmState.getWmState().getLastOrientation());
    }

    /**
     * Test that device doesn't change device orientation by app request while in multi-window.
     */
    @Test
    public void testSplitscreenPortraitAppOrientationRequests() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
          CLog.logAndDisplay(LogLevel.INFO, "Skipping test: no multi-window support");
          return;
        }
        requestOrientationInSplitScreen(1 /* portrait */, LANDSCAPE_ACTIVITY_NAME);
    }

    /**
     * Test that device doesn't change device orientation by app request while in multi-window.
     */
    @Test
    public void testSplitscreenLandscapeAppOrientationRequests() throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
          CLog.logAndDisplay(LogLevel.INFO, "Skipping test: no multi-window support");
          return;
        }
        requestOrientationInSplitScreen(0 /* landscape */, PORTRAIT_ACTIVITY_NAME);
    }

    /**
     * Rotate the device and launch specified activity in split-screen, checking if orientation
     * didn't change.
     */
    private void requestOrientationInSplitScreen(int orientation, String activity)
            throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        // Set initial orientation.
        setDeviceRotation(orientation);

        // Launch activities that request orientations and check that device doesn't rotate.
        launchActivityInDockStack(LAUNCHING_ACTIVITY);

        getLaunchActivityBuilder().setToSide(true).setMultipleTask(true)
                .setTargetActivityName(activity).execute();
        mAmWmState.computeState(new String[] {activity});
        mAmWmState.assertVisibility(activity, true /* visible */);
        assertEquals("Split-screen apps shouldn't influence device orientation",
                orientation, mAmWmState.getWmState().getRotation());

        getLaunchActivityBuilder().setMultipleTask(true).setTargetActivityName(activity).execute();
        mAmWmState.computeState(new String[] {activity});
        mAmWmState.assertVisibility(activity, true /* visible */);
        assertEquals("Split-screen apps shouldn't influence device orientation",
                orientation, mAmWmState.getWmState().getRotation());
    }

    /**
     * Launches activity in docked stack, moves to fullscreen stack and back to docked stack.
     * Asserts that initial and final reported sizes in docked stack are the same.
     */
    private void moveActivitySplitFullSplit(String activityName) throws Exception {
        if (!supportsSplitScreenMultiWindow()) {
            log("Skipping test: no multi-window support");
            return;
        }

        // Launch to docked stack and record size.
        String logSeparator = clearLogcat();
        launchActivity(activityName, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ReportedSizes initialDockedSizes = getActivityDisplaySize(activityName, logSeparator);
        // Make sure docked stack is focused. This way when we dismiss it later fullscreen stack
        // will come up.
        launchActivity(activityName, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        mAmWmState.computeState(false /* compareTaskAndStackBounds */,
                new WaitForValidActivityState.Builder(activityName).build());

        // Move to fullscreen stack.
        logSeparator = clearLogcat();
        setActivityTaskWindowingMode(
                activityName, WINDOWING_MODE_FULLSCREEN_OR_SPLIT_SCREEN_SECONDARY);
        final ReportedSizes fullscreenSizes = getActivityDisplaySize(activityName, logSeparator);
        assertSizesAreSane(fullscreenSizes, initialDockedSizes);

        // Move activity back to docked stack.
        logSeparator = clearLogcat();
        setActivityTaskWindowingMode(activityName, WINDOWING_MODE_SPLIT_SCREEN_PRIMARY);
        final ReportedSizes finalDockedSizes = getActivityDisplaySize(activityName, logSeparator);

        // After activity configuration was changed twice it must report same size as original one.
        assertSizesAreSame(initialDockedSizes, finalDockedSizes);
    }

    /**
     * Asserts that after rotation, the aspect ratios of display size, metrics, and configuration
     * have flipped.
     */
    private static void assertSizesRotate(ReportedSizes rotationA, ReportedSizes rotationB)
            throws Exception {
        assertEquals(rotationA.displayWidth, rotationA.metricsWidth);
        assertEquals(rotationA.displayHeight, rotationA.metricsHeight);
        assertEquals(rotationB.displayWidth, rotationB.metricsWidth);
        assertEquals(rotationB.displayHeight, rotationB.metricsHeight);

        final boolean beforePortrait = rotationA.displayWidth < rotationA.displayHeight;
        final boolean afterPortrait = rotationB.displayWidth < rotationB.displayHeight;
        assertFalse(beforePortrait == afterPortrait);

        final boolean beforeConfigPortrait = rotationA.widthDp < rotationA.heightDp;
        final boolean afterConfigPortrait = rotationB.widthDp < rotationB.heightDp;
        assertEquals(beforePortrait, beforeConfigPortrait);
        assertEquals(afterPortrait, afterConfigPortrait);

        assertEquals(rotationA.smallestWidthDp, rotationB.smallestWidthDp);
    }

    /**
     * Throws an AssertionError if fullscreenSizes has widths/heights (depending on aspect ratio)
     * that are smaller than the dockedSizes.
     */
    private static void assertSizesAreSane(ReportedSizes fullscreenSizes, ReportedSizes dockedSizes)
            throws Exception {
        final boolean portrait = fullscreenSizes.displayWidth < fullscreenSizes.displayHeight;
        if (portrait) {
            assertTrue(dockedSizes.displayHeight < fullscreenSizes.displayHeight);
            assertTrue(dockedSizes.heightDp < fullscreenSizes.heightDp);
            assertTrue(dockedSizes.metricsHeight < fullscreenSizes.metricsHeight);
        } else {
            assertTrue(dockedSizes.displayWidth < fullscreenSizes.displayWidth);
            assertTrue(dockedSizes.widthDp < fullscreenSizes.widthDp);
            assertTrue(dockedSizes.metricsWidth < fullscreenSizes.metricsWidth);
        }
    }

    /**
     * Throws an AssertionError if sizes are different.
     */
    private static void assertSizesAreSame(ReportedSizes firstSize, ReportedSizes secondSize)
            throws Exception {
        assertEquals(firstSize.widthDp, secondSize.widthDp);
        assertEquals(firstSize.heightDp, secondSize.heightDp);
        assertEquals(firstSize.displayWidth, secondSize.displayWidth);
        assertEquals(firstSize.displayHeight, secondSize.displayHeight);
        assertEquals(firstSize.metricsWidth, secondSize.metricsWidth);
        assertEquals(firstSize.metricsHeight, secondSize.metricsHeight);
        assertEquals(firstSize.smallestWidthDp, secondSize.smallestWidthDp);
    }

    private ReportedSizes getActivityDisplaySize(String activityName, String logSeparator)
            throws Exception {
        mAmWmState.computeState(false /* compareTaskAndStackBounds */,
                new WaitForValidActivityState.Builder(activityName).build());
        final ReportedSizes details = getLastReportedSizesForActivity(activityName, logSeparator);
        assertNotNull(details);
        return details;
    }

    private Rect getDisplayRect(String activityName)
            throws Exception {
        final String windowName = getWindowName(activityName);

        mAmWmState.computeState(new String[] {activityName});
        mAmWmState.assertFocusedWindow("Test window must be the front window.", windowName);

        final List<WindowManagerState.WindowState> tempWindowList = new ArrayList<>();
        mAmWmState.getWmState().getMatchingVisibleWindowState(windowName, tempWindowList);

        assertEquals("Should have exactly one window state for the activity.", 1,
                tempWindowList.size());

        WindowManagerState.WindowState windowState = tempWindowList.get(0);
        assertNotNull("Should have a valid window", windowState);

        WindowManagerState.Display display = mAmWmState.getWmState()
                .getDisplay(windowState.getDisplayId());
        assertNotNull("Should be on a display", display);

        return display.getDisplayRect();
    }

    /**
     * Test launching an activity which requests specific UI mode during creation.
     */
    @Test
    public void testLaunchWithUiModeChange() throws Exception {
        // Launch activity that changes UI mode and handles this configuration change.
        launchActivity(NIGHT_MODE_ACTIVITY);
        mAmWmState.waitForActivityState(NIGHT_MODE_ACTIVITY, STATE_RESUMED);

        // Check if activity is launched successfully.
        mAmWmState.assertVisibility(NIGHT_MODE_ACTIVITY, true /* visible */);
        mAmWmState.assertFocusedActivity("Launched activity should be focused",
                NIGHT_MODE_ACTIVITY);
        mAmWmState.assertResumedActivity("Launched activity must be resumed", NIGHT_MODE_ACTIVITY);
    }
}
