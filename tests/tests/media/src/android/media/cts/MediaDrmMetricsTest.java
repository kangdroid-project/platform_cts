/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.media.cts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import android.media.MediaDrm;
import android.media.MediaDrm.MediaDrmStateException;
import android.os.PersistableBundle;
import android.test.AndroidTestCase;
import android.util.Log;
import com.google.common.io.BaseEncoding;
import java.util.Base64;
import java.util.UUID;


/**
 * MediaDrm tests covering {@link MediaDrm#getMetrics} and related
 * functionality.
 */
public class MediaDrmMetricsTest extends AndroidTestCase {
    private static final String TAG = MediaDrmMetricsTest.class.getSimpleName();

    private static final UUID CLEARKEY_SCHEME_UUID =
            new UUID(0xe2719d58a985b3c9L, 0x781ab030af78d30eL);

    public void testGetMetricsEmpty() throws Exception {
        MediaDrm drm = new MediaDrm(CLEARKEY_SCHEME_UUID);
        assertNotNull(drm);

        PersistableBundle metrics = drm.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.isEmpty());
        drm.close();
    }

    public void testGetMetricsSession() throws Exception {
        MediaDrm drm = new MediaDrm(CLEARKEY_SCHEME_UUID);
        assertNotNull(drm);
        byte[] sid1 = drm.openSession();
        assertNotNull(sid1);
        byte[] sid2 = drm.openSession();
        assertNotNull(sid2);

        PersistableBundle metrics = drm.getMetrics();
        assertNotNull(metrics);
        assertEquals(3, metrics.keySet().size());

        assertEquals(2, metrics.getLong(
            MediaDrm.MetricsConstants.OPEN_SESSION_OK_COUNT, -1));
        assertEquals(-1, metrics.getLong(
            MediaDrm.MetricsConstants.OPEN_SESSION_ERROR_COUNT, -1));

        PersistableBundle startTimesMs = metrics.getPersistableBundle(
            MediaDrm.MetricsConstants.SESSION_START_TIMES_MS);
        assertNotNull(startTimesMs);
        assertEquals(2, startTimesMs.keySet().size());
        assertThat("Start times contain all session ids.",
            startTimesMs.keySet(), containsInAnyOrder(
                BaseEncoding.base16().encode(sid1),
                BaseEncoding.base16().encode(sid2)));

        PersistableBundle endTimesMs = metrics.getPersistableBundle(
            MediaDrm.MetricsConstants.SESSION_END_TIMES_MS);
        assertNotNull(endTimesMs);
        assertEquals(2, endTimesMs.keySet().size());
        assertThat("End times contain all session ids.",
            endTimesMs.keySet(), containsInAnyOrder(
                BaseEncoding.base16().encode(sid1),
                BaseEncoding.base16().encode(sid2)));
        drm.close();
    }

    public void testGetMetricsGetKeyRequest() throws Exception {
        MediaDrm drm = new MediaDrm(CLEARKEY_SCHEME_UUID);
        assertNotNull(drm);
        byte[] sid = drm.openSession();
        assertNotNull(sid);

        try {
          drm.getKeyRequest(sid, null, "", 2, null);
        } catch (MediaDrmStateException e) {
          // Exception expected.
        }

        drm.closeSession(sid);

        PersistableBundle metrics = drm.getMetrics();
        assertNotNull(metrics);

        // Verify the count of metric, operation counts and errors.
        assertEquals(6, metrics.keySet().size());
        assertEquals(1, metrics.getLong(
            MediaDrm.MetricsConstants.OPEN_SESSION_OK_COUNT, -1));
        assertEquals(1, metrics.getLong(
            MediaDrm.MetricsConstants.CLOSE_SESSION_OK_COUNT, -1));
        assertEquals(1, metrics.getLong(
            MediaDrm.MetricsConstants.GET_KEY_REQUEST_ERROR_COUNT));
        long[] errorList = metrics.getLongArray(
            MediaDrm.MetricsConstants.GET_KEY_REQUEST_ERROR_LIST);
        assertEquals(1, errorList.length);
        assertFalse(errorList[0] == 0);

        // Verify the start and end time groups in the nested
        // PersistableBundles.
        String hexSid = BaseEncoding.base16().encode(sid);
        PersistableBundle startTimesMs = metrics.getPersistableBundle(
            MediaDrm.MetricsConstants.SESSION_START_TIMES_MS);
        assertNotNull(startTimesMs);
        assertEquals(1, startTimesMs.keySet().size());
        assertEquals(startTimesMs.keySet().toArray()[0], hexSid);

        PersistableBundle endTimesMs = metrics.getPersistableBundle(
            MediaDrm.MetricsConstants.SESSION_END_TIMES_MS);
        assertNotNull(endTimesMs);
        assertEquals(1, endTimesMs.keySet().size());
        assertEquals(endTimesMs.keySet().toArray()[0], hexSid);
        assertThat(startTimesMs.getLong(hexSid),
            lessThanOrEqualTo(endTimesMs.getLong(hexSid)));
        drm.close();
    }
}
