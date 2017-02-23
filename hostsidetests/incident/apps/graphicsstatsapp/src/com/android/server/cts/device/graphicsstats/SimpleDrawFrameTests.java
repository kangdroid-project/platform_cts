/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.server.cts.device.graphicsstats;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Used by GraphicsStatsTest.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SimpleDrawFrameTests {
    private static final String TAG = "GraphicsStatsDeviceTest";

    @Rule
    public ActivityTestRule<DrawFramesActivity> mActivityRule =
            new ActivityTestRule<>(DrawFramesActivity.class);

    @Test
    public void testDrawTenFrames() throws Throwable {
        DrawFramesActivity activity = mActivityRule.getActivity();
        activity.drawFrames(10);
    }
}
