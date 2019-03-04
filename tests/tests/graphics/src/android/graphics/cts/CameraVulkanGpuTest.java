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
package android.graphics.cts;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.test.suitebuilder.annotation.SmallTest;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CameraVulkanGpuTest {

    static {
        System.loadLibrary("ctsgraphics_jni");
    }

    @Test
    public void testCameraImportAndRendering() throws Exception {
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // Test requires a camera.
            return;
        }

        loadCameraAndVerifyFrameImport(InstrumentationRegistry.getContext().getAssets());
    }

    private static native void loadCameraAndVerifyFrameImport(AssetManager assetManager);
}
