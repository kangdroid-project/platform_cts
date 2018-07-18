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

package com.android.server.cts.device.statsd;

import android.net.wifi.WifiManager;
import android.support.test.InstrumentationRegistry;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Methods to check device properties. They pass iff the check returns true.
 */
public class Checkers {
    private static final String TAG = Checkers.class.getSimpleName();

    @Test
    public void checkWifiEnhancedPowerReportingSupported() {
        WifiManager wm = InstrumentationRegistry.getContext().getSystemService(WifiManager.class);
        assertTrue(wm.isEnhancedPowerReportingSupported());
    }
}
