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

package android.hardware.input.cts;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class InputCtsActivity extends Activity {
    private static final String TAG = "InputCtsActivity";

    private InputCallback mInputCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (mInputCallback != null) {
            mInputCallback.onMotionEvent(ev);
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mInputCallback != null) {
            mInputCallback.onMotionEvent(ev);
        }
        return true;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if (mInputCallback != null) {
            mInputCallback.onMotionEvent(ev);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (mInputCallback != null) {
            mInputCallback.onKeyEvent(ev);
        }
        return true;
    }

    public void setInputCallback(InputCallback callback) {
        mInputCallback = callback;
    }
}
