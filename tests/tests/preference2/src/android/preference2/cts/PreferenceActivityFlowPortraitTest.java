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

package android.preference2.cts;

import static android.content.pm.PackageManager.FEATURE_LEANBACK;

import static org.junit.Assume.assumeFalse;

import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Portrait tests setup for {@link PreferenceActivityFlowTest}
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreferenceActivityFlowPortraitTest extends PreferenceActivityFlowTest {

    @Rule
    public ActivityTestRule<PreferenceWithHeadersPortrait> mActivityRule =
            new ActivityTestRule<>(PreferenceWithHeadersPortrait.class, true, false);

    @Before
    public void setup() {
        PackageManager pm = InstrumentationRegistry.getTargetContext().getPackageManager();
        // Ignore this test on Leanback since Leanback doesn't support portrait orientation
        assumeFalse(pm.hasSystemFeature(FEATURE_LEANBACK));
        mTestUtils = new TestUtils();
    }

    /**
     * Portrait setup of {@link #switchHeadersInner}.
     */
    @Test
    public void switchHeadersPortraitTest() {
        switchHeadersInner();
    }

    /**
     * Portrait setup of {@link #smallScreenNoHighlightInHeadersListInner}.
     */
    @Test
    public void smallScreenNoHighlightInHeadersListTest() {
        smallScreenNoHighlightInHeadersListInner();
    }

    /**
     * Portrait setup of {@link #backPressToExitInner}.
     */
    @Test
    public void backPressToExitPortraitTest() {
        backPressToExitInner();
    }

    /**
     * Portrait setup of {@link #goToFragmentInner}.
     */
    @Test
    public void goToFragmentPortraitTest() {
        goToFragmentInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentInner}.
     */
    @Test
    public void startWithFragmentPortraitTest() {
        startWithFragmentInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentAndRecreateInner}.
     */
    @Test
    public void startWithFragmentAndRecreatePortraitTest() {
        startWithFragmentAndRecreateInner();
    }


    /**
     * Portrait setup of {@link #startWithFragmentAndInitTitleInner}.
     */
    @Test
    public void startWithFragmentAndInitTitlePortraitTest() {
        startWithFragmentAndInitTitleInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentAndInitTitleMultiWindowInner}.
     */
    @Test
    public void startWithFragmentAndInitTitleMultiWindowPortraitTest() {
        startWithFragmentAndInitTitleMultiWindowInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentNoHeadersInner}.
     */
    @Test
    public void startWithFragmentNoHeadersPortraitTest() {
        startWithFragmentNoHeadersInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentNoHeadersButInitTitleInner}.
     */
    @Test
    public void startWithFragmentNoHeadersButInitTitlePortraitTest() {
        startWithFragmentNoHeadersButInitTitleInner();
    }

    /**
     * Portrait setup of {@link #startWithFragmentNoHeadersMultiWindowTest}.
     */
    @Test
    public void startWithFragmentNoHeadersMultiWindowPortraitTest() {
        startWithFragmentNoHeadersMultiWindowTest();
    }

    /**
     * Portrait setup of {@link #listDialogTest}.
     */
    @Test
    public void listDialogPortraitTest() {
        listDialogTest();
    }

    /**
     * Portrait setup of {@link #recreateTest}.
     */
    @Test
    public void recreatePortraitTest() {
        recreateTest();
    }

    /**
     * Portrait setup of {@link #recreateInnerFragmentTest}.
     */
    @Test
    public void recreateInnerFragmentPortraitTest() {
        recreateInnerFragmentTest();
    }

    /**
     * Portrait setup of {@link #multiWindowInOutTest}.
     */
    @Test
    public void multiWindowInOutPortraitTest() {
        multiWindowInOutTest();
    }

    /**
     * Portrait setup of {@link #multiWindowInnerFragmentInOutTest}.
     */
    @Test
    public void multiWindowInnerFragmentInOutPortraitTest() {
        multiWindowInnerFragmentInOutTest();
    }

    /**
     * Portrait setup of {@link #multiWindowInitialHeaderOnBackTest}.
     */
    @Test
    public void multiWindowInitialHeaderOnBackPortraitTest() {
        multiWindowInitialHeaderOnBackTest();
    }

    /**
     * Portrait setup of {@link #multiWindowHistoryPreserveTest}.
     */
    @Test
    public void multiWindowHistoryPreservePortraitTest() {
        multiWindowHistoryPreserveTest();
    }

    @Override
    protected PreferenceWithHeaders launchActivity(Intent intent) {
        if (intent != null) {
            intent.setClass(InstrumentationRegistry.getTargetContext(),
                    PreferenceWithHeadersPortrait.class);
        }
        return mActivityRule.launchActivity(intent);
    }

    @Override
    protected void runOnUiThread(final Runnable runnable) {
        try {
            mActivityRule.runOnUiThread(() -> {
                runnable.run();
            });
        } catch (Throwable ex) {
            throw new RuntimeException("Failure on the UI thread", ex);
        }
    }

}
