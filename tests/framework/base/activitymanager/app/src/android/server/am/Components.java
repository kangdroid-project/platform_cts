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

package android.server.am;

import android.content.ComponentName;
import android.server.am.component.ComponentsBase;

public class Components extends ComponentsBase {

    public static final ComponentName LAUNCHING_ACTIVITY = component("LaunchingActivity");
    public static final ComponentName SPLASHSCREEN_ACTIVITY = component("SplashscreenActivity");
    public static final ComponentName TEST_ACTIVITY = component("TestActivity");

    public static final ComponentName LAUNCH_BROADCAST_RECEIVER =
            component("LaunchBroadcastReceiver");
    public static final String LAUNCH_BROADCAST_ACTION =
            getPackageName() + ".LAUNCH_BROADCAST_ACTION";


    private static ComponentName component(String className) {
        return component(Components.class, className);
    }

    private static String getPackageName() {
        return getPackageName(Components.class);
    }
}
