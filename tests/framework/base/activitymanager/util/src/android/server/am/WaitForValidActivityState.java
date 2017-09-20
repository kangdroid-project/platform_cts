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
 * limitations under the License
 */

package android.server.am;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;
import static android.app.WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;

public class WaitForValidActivityState {
    public final String activityName;
    public final int stackId;
    public final int windowingMode;
    public final int activityType;

    public WaitForValidActivityState(String activityName) {
        this.activityName = activityName;
        this.stackId = INVALID_STACK_ID;
        this.windowingMode = WINDOWING_MODE_UNDEFINED;
        this.activityType = ACTIVITY_TYPE_UNDEFINED;
    }

    private WaitForValidActivityState(String activityName, int stackId, int windowingMode,
            int activityType) {
        this.activityName = activityName;
        this.stackId = stackId;
        this.windowingMode = windowingMode;
        this.activityType = activityType;
    }

    public static class Builder {
        private String mActivityName = null;
        private int mStackId = INVALID_STACK_ID;
        private int mWindowingMode = WINDOWING_MODE_UNDEFINED;
        private int mActivityType = ACTIVITY_TYPE_UNDEFINED;

        public Builder() {}

        public Builder(String activityName) {
            mActivityName = activityName;
        }

        public Builder setActivityName(String activityName) {
            mActivityName = activityName;
            return this;
        }

        public Builder setStackId(int stackId) {
            mStackId = stackId;
            return this;
        }

        public Builder setWindowingMode(int windowingMode) {
            mWindowingMode = windowingMode;
            return this;
        }

        public Builder setActivityType(int activityType) {
            mActivityType = activityType;
            return this;
        }

        public WaitForValidActivityState build() {
            return new WaitForValidActivityState(
                    mActivityName, mStackId, mWindowingMode, mActivityType);
        }
    }
}
