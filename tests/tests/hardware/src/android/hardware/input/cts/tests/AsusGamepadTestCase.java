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

import android.hardware.cts.R;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AsusGamepadTestCase extends InputTestCase {
    public AsusGamepadTestCase() {
        super(R.raw.asus_gamepad_register);
    }

    @Test
    public void testAllKeys() {
        testInputEvents(R.raw.asus_gamepad_keyeventtests);
    }

    @Test
    public void testAllMotions() {
        testInputEvents(R.raw.asus_gamepad_motioneventtests);
    }
}
