/*
 * Copyright (C) 2015 The Android Open Source Project
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
 *
 *
 * This code was provided to AOSP by Zimperium Inc and was
 * written by:
 *
 * Simone "evilsocket" Margaritelli
 * Joshua "jduck" Drake
 */
package android.security.cts;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import android.os.Looper;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.view.Surface;
import android.webkit.cts.CtsTestServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.android.cts.security.R;


/**
 * Verify that the device is not vulnerable to any known Stagefright
 * vulnerabilities.
 */
public class StagefrightTest extends InstrumentationTestCase {
    static final String TAG = "StagefrightTest";

    private final long TIMEOUT_NS = 10000000000L;  // 10 seconds.

    public StagefrightTest() {
    }

    /***********************************************************
     to prevent merge conflicts, add K tests below this comment,
     before any existing test methods
     ***********************************************************/

    public void testStagefright_bug_35763994() throws Exception {
        doStagefrightTest(R.raw.bug_35763994);
    }

    public void testStagefright_cve_2016_2507() throws Exception {
        doStagefrightTest(R.raw.cve_2016_2507);
    }

    public void testStagefright_bug_31647370() throws Exception {
        doStagefrightTest(R.raw.bug_31647370);
    }

    public void testStagefright_bug_32577290() throws Exception {
        doStagefrightTest(R.raw.bug_32577290);
    }

    public void testStagefright_cve_2015_1538_1() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_1);
    }

    public void testStagefright_cve_2015_1538_2() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_2);
    }

    public void testStagefright_cve_2015_1538_3() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_3);
    }

    public void testStagefright_cve_2015_1538_4() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_4);
    }

    public void testStagefright_cve_2015_1539() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1539);
    }

    public void testStagefright_cve_2015_3824() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3824);
    }

    public void testStagefright_cve_2015_3826() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3826);
    }

    public void testStagefright_cve_2015_3827() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3827);
    }

    public void testStagefright_cve_2015_3828() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3828);
    }

    public void testStagefright_cve_2015_3829() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3829);
    }

    public void testStagefright_cve_2015_3864() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3864);
    }

    public void testStagefright_cve_2015_6598() throws Exception {
        doStagefrightTest(R.raw.cve_2015_6598);
    }

    public void testStagefright_bug_26366256() throws Exception {
        doStagefrightTest(R.raw.bug_26366256);
    }

    public void testStagefright_cve_2016_2429_b_27211885() throws Exception {
        doStagefrightTest(R.raw.cve_2016_2429_b_27211885);
    }

    public void testStagefright_bug_34031018() throws Exception {
        doStagefrightTest(R.raw.bug_34031018_32bit);
        doStagefrightTest(R.raw.bug_34031018_64bit);
    }

    private void doStagefrightTest(final int rid) throws Exception {
        doStagefrightTestMediaPlayer(rid);
        doStagefrightTestMediaCodec(rid);
        doStagefrightTestMediaMetadataRetriever(rid);

        Context context = getInstrumentation().getContext();
        Resources resources =  context.getResources();
        CtsTestServer server = new CtsTestServer(context);
        String rname = resources.getResourceEntryName(rid);
        String url = server.getAssetUrl("raw/" + rname);
        doStagefrightTestMediaPlayer(url);
        doStagefrightTestMediaCodec(url);
        doStagefrightTestMediaMetadataRetriever(url);
        server.shutdown();
    }

    private Surface getDummySurface() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        SurfaceTexture surfaceTex = new SurfaceTexture(textures[0]);
        surfaceTex.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                Log.i(TAG, "new frame available");
            }
        });
        return new Surface(surfaceTex);
    }

    class MediaPlayerCrashListener
    implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
        @Override
        public boolean onError(MediaPlayer mp, int newWhat, int extra) {
            Log.i(TAG, "error: " + newWhat + "/" + extra);
            // don't overwrite a more severe error with a less severe one
            if (what != MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                what = newWhat;
            }
            lock.lock();
            condition.signal();
            lock.unlock();

            return true; // don't call oncompletion
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            // preserve error condition, if any
            lock.lock();
            condition.signal();
            lock.unlock();
        }

        public int waitForError() throws InterruptedException {
            lock.lock();
            if (condition.awaitNanos(TIMEOUT_NS) <= 0) {
                Log.d(TAG, "timed out on waiting for error");
            }
            lock.unlock();
            if (what != 0) {
                // Sometimes mediaserver signals a decoding error first, and *then* crashes
                // due to additional in-flight buffers being processed, so wait a little
                // and see if more errors show up.
                SystemClock.sleep(1000);
            }
            return what;
        }

        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        int what;
    }

    class LooperThread extends Thread {
        private Looper mLooper;

        LooperThread(Runnable runner) {
            super(runner);
        }

        @Override
        public void run() {
            Looper.prepare();
            mLooper = Looper.myLooper();
            super.run();
        }

        public void stopLooper() {
            mLooper.quitSafely();
        }
    }

    private void doStagefrightTestMediaPlayer(final int rid) throws Exception {
        doStagefrightTestMediaPlayer(rid, null);
    }

    private void doStagefrightTestMediaPlayer(final String url) throws Exception {
        doStagefrightTestMediaPlayer(-1, url);
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private void doStagefrightTestMediaPlayer(final int rid, final String uri) throws Exception {

        String name = uri != null ? uri :
            getInstrumentation().getContext().getResources().getResourceEntryName(rid);
        Log.i(TAG, "start mediaplayer test for: " + name);

        final MediaPlayerCrashListener mpcl = new MediaPlayerCrashListener();

        LooperThread t = new LooperThread(new Runnable() {
            @Override
            public void run() {

                MediaPlayer mp = new MediaPlayer();
                mp.setOnErrorListener(mpcl);
                mp.setOnPreparedListener(mpcl);
                mp.setOnCompletionListener(mpcl);
                Surface surface = getDummySurface();
                mp.setSurface(surface);
                AssetFileDescriptor fd = null;
                try {
                    if (uri == null) {
                        fd = getInstrumentation().getContext().getResources()
                                .openRawResourceFd(rid);

                        mp.setDataSource(fd.getFileDescriptor(),
                                         fd.getStartOffset(),
                                         fd.getLength());
                    } else {
                        mp.setDataSource(uri);
                    }
                    mp.prepareAsync();
                } catch (Exception e) {
                } finally {
                    closeQuietly(fd);
                }

                Looper.loop();
                mp.release();
            }
        });

        t.start();
        String cve = name.replace("_", "-").toUpperCase();
        assertFalse("Device *IS* vulnerable to " + cve,
                    mpcl.waitForError() == MediaPlayer.MEDIA_ERROR_SERVER_DIED);
        t.stopLooper();
        t.join(); // wait for thread to exit so we're sure the player was released
    }

    private void doStagefrightTestMediaCodec(final int rid) throws Exception {
        doStagefrightTestMediaCodec(rid, null);
    }

    private void doStagefrightTestMediaCodec(final String url) throws Exception {
        doStagefrightTestMediaCodec(-1, url);
    }

    private void doStagefrightTestMediaCodec(final int rid, final String url) throws Exception {

        final MediaPlayerCrashListener mpcl = new MediaPlayerCrashListener();

        LooperThread thr = new LooperThread(new Runnable() {
            @Override
            public void run() {

                MediaPlayer mp = new MediaPlayer();
                mp.setOnErrorListener(mpcl);
                try {
                    AssetFileDescriptor fd = getInstrumentation().getContext().getResources()
                        .openRawResourceFd(R.raw.good);

                    // the onErrorListener won't receive MEDIA_ERROR_SERVER_DIED until
                    // setDataSource has been called
                    mp.setDataSource(fd.getFileDescriptor(),
                                     fd.getStartOffset(),
                                     fd.getLength());
                    fd.close();
                } catch (Exception e) {
                    // this is a known-good file, so no failure should occur
                    fail("setDataSource of known-good file failed");
                }

                synchronized(mpcl) {
                    mpcl.notify();
                }
                Looper.loop();
                mp.release();
            }
        });
        thr.start();
        // wait until the thread has initialized the MediaPlayer
        synchronized(mpcl) {
            mpcl.wait();
        }

        Resources resources =  getInstrumentation().getContext().getResources();
        MediaExtractor ex = new MediaExtractor();
        if (url == null) {
            AssetFileDescriptor fd = resources.openRawResourceFd(rid);
            try {
                ex.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            } catch (IOException e) {
                // ignore
            } finally {
                closeQuietly(fd);
            }
        } else {
            ex.setDataSource(url);
        }
        int numtracks = ex.getTrackCount();
        String rname = url != null ? url: resources.getResourceEntryName(rid);
        Log.i(TAG, "start mediacodec test for: " + rname + ", which has " + numtracks + " tracks");
        for (int t = 0; t < numtracks; t++) {
            // find all the available decoders for this format
            ArrayList<String> matchingCodecs = new ArrayList<String>();
            MediaFormat format = null;
            try {
                format = ex.getTrackFormat(t);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "could not get track format for track " + t);
                continue;
            }
            String mime = format.getString(MediaFormat.KEY_MIME);
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                if (info.isEncoder()) {
                    continue;
                }
                try {
                    MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
                    if (caps != null) {
                        matchingCodecs.add(info.getName());
                    }
                } catch (IllegalArgumentException e) {
                    // type is not supported
                }
            }

            if (matchingCodecs.size() == 0) {
                Log.w(TAG, "no codecs for track " + t + ", type " + mime);
            }
            // decode this track once with each matching codec
            ex.selectTrack(t);
            for (String codecName: matchingCodecs) {
                Log.i(TAG, "Decoding track " + t + " using codec " + codecName);
                ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                MediaCodec codec = MediaCodec.createByCodecName(codecName);
                Surface surface = null;
                if (mime.startsWith("video/")) {
                    surface = getDummySurface();
                }
                try {
                    codec.configure(format, surface, null, 0);
                    codec.start();
                } catch (Exception e) {
                    Log.i(TAG, "Failed to start/configure:", e);
                }
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                try {
                    ByteBuffer [] inputBuffers = codec.getInputBuffers();
                    while (true) {
                        int flags = ex.getSampleFlags();
                        long time = ex.getSampleTime();
                        ex.getCachedDuration();
                        int bufidx = codec.dequeueInputBuffer(5000);
                        if (bufidx >= 0) {
                            int n = ex.readSampleData(inputBuffers[bufidx], 0);
                            if (n < 0) {
                                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                                time = 0;
                                n = 0;
                            }
                            codec.queueInputBuffer(bufidx, 0, n, time, flags);
                            ex.advance();
                        }
                        int status = codec.dequeueOutputBuffer(info, 5000);
                        if (status >= 0) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break;
                            }
                            if (info.presentationTimeUs > TIMEOUT_NS / 1000) {
                                Log.d(TAG, "stopping after 10 seconds worth of data");
                                break;
                            }
                            codec.releaseOutputBuffer(status, true);
                        }
                    }
                } catch (Exception e) {
                    // local exceptions ignored, not security issues
                } finally {
                    codec.release();
                }
            }
        }
        ex.release();
        String cve = rname.replace("_", "-").toUpperCase();
        assertFalse("Device *IS* vulnerable to " + cve,
                    mpcl.waitForError() == MediaPlayer.MEDIA_ERROR_SERVER_DIED);
        thr.stopLooper();
        thr.join();
    }

    private void doStagefrightTestMediaMetadataRetriever(final int rid) throws Exception {
        doStagefrightTestMediaMetadataRetriever(rid, null);
    }

    private void doStagefrightTestMediaMetadataRetriever(final String url) throws Exception {
        doStagefrightTestMediaMetadataRetriever(-1, url);
    }

    private void doStagefrightTestMediaMetadataRetriever(
            final int rid, final String url) throws Exception {

        final MediaPlayerCrashListener mpcl = new MediaPlayerCrashListener();

        LooperThread thr = new LooperThread(new Runnable() {
            @Override
            public void run() {

                MediaPlayer mp = new MediaPlayer();
                mp.setOnErrorListener(mpcl);
                try {
                    AssetFileDescriptor fd = getInstrumentation().getContext().getResources()
                        .openRawResourceFd(R.raw.good);

                    // the onErrorListener won't receive MEDIA_ERROR_SERVER_DIED until
                    // setDataSource has been called
                    mp.setDataSource(fd.getFileDescriptor(),
                                     fd.getStartOffset(),
                                     fd.getLength());
                    fd.close();
                } catch (Exception e) {
                    // this is a known-good file, so no failure should occur
                    fail("setDataSource of known-good file failed");
                }

                synchronized(mpcl) {
                    mpcl.notify();
                }
                Looper.loop();
                mp.release();
            }
        });
        thr.start();
        // wait until the thread has initialized the MediaPlayer
        synchronized(mpcl) {
            mpcl.wait();
        }

        Resources resources =  getInstrumentation().getContext().getResources();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (url == null) {
            AssetFileDescriptor fd = resources.openRawResourceFd(rid);
            try {
                retriever.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            } catch (IllegalArgumentException e) {
                // ignore
            } finally {
                closeQuietly(fd);
            }
        } else {
            retriever.setDataSource(url, new HashMap<String, String>());
        }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.getEmbeddedPicture();
        retriever.getFrameAtTime();

        retriever.release();
        String rname = url != null ? url : resources.getResourceEntryName(rid);
        String cve = rname.replace("_", "-").toUpperCase();
        assertFalse("Device *IS* vulnerable to " + cve,
                    mpcl.waitForError() == MediaPlayer.MEDIA_ERROR_SERVER_DIED);
        thr.stopLooper();
        thr.join();
    }
}
