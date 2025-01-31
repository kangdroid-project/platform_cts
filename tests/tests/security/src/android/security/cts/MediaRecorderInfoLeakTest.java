/**
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

package android.security.cts;

import android.platform.test.annotations.SecurityTest;
import android.media.MediaRecorder;
import android.test.AndroidTestCase;

@SecurityTest
public class MediaRecorderInfoLeakTest extends AndroidTestCase {

   /**
    *  b/27855172
    */
    @SecurityTest(minPatchLevel = "2016-06")
    public void test_cve_2016_2499() throws Exception {
        MediaRecorder mediaRecorder = null;
        try {
            for (int i = 0; i < 1000; i++) {
              mediaRecorder = new MediaRecorder();
              mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
              mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
              mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
              mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
              mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
              mediaRecorder.setVideoFrameRate(30);
              mediaRecorder.setVideoSize(352, 288);
              mediaRecorder.setOutputFile("/sdcard/record.output");
              mediaRecorder.prepare();
              int test = mediaRecorder.getMaxAmplitude();
              mediaRecorder.release();
              if(test != 0){
                fail("MediaRecorderInfoLeakTest failed");
              }
            }
        } catch (Exception e) {
            fail("Media Recorder Exception" + e.getMessage());
        } finally {
            if (mediaRecorder != null){
                mediaRecorder.release();
            }
        }
      }
}
