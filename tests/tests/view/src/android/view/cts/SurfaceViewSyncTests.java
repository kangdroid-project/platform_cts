/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.view.cts;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.cts.surfacevalidator.AnimationFactory;
import android.view.cts.surfacevalidator.AnimationTestCase;
import android.view.cts.surfacevalidator.CapturedActivity;
import android.view.cts.surfacevalidator.ViewFactory;
import android.widget.FrameLayout;

import libcore.io.IoUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SuppressLint("RtlHardcoded")
public class SurfaceViewSyncTests {
    private static final String TAG = "SurfaceViewSyncTests";
    private static final int PERMISSION_DIALOG_WAIT_MS = 1000;

    /**
     * Want to be especially sure we don't leave up the permission dialog, so try and dismiss both
     * before and after test.
     */
    @Before
    @After
    public void setUp() throws UiObjectNotFoundException {
        // The permission dialog will be auto-opened by the activity - find it and accept
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector acceptButtonSelector = new UiSelector().resourceId("android:id/button1");
        UiObject acceptButton = uiDevice.findObject(acceptButtonSelector);
        if (acceptButton.waitForExists(PERMISSION_DIALOG_WAIT_MS)) {
            Log.d(TAG, "found permission dialog, dismissing...");
            assertTrue(acceptButton.click());
        }
    }

    private CapturedActivity getActivity() {
        return (CapturedActivity) mActivityRule.getActivity();
    }

    private MediaPlayer getMediaPlayer() {
        return getActivity().getMediaPlayer();
    }

    @Rule
    public ActivityTestRule mActivityRule = new ActivityTestRule<>(CapturedActivity.class);

    @Rule
    public TestName mName = new TestName();

    static ValueAnimator makeInfinite(ValueAnimator a) {
        a.setRepeatMode(ObjectAnimator.REVERSE);
        a.setRepeatCount(ObjectAnimator.INFINITE);
        a.setDuration(200);
        a.setInterpolator(new LinearInterpolator());
        return a;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ViewFactories
    ///////////////////////////////////////////////////////////////////////////

    private ViewFactory sEmptySurfaceViewFactory = SurfaceView::new;

    private ViewFactory sGreenSurfaceViewFactory = context -> {
        SurfaceView surfaceView = new SurfaceView(context);
        surfaceView.getHolder().setFixedSize(640, 480);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {}

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.GREEN);
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        return surfaceView;
    };

    private ViewFactory sVideoViewFactory = context -> {
        SurfaceView surfaceView = new SurfaceView(context);
        surfaceView.getHolder().setFixedSize(640, 480);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                getMediaPlayer().setSurface(holder.getSurface());
                getMediaPlayer().start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                getMediaPlayer().pause();
                getMediaPlayer().setSurface(null);
            }
        });
        return surfaceView;
    };

    ///////////////////////////////////////////////////////////////////////////
    // AnimationFactories
    ///////////////////////////////////////////////////////////////////////////

    private AnimationFactory sSmallScaleAnimationFactory = view -> {
        view.setPivotX(0);
        view.setPivotY(0);
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.01f, 1f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.01f, 1f);
        return makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY));
    };

    private AnimationFactory sBigScaleAnimationFactory = view -> {
        view.setTranslationX(10);
        view.setTranslationY(10);
        view.setPivotX(0);
        view.setPivotY(0);
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 3f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 3f);
        return makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY));
    };

    private AnimationFactory sTranslateAnimationFactory = view -> {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 10f, 30f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 10f, 30f);
        return makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY));
    };

    ///////////////////////////////////////////////////////////////////////////
    // Bad frame capture
    ///////////////////////////////////////////////////////////////////////////

    private void saveFailureCaptures(SparseArray<Bitmap> failFrames) {
        if (failFrames.size() == 0) return;

        String directoryName = Environment.getExternalStorageDirectory()
                + "/" + getClass().getSimpleName()
                + "/" + mName.getMethodName();
        File testDirectory = new File(directoryName);
        if (testDirectory.exists()) {
            String[] children = testDirectory.list();
            if (children == null) {
                return;
            }
            for (String file : children) {
                new File(testDirectory, file).delete();
            }
        } else {
            testDirectory.mkdirs();
        }

        for (int i = 0; i < failFrames.size(); i++) {
            int frameNr = failFrames.keyAt(i);
            Bitmap bitmap = failFrames.valueAt(i);

            String bitmapName =  "frame_" + frameNr + ".png";
            Log.d(TAG, "Saving file : " + bitmapName + " in directory : " + directoryName);

            File file = new File(directoryName, bitmapName);
            FileOutputStream fileStream = null;
            try {
                fileStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileStream);
                fileStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IoUtils.closeQuietly(fileStream);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Tests
    ///////////////////////////////////////////////////////////////////////////

    public void verifyTest(AnimationTestCase testCase) {
        CapturedActivity.TestResult result = getActivity().runTest(testCase);
        saveFailureCaptures(result.failures);

        float failRatio = 1.0f * result.failFrames / (result.failFrames + result.passFrames);
        assertTrue("Error: " + failRatio + " fail ratio - extremely high, is activity obstructed?",
                failRatio < 0.95f);
        assertTrue("Error: " + result.failFrames
                + " incorrect frames observed - incorrect positioning",
                result.failFrames == 0);
        float framesPerSecond = 1.0f * result.passFrames
                / TimeUnit.MILLISECONDS.toSeconds(CapturedActivity.CAPTURE_DURATION_MS);
        assertTrue("Error, only " + result.passFrames
                + " frames observed, virtual display only capturing at "
                + framesPerSecond + " frames per second",
                result.passFrames > 100);
    }

    /** Draws a moving 10x10 black rectangle, validates 100 pixels of black are seen each frame */
    @Test
    public void testSmallRect() {
        verifyTest(new AnimationTestCase(
                context -> new View(context) {
                    // draw a single pixel
                    final Paint sBlackPaint = new Paint();
                    @Override
                    protected void onDraw(Canvas canvas) {
                        canvas.drawRect(0, 0, 10, 10, sBlackPaint);
                    }

                    @SuppressWarnings("unused")
                    void setOffset(int offset) {
                        // Note: offset by integer values, to ensure no rounding
                        // is done in rendering layer, as that may be brittle
                        setTranslationX(offset);
                        setTranslationY(offset);
                    }
                },
                new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.TOP),
                view -> makeInfinite(ObjectAnimator.ofInt(view, "offset", 10, 30)),
                (blackishPixelCount, width, height) -> blackishPixelCount == 100));
    }

    /**
     * Verifies that a SurfaceView without a surface is entirely black, with pixel count being
     * approximate to avoid rounding brittleness.
     */
    @Test
    public void testEmptySurfaceView() {
        verifyTest(new AnimationTestCase(
                sEmptySurfaceViewFactory,
                new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.TOP),
                sTranslateAnimationFactory,
                (blackishPixelCount, width, height) ->
                        blackishPixelCount > 9000 && blackishPixelCount < 11000));
    }

    @Test
    public void testSurfaceViewSmallScale() {
        verifyTest(new AnimationTestCase(
                sGreenSurfaceViewFactory,
                new FrameLayout.LayoutParams(640, 480, Gravity.LEFT | Gravity.TOP),
                sSmallScaleAnimationFactory,
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }

    @Test
    public void testSurfaceViewBigScale() {
        verifyTest(new AnimationTestCase(
                sGreenSurfaceViewFactory,
                new FrameLayout.LayoutParams(640, 480, Gravity.LEFT | Gravity.TOP),
                sBigScaleAnimationFactory,
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }

    @Test
    public void testVideoSurfaceViewTranslate() {
        verifyTest(new AnimationTestCase(
                sVideoViewFactory,
                new FrameLayout.LayoutParams(640, 480, Gravity.LEFT | Gravity.TOP),
                sTranslateAnimationFactory,
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }

    @Test
    public void testVideoSurfaceViewRotated() {
        verifyTest(new AnimationTestCase(
                sVideoViewFactory,
                new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.TOP),
                view -> makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view,
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 10f, 30f),
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 10f, 30f),
                        PropertyValuesHolder.ofFloat(View.ROTATION, 45f, 45f))),
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }

    @Test
    public void testVideoSurfaceViewEdgeCoverage() {
        verifyTest(new AnimationTestCase(
                sVideoViewFactory,
                new FrameLayout.LayoutParams(640, 480, Gravity.CENTER),
                view -> {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    final int x = parent.getWidth() / 2;
                    final int y = parent.getHeight() / 2;

                    // Animate from left, to top, to right, to bottom
                    return makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view,
                            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -x, 0, x, 0, -x),
                            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0, -y, 0, y, 0)));
                },
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }

    @Test
    public void testVideoSurfaceViewCornerCoverage() {
        verifyTest(new AnimationTestCase(
                sVideoViewFactory,
                new FrameLayout.LayoutParams(640, 480, Gravity.CENTER),
                view -> {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    final int x = parent.getWidth() / 2;
                    final int y = parent.getHeight() / 2;

                    // Animate from top left, to top right, to bottom right, to bottom left
                    return makeInfinite(ObjectAnimator.ofPropertyValuesHolder(view,
                            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -x, x, x, -x, -x),
                            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -y, -y, y, y, -y)));
                },
                (blackishPixelCount, width, height) -> blackishPixelCount == 0));
    }
}
