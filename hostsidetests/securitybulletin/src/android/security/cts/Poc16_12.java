/**
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

package android.security.cts;

import android.platform.test.annotations.SecurityTest;

@SecurityTest
public class Poc16_12 extends SecurityTestCase {

    //Criticals
    /**
     *  b/31796940
     */
    @SecurityTest(minPatchLevel = "2016-12")
    public void testPocCVE_2016_8406() throws Exception {
        String cmd ="ls -l /sys/kernel/slab 2>/dev/null | grep nf_conn";
        String result =  AdbUtils.runCommandLine(cmd ,getDevice());
        assertNotMatchesMultiLine("nf_conntrack_(?!0{8})[A-Fa-f0-9]{8}", result);
    }
}
