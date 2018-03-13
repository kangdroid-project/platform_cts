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

package android.net.cts;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Tests for multinetwork sysctl functionality.
 */
public class IpSecSysctlTest extends SysctlBaseTest {

    // SPI expiration sysctls. Must be present and set greater than 1h.
    private static final String SPI_TIMEOUT_SYSCTL = "/proc/sys/net/core/xfrm_acq_expires";
    private static final int MIN_ACQ_EXPIRES = 3600;

    /**
     * Checks that SPI default timeouts are overridden, and set to a reasonable length of time
     */
    public void testProcFiles() throws ErrnoException, IOException, NumberFormatException {
        int value = getIntValue(SPI_TIMEOUT_SYSCTL);
        assertAtLeast(SPI_TIMEOUT_SYSCTL, value, MIN_ACQ_EXPIRES);
    }
}
