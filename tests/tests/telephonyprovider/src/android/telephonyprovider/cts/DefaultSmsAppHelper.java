/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.telephonyprovider.cts;

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;

import android.support.test.InstrumentationRegistry;

class DefaultSmsAppHelper {
    static void setDefaultSmsApp(boolean setToSmsApp) {
        String packageName =
                InstrumentationRegistry.getInstrumentation().getContext().getPackageName();

        setDefaultSmsAppSetting(setToSmsApp, packageName);

        // FIXME: Required because setting default SMS app to a given package adds appops WRITE_SMS
        // permissions to the given package, but changing away from a given package seem to remove
        // the appops permission from the given package. This is a known issue and should be fixed
        // for Q.
        setSmsWritePermission(setToSmsApp, packageName);
    }

    private static void setDefaultSmsAppSetting(boolean setToSmsApp, String packageName) {
        runShellCommand(String.format("settings put secure sms_default_application %s",
                setToSmsApp ? packageName : "default"));
    }

    private static void setSmsWritePermission(boolean setToSmsApp, String packageName) {
        runShellCommand(String.format("appops set %s WRITE_SMS %s", packageName,
                setToSmsApp ? "allow" : "default"));
    }
}