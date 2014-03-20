/*
 * Copyright 2014 The Android Open Source Project
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

package android.hardware.camera2.cts.helpers;

import android.hardware.camera2.CameraMetadata.Key;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.rules.ErrorCollector;

import java.util.Objects;

/**
 * A camera test ErrorCollector class to gather the test failures during a test,
 * instead of failing the test immediately for each failure.
 */
public class CameraErrorCollector extends ErrorCollector {
    private String mCameraMsg = "";

    @Override
    public void verify() throws Throwable {
        // Do not remove if using JUnit 3 test runners. super.verify() is protected.
        super.verify();
    }

    /**
     * Adds an unconditional error to the table. Execution continues, but test will fail at the end.
     *
     * @param message A string containing the failure reason.
     */
    public void addMessage(String message) {
        super.addError(new Throwable(mCameraMsg + message));
    }

    /**
     * Adds a Throwable to the table.  Execution continues, but the test will fail at the end.
     */
    @Override
    public void addError(Throwable error) {
        super.addError(new Throwable(mCameraMsg + error.getMessage(), error));
    }

    /**
     * Adds a failure to the table if {@code matcher} does not match {@code value}.
     * Execution continues, but the test will fail at the end if the match fails.
     * The camera id is included into the failure log.
     */
    @Override
    public <T> void checkThat(final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg, value, matcher);
    }

    /**
     * Adds a failure with the given {@code reason} to the table if
     * {@code matcher} does not match {@code value}. Execution continues, but
     * the test will fail at the end if the match fails. The camera id is
     * included into the failure log.
     */
    @Override
    public <T> void checkThat(final String reason, final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg + reason, value, matcher);

    }

    /**
     * Set the camera id to this error collector object for logging purpose.
     *
     * @param id The camera id to be set.
     */
    public void setCameraId(String id) {
        if (id != null) {
            mCameraMsg = "Test failed for camera " + id + ": ";
        } else {
            mCameraMsg = "";
        }
    }

    /**
     * Adds a failure to the table if {@code condition} is not {@code true}.
     * <p>
     * Execution continues, but the test will fail at the end if the condition
     * failed.
     * </p>
     *
     * @param msg Message to be logged when check fails.
     * @param condition Log the failure if it is not true.
     */
    public boolean expectTrue(String msg, boolean condition) {
        if (!condition) {
            addMessage(msg);
        }

        return condition;
    }

    /**
     * Check if the two values are equal.
     *
     * @param msg Message to be logged when check fails.
     * @param expected Expected value to be checked against.
     * @param actual Actual value to be checked.
     * @return {@code true} if the two values are equal, {@code false} otherwise.
     */
    public <T> boolean expectEquals(String msg, T expected, T actual) {
        if (!Objects.equals(expected, actual)) {
            addMessage(String.format("%s (expected = %s, actual = %s) ", msg, expected.toString(),
                    actual.toString()));
            return false;
        }

        return true;
    }

    /**
     * Check if the key value is not null and return the value.
     *
     * @param request The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @return The value of the key.
     */
    public <T> T expectKeyValueNotNull(Builder request, Key<T> key) {

        T value = request.get(key);
        if (value == null) {
            addMessage("Key " + key.getName() + " shouldn't be null");
        }

        return value;
    }

    /**
     * Check if the key is non-null and the value is not equal to target.
     *
     * @param request The The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @param expected The expected value of the CaptureRequest key.
     */
    public <T> void expectKeyValueNotEquals(Builder request, Key<T> key, T expected) {
        if (request == null || key == null || expected == null) {
            throw new IllegalArgumentException("request, key and target shouldn't be null");
        }

        T value;
        if ((value = expectKeyValueNotNull(request, key)) == null) {
            return;
        }

        String reason = "Key " + key.getName() + " shouldn't have value " + value.toString();
        checkThat(reason, value, CoreMatchers.not(expected));
    }

    /**
     * Check if the key is non-null and the value is equal to target.
     *
     * <p>Only check non-null if the target is null.</p>
     *
     * @param request The The {@link CaptureRequest#Builder} to get the key from.
     * @param key The {@link CaptureRequest} key to be checked.
     * @param expected The expected value of the CaptureRequest key.
     */
    public <T> void expectKeyValueEquals(Builder request, Key<T> key, T expected) {
        if (request == null || key == null || expected == null) {
            throw new IllegalArgumentException("request, key and target shouldn't be null");
        }

        T value;
        if ((value = expectKeyValueNotNull(request, key)) == null) {
            return;
        }

        String reason = "Key " + key.getName() + " value " + value.toString()
                + " doesn't match the expected value " + expected.toString();
        checkThat(reason, value, CoreMatchers.equalTo(expected));
    }
}
