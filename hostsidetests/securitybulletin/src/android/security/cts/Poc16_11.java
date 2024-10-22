/**
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
package android.security.cts;

import android.platform.test.annotations.SecurityTest;

@SecurityTest
public class Poc16_11 extends SecurityTestCase {

    /**
     *  b/29149404
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2012_6702() throws Exception {
        AdbUtils.runCommandLine("logcat -c", getDevice());
        AdbUtils.runPoc("CVE-2012-6702", getDevice(), 60);
        String logcat = AdbUtils.runCommandLine("logcat -d", getDevice());
        assertNotMatches("[\\s\\n\\S]*fail: encountered same random values![\\s\\n\\S]*", logcat);
    }

    /**
     *  b/30904789
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6730() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6730", getDevice(), 60);
        }
    }

    /**
     *  b/30906023
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6731() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6731", getDevice(), 60);
        }
    }

    /**
     *  b/30906599
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6732() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6732", getDevice(), 60);
        }
    }

    /**
     *  b/30906694
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6733() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6733", getDevice(), 60);
        }
    }

    /**
     *  b/30907120
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6734() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6734", getDevice(), 60);
        }
    }

    /**
     *  b/30907701
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6735() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6735", getDevice(), 60);
        }
    }

    /**
     *  b/30953284
     */
    @SecurityTest(minPatchLevel = "2016-11")
    public void testPocCVE_2016_6736() throws Exception {
        if(containsDriver(getDevice(), "/dev/dri/renderD129")) {
            AdbUtils.runPoc("CVE-2016-6736", getDevice(), 60);
        }
    }
}
