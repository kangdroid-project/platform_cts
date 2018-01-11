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

import static android.app.WindowConfiguration.ACTIVITY_TYPE_ASSISTANT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class AssistantActivity extends Activity {

    // Launches the given activity in onResume
    public static final String EXTRA_LAUNCH_NEW_TASK = "launch_new_task";
    // Finishes this activity in onResume, this happens after EXTRA_LAUNCH_NEW_TASK
    public static final String EXTRA_FINISH_SELF = "finish_self";
    // Attempts to enter picture-in-picture in onResume
    public static final String EXTRA_ENTER_PIP = "enter_pip";
    // Display on which Assistant runs
    public static final String EXTRA_ASSISTANT_DISPLAY_ID = "assistant_display_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.assistant);

        // Launch the new activity if requested
        if (getIntent().hasExtra(EXTRA_LAUNCH_NEW_TASK)) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(this, getPackageName() + "."
                    + getIntent().getStringExtra(EXTRA_LAUNCH_NEW_TASK)));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (getIntent().hasExtra(EXTRA_ASSISTANT_DISPLAY_ID)) {
                ActivityOptions displayOptions = ActivityOptions.makeBasic();
                displayOptions.setLaunchDisplayId(Integer.parseInt(getIntent()
                        .getStringExtra(EXTRA_ASSISTANT_DISPLAY_ID)));
                startActivity(i, displayOptions.toBundle());
            } else {
                startActivity(i);
            }
        }

        // Enter pip if requested
        if (getIntent().hasExtra(EXTRA_ENTER_PIP)) {
            try {
                enterPictureInPictureMode();
            } catch (IllegalStateException e) {
                finish();
                return;
            }
        }

        // Finish this activity if requested
        if (getIntent().hasExtra(EXTRA_FINISH_SELF)) {
            finish();
        }
    }

    /**
     * Launches a new instance of the AssistantActivity directly into the assistant stack.
     */
    static void launchActivityIntoAssistantStack(Activity caller, Bundle extras) {
        final Intent intent = new Intent(caller, AssistantActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
        if (extras != null) {
            intent.putExtras(extras);
        }

        final ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchActivityType(ACTIVITY_TYPE_ASSISTANT);
        caller.startActivity(intent, options.toBundle());
    }
}
