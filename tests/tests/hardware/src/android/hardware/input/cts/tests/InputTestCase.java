/*
 * Copyright 2015 The Android Open Source Project
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

package android.hardware.input.cts.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.hardware.input.cts.InputCallback;
import android.hardware.input.cts.InputCtsActivity;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.cts.input.HidDevice;
import com.android.cts.input.HidJsonParser;
import com.android.cts.input.HidTestData;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class InputTestCase {
    private static final float TOLERANCE = 0.005f;

    private final BlockingQueue<InputEvent> mEvents;

    private InputListener mInputListener;
    private Instrumentation mInstrumentation;
    private HidDevice mHidDevice;
    private HidJsonParser mParser;
    // Stores the name of the currently running test
    private String mCurrentTestCase;
    private int mRegisterResourceId; // raw resource that contains json for registering a hid device

    InputTestCase(int registerResourceId) {
        mEvents = new LinkedBlockingQueue<>();
        mInputListener = new InputListener();
        mRegisterResourceId = registerResourceId;
    }

    @Rule
    public ActivityTestRule<InputCtsActivity> mActivityRule =
        new ActivityTestRule<>(InputCtsActivity.class);

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivityRule.getActivity().setInputCallback(mInputListener);
        mParser = new HidJsonParser(mInstrumentation.getTargetContext());
        int hidDeviceId = mParser.readDeviceId(mRegisterResourceId);
        String registerCommand = mParser.readRegisterCommand(mRegisterResourceId);
        mHidDevice = new HidDevice(mInstrumentation, hidDeviceId, registerCommand);
        mEvents.clear();
    }

    @After
    public void tearDown() {
        mHidDevice.close();
    }

    /**
     * Asserts that the application received a {@link android.view.KeyEvent} with the given action
     * and keycode.
     *
     * If other KeyEvents are received by the application prior to the expected KeyEvent, or no
     * KeyEvents are received within a reasonable amount of time, then this will throw an
     * AssertionFailedError.
     *
     * Only action and keyCode are being compared.
     */
    private void assertReceivedKeyEvent(@NonNull KeyEvent expectedKeyEvent) {
        KeyEvent receivedKeyEvent = waitForKey();
        if (receivedKeyEvent == null) {
            fail(mCurrentTestCase + ": timed out waiting for "
                    + KeyEvent.keyCodeToString(expectedKeyEvent.getKeyCode())
                    + " with action " + KeyEvent.actionToString(expectedKeyEvent.getAction()));
        }
        assertEquals(mCurrentTestCase, expectedKeyEvent.getAction(), receivedKeyEvent.getAction());
        assertEquals(mCurrentTestCase,
                expectedKeyEvent.getKeyCode(), receivedKeyEvent.getKeyCode());
    }

    private void assertReceivedMotionEvent(@NonNull MotionEvent expectedEvent) {
        MotionEvent event = waitForMotion();
        /*
         If the test fails here, one thing to try is to forcefully add a delay after the device
         added callback has been received, but before any hid data has been written to the device.
         We already wait for all of the proper callbacks here and in other places of the stack, but
         it appears that the device sometimes is still not ready to receive hid data. If any data
         gets written to the device in that state, it will disappear,
         and no events will be generated.
          */

        if (event == null) {
            fail(mCurrentTestCase + ": timed out waiting for MotionEvent");
        }
        if (event.getHistorySize() > 0) {
            fail(mCurrentTestCase + ": expected each MotionEvent to only have a single entry");
        }
        assertEquals(mCurrentTestCase, expectedEvent.getAction(), event.getAction());
        for (int axis = MotionEvent.AXIS_X; axis <= MotionEvent.AXIS_GENERIC_16; axis++) {
            assertEquals(mCurrentTestCase + " (" + MotionEvent.axisToString(axis) + ")",
                    expectedEvent.getAxisValue(axis), event.getAxisValue(axis), TOLERANCE);
        }
    }

    /**
     * Assert that no more events have been received by the application.
     *
     * If any more events have been received by the application, this will cause failure.
     */
    private void assertNoMoreEvents() {
        mInstrumentation.waitForIdleSync();
        InputEvent event = mEvents.poll();
        if (event == null) {
            return;
        }
        fail(mCurrentTestCase + ": extraneous events generated: " + event);
    }

    protected void testInputEvents(int resourceId) {
        List<HidTestData> tests = mParser.getTestData(resourceId);

        for (HidTestData testData: tests) {
            mCurrentTestCase = testData.name;

            // Send all of the HID reports
            for (int i = 0; i < testData.reports.size(); i++) {
                final String report = testData.reports.get(i);
                mHidDevice.sendHidReport(report);
            }

            // Make sure we received the expected input events
            for (int i = 0; i < testData.events.size(); i++) {
                final InputEvent event = testData.events.get(i);
                if (event instanceof MotionEvent) {
                    assertReceivedMotionEvent((MotionEvent) event);
                } else if (event instanceof KeyEvent) {
                    assertReceivedKeyEvent((KeyEvent) event);
                } else {
                    fail("Entry " + i + " is neither a KeyEvent nor a MotionEvent: " + event);
                }
            }
        }
        assertNoMoreEvents();
    }

    private InputEvent waitForEvent() {
        try {
            return mEvents.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(mCurrentTestCase + ": unexpectedly interrupted while waiting for InputEvent");
            return null;
        }
    }

    private KeyEvent waitForKey() {
        InputEvent event = waitForEvent();
        if (event instanceof KeyEvent) {
            return (KeyEvent) event;
        }
        fail("Expected a KeyEvent, but received: " + event);
        return null;
    }

    private MotionEvent waitForMotion() {
        InputEvent event = waitForEvent();
        if (event instanceof MotionEvent) {
            return (MotionEvent) event;
        }
        fail("Expected a MotionEvent, but received: " + event);
        return null;
    }

    /**
     * Since MotionEvents are batched together based on overall system timings (i.e. vsync), we
     * can't rely on them always showing up batched in the same way. In order to make sure our
     * test results are consistent, we instead split up the batches so they end up in a
     * consistent and reproducible stream.
     *
     * Note, however, that this ignores the problem of resampling, as we still don't know how to
     * distinguish resampled events from real events. Only the latter will be consistent and
     * reproducible.
     *
     * @param event The (potentially) batched MotionEvent
     * @return List of MotionEvents, with each event guaranteed to have zero history size, and
     * should otherwise be equivalent to the original batch MotionEvent.
     */
    private static List<MotionEvent> splitBatchedMotionEvent(MotionEvent event) {
        List<MotionEvent> events = new ArrayList<>();
        final int historySize = event.getHistorySize();
        final int pointerCount = event.getPointerCount();
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] currentCoords = new MotionEvent.PointerCoords[pointerCount];
        for (int p = 0; p < pointerCount; p++) {
            properties[p] = new MotionEvent.PointerProperties();
            event.getPointerProperties(p, properties[p]);
            currentCoords[p] = new MotionEvent.PointerCoords();
            event.getPointerCoords(p, currentCoords[p]);
        }
        for (int h = 0; h < historySize; h++) {
            long eventTime = event.getHistoricalEventTime(h);
            MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerCount];

            for (int p = 0; p < pointerCount; p++) {
                coords[p] = new MotionEvent.PointerCoords();
                event.getHistoricalPointerCoords(p, h, coords[p]);
            }
            MotionEvent singleEvent =
                    MotionEvent.obtain(event.getDownTime(), eventTime, event.getAction(),
                            pointerCount, properties, coords,
                            event.getMetaState(), event.getButtonState(),
                            event.getXPrecision(), event.getYPrecision(),
                            event.getDeviceId(), event.getEdgeFlags(),
                            event.getSource(), event.getFlags());
            events.add(singleEvent);
        }

        MotionEvent singleEvent =
                MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(),
                        pointerCount, properties, currentCoords,
                        event.getMetaState(), event.getButtonState(),
                        event.getXPrecision(), event.getYPrecision(),
                        event.getDeviceId(), event.getEdgeFlags(),
                        event.getSource(), event.getFlags());
        events.add(singleEvent);
        return events;
    }

    private class InputListener implements InputCallback {
        @Override
        public void onKeyEvent(KeyEvent ev) {
            try {
                mEvents.put(new KeyEvent(ev));
            } catch (InterruptedException ex) {
                fail(mCurrentTestCase + ": interrupted while adding a KeyEvent to the queue");
            }
        }

        @Override
        public void onMotionEvent(MotionEvent ev) {
            try {
                for (MotionEvent event : splitBatchedMotionEvent(ev)) {
                    mEvents.put(event);
                }
            } catch (InterruptedException ex) {
                fail(mCurrentTestCase + ": interrupted while adding a MotionEvent to the queue");
            }
        }
    }
}
