/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.view.cts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ScaleGestureDetectorTest {

    private ScaleGestureDetector mScaleGestureDetector;
    private ScaleGestureDetectorCtsActivity mActivity;

    @Rule
    public ActivityTestRule<ScaleGestureDetectorCtsActivity> mActivityRule =
            new ActivityTestRule<>(ScaleGestureDetectorCtsActivity.class);

    @Before
    public void setup() {
        mActivity = mActivityRule.getActivity();
        mScaleGestureDetector = mActivity.getScaleGestureDetector();
    }

    @UiThreadTest
    @Test
    public void testConstructor() {
        new ScaleGestureDetector(
                mActivity, new SimpleOnScaleGestureListener(), new Handler(Looper.getMainLooper()));
        new ScaleGestureDetector(mActivity, new SimpleOnScaleGestureListener());
    }

    @Test
    public void testAccessStylusScaleEnabled() {
        assertTrue(mScaleGestureDetector.isStylusScaleEnabled());
        mScaleGestureDetector.setStylusScaleEnabled(true);

        mScaleGestureDetector.setStylusScaleEnabled(false);
        assertFalse(mScaleGestureDetector.isStylusScaleEnabled());
    }
}