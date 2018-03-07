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

package android.display.cts;

import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManager;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.test.InstrumentationTestCase;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BrightnessTest extends InstrumentationTestCase {

    private Map<Long, BrightnessChangeEvent> mLastReadEvents = new HashMap<>();
    private DisplayManager mDisplayManager;
    PowerManager.WakeLock mWakeLock;

    @Override
    public void setUp() {
        mDisplayManager =
                InstrumentationRegistry.getContext().getSystemService(DisplayManager.class);
        PowerManager pm =
                InstrumentationRegistry.getContext().getSystemService(PowerManager.class);
        // Fail early if screen isn't on as wakelock won't wake it up.
        assertTrue(pm.isInteractive());

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "BrightnessTest");
        mWakeLock.acquire();

        runShellCommand("pm revoke " + InstrumentationRegistry.getContext().getPackageName()
                + " android.permission.CONFIGURE_DISPLAY_BRIGHTNESS");
        runShellCommand("pm revoke " + InstrumentationRegistry.getContext().getPackageName()
                + " android.permission.BRIGHTNESS_SLIDER_USAGE");
    }

    @Override
    public void tearDown() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    public void testBrightnessSliderTracking() throws IOException, InterruptedException {
        if (!systemAppWithPermission("android.permission.BRIGHTNESS_SLIDER_USAGE",
                InstrumentationRegistry.getContext())) {
            // Don't run as there is no app that has permission to access slider usage.
            return;
        }

        int previousBrightness = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS);
        int previousBrightnessMode =
                getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
        try {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            int mode = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
            assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, mode);

            runShellCommand("pm grant " + InstrumentationRegistry.getContext().getPackageName()
                    + " android.permission.BRIGHTNESS_SLIDER_USAGE");

            // Setup and remember some initial state.
            recordSliderEvents();
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 20);
            getNewEvents(1);

            // Update brightness
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 60);

            // Check we got a slider event for the change.
            List<BrightnessChangeEvent> newEvents = getNewEvents(1);
            assertEquals(1, newEvents.size());
            BrightnessChangeEvent firstEvent = newEvents.get(0);
            assertValidLuxData(firstEvent);

            // Update brightness again
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 200);

            // Check we get a second slider event.
            newEvents = getNewEvents(1);
            assertEquals(1, newEvents.size());
            BrightnessChangeEvent secondEvent = newEvents.get(0);
            assertValidLuxData(secondEvent);
            assertEquals(secondEvent.lastBrightness, firstEvent.brightness, 1.0f);
            assertTrue(secondEvent.isUserSetBrightness);
            assertTrue("failed " + secondEvent.brightness + " not greater than " +
                    firstEvent.brightness, secondEvent.brightness > firstEvent.brightness);
        } finally {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, previousBrightness);
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE, previousBrightnessMode);
        }
    }

    public void testNoTrackingForManualBrightness() throws IOException, InterruptedException {
        if (!systemAppWithPermission("android.permission.BRIGHTNESS_SLIDER_USAGE",
                InstrumentationRegistry.getContext())) {
            // Don't run as there is no app that has permission to access slider usage.
            return;
        }
        int previousBrightness = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS);
        int previousBrightnessMode =
                getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
        try {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            int mode = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
            assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, mode);

            runShellCommand("pm grant " + InstrumentationRegistry.getContext().getPackageName()
                    + " android.permission.BRIGHTNESS_SLIDER_USAGE");

            // Setup and remember some initial state.
            recordSliderEvents();
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 20);
            assertTrue(getNewEvents().isEmpty());

            // Then change the brightness
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 80);
            Thread.sleep(200);
            // There shouldn't be any events.
            assertTrue(getNewEvents().isEmpty());
        } finally {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, previousBrightness);
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE, previousBrightnessMode);
        }
    }

    public void testSliderUsagePermission() throws IOException, InterruptedException {
        runShellCommand("pm revoke " + InstrumentationRegistry.getContext().getPackageName()
                + " android.permission.BRIGHTNESS_SLIDER_USAGE");

        try {
            mDisplayManager.getBrightnessEvents();
        } catch (SecurityException e) {
            // Expected
            return;
        }
        fail();
    }

    public void testConfigureBrightnessPermission() throws IOException, InterruptedException {
        runShellCommand("pm revoke " + InstrumentationRegistry.getContext().getPackageName()
                + " android.permission.CONFIGURE_DISPLAY_BRIGHTNESS");

        BrightnessConfiguration config =
            new BrightnessConfiguration.Builder(
                    new float[]{0.0f, 1000.0f},new float[]{20.0f, 500.0f})
                .setDescription("some test").build();

        try {
            mDisplayManager.setBrightnessConfiguration(config);
        } catch (SecurityException e) {
            // Expected
            return;
        }
        fail();
    }

    public void testPushSimpleCurves() throws IOException, InterruptedException {
        if (!systemAppWithPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS",
                InstrumentationRegistry.getContext())) {
            // Don't run as there is no app that has permission to push curves.
            return;
        }
        runShellCommand("pm grant " + InstrumentationRegistry.getContext().getPackageName()
                + " android.permission.CONFIGURE_DISPLAY_BRIGHTNESS");

        BrightnessConfiguration config =
                new BrightnessConfiguration.Builder(
                        new float[]{0.0f, 1000.0f},new float[]{20.0f, 500.0f})
                        .setDescription("some test").build();
        mDisplayManager.setBrightnessConfiguration(config);
        mDisplayManager.setBrightnessConfiguration(null);
    }

    public void testSliderEventsReflectCurves() throws IOException, InterruptedException {
        if (!systemAppWithPermission("android.permission.BRIGHTNESS_SLIDER_USAGE",
                InstrumentationRegistry.getContext())) {
            // Don't run as there is no app that has permission to access slider usage.
            return;
        }
        if (!systemAppWithPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS",
                InstrumentationRegistry.getContext())) {
            // Don't run as there is no app that has permission to push curves.
            return;
        }

        BrightnessConfiguration config =
                new BrightnessConfiguration.Builder(
                        new float[]{0.0f, 10000.0f},new float[]{15.0f, 400.0f})
                        .setDescription("model:8").build();

        int previousBrightness = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS);
        int previousBrightnessMode =
                getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
        try {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            int mode = getSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);
            assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, mode);

            runShellCommand("pm grant " + InstrumentationRegistry.getContext().getPackageName()
                    + " android.permission.BRIGHTNESS_SLIDER_USAGE");
            runShellCommand("pm grant " + InstrumentationRegistry.getContext().getPackageName()
                    + " android.permission.CONFIGURE_DISPLAY_BRIGHTNESS");

            // Setup and remember some initial state.
            recordSliderEvents();
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 20);
            getNewEvents(1);

            // Update brightness while we have a custom curve.
            mDisplayManager.setBrightnessConfiguration(config);
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 60);

            // Check we got a slider event for the change.
            List<BrightnessChangeEvent> newEvents = getNewEvents(1);
            assertEquals(1, newEvents.size());
            BrightnessChangeEvent firstEvent = newEvents.get(0);
            assertValidLuxData(firstEvent);
            assertFalse(firstEvent.isDefaultBrightnessConfig);

            // Update brightness again now with default curve.
            mDisplayManager.setBrightnessConfiguration(null);
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, 200);

            // Check we get a second slider event.
            newEvents = getNewEvents(1);
            assertEquals(1, newEvents.size());
            BrightnessChangeEvent secondEvent = newEvents.get(0);
            assertValidLuxData(secondEvent);
            assertTrue(secondEvent.isDefaultBrightnessConfig);
        } finally {
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS, previousBrightness);
            setSystemSetting(Settings.System.SCREEN_BRIGHTNESS_MODE, previousBrightnessMode);
        }
    }

    private void assertValidLuxData(BrightnessChangeEvent event) {
        assertNotNull(event.luxTimestamps);
        assertNotNull(event.luxValues);
        assertTrue(event.luxTimestamps.length > 0);
        assertEquals(event.luxValues.length, event.luxTimestamps.length);
        for (int i = 1; i < event.luxTimestamps.length; ++i) {
            assertTrue(event.luxTimestamps[i - 1] <= event.luxTimestamps[i]);
        }
        for (int i = 0; i < event.luxValues.length; ++i) {
            assertTrue(event.luxValues[i] >= 0.0f);
            assertTrue(event.luxValues[i] <= Float.MAX_VALUE);
            assertFalse(Float.isNaN(event.luxValues[i]));
        }
    }

    /**
     * Check if there exists a system app that has the permission.
     */
    private boolean systemAppWithPermission(String permission, Context context) {
        List<PackageInfo> packages = context.getPackageManager().getPackagesHoldingPermissions(
                new String[] {permission}, PackageManager.MATCH_SYSTEM_ONLY);
        return !packages.isEmpty();
    }

    private List<BrightnessChangeEvent> getNewEvents(int expected)
            throws InterruptedException {
        List<BrightnessChangeEvent> newEvents = new ArrayList<>();
        for (int i = 0; newEvents.size() < expected && i < 20; ++i) {
            if (i != 0) {
                Thread.sleep(100);
            }
            newEvents.addAll(getNewEvents());
        }
        return newEvents;
    }

    private List<BrightnessChangeEvent> getNewEvents() {
        List<BrightnessChangeEvent> newEvents = new ArrayList<>();
        List<BrightnessChangeEvent> events = mDisplayManager.getBrightnessEvents();
        for (BrightnessChangeEvent event : events) {
            if (!mLastReadEvents.containsKey(event.timeStamp)) {
                newEvents.add(event);
            }
        }
        mLastReadEvents = new HashMap<>();
        for (BrightnessChangeEvent event : events) {
            mLastReadEvents.put(event.timeStamp, event);
        }
        return newEvents;
    }

    private void recordSliderEvents() {
        mLastReadEvents = new HashMap<>();
        List<BrightnessChangeEvent> eventsBefore = mDisplayManager.getBrightnessEvents();
        for (BrightnessChangeEvent event: eventsBefore) {
            mLastReadEvents.put(event.timeStamp, event);
        }
    }

    private int getSystemSetting(String setting) throws IOException {
        return Integer.parseInt(runShellCommand("settings get system " + setting));
    }

    private void setSystemSetting(String setting, int value)
            throws IOException {
        runShellCommand("settings put system " + setting + " " + Integer.toString(value));
    }

    private String runShellCommand(String cmd) {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor output = automation.executeShellCommand(cmd);
        String result = convertFileDescriptorToString(output.getFileDescriptor());
        return result.trim();
    }

    private String convertFileDescriptorToString(FileDescriptor desc) {
        try (Scanner s = new Scanner(new FileInputStream(desc)).useDelimiter("\\Z")) {
            return s.hasNext() ? s.next() : "";
        }
    }
}
