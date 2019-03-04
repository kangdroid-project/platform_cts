/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.method.cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.widget.TextView.BufferType;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.CtsKeyEventUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class NumberKeyListenerTest extends KeyListenerTestCase {
    private MockNumberKeyListener mMockNumberKeyListener;

    /**
     * Check point:
     * 1. Filter "Android test", return "".
     * 2. Filter "12345", return null.
     * 3. Filter "", return null.
     * 4. Filter "12345 Android", return "12345".
     * 5. Filter Spanned("12345 Android"), return Spanned("12345") and copy spans.
     */
    @Test
    public void testFilter() {
        mMockNumberKeyListener = new MockNumberKeyListener(MockNumberKeyListener.DIGITS);
        String source = "Android test";
        SpannableString dest = new SpannableString("012345");
        assertEquals("", mMockNumberKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()).toString());

        source = "12345";
        dest = new SpannableString("012345");
        assertNull(mMockNumberKeyListener.filter(source, 0, source.length(), dest, 0,
                dest.length()));

        source = "";
        dest = new SpannableString("012345");
        assertNull(mMockNumberKeyListener.filter(source, 0, source.length(), dest, 0,
                dest.length()));

        source = "12345 Android";
        dest = new SpannableString("012345 Android-test");
        assertEquals("12345", mMockNumberKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()).toString());

        Object what = new Object();
        Spannable spannableSource = new SpannableString("12345 Android");
        spannableSource.setSpan(what, 0, spannableSource.length(), Spanned.SPAN_POINT_POINT);
        Spanned filtered = (Spanned) mMockNumberKeyListener.filter(spannableSource,
                0, spannableSource.length(), dest, 0, dest.length());
        assertEquals("12345", filtered.toString());
        assertEquals(Spanned.SPAN_POINT_POINT, filtered.getSpanFlags(what));
        assertEquals(0, filtered.getSpanStart(what));
        assertEquals("12345".length(), filtered.getSpanEnd(what));

        try {
            mMockNumberKeyListener.filter(null, 0, 1, dest, 0, dest.length());
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Check point:
     * If one of the chars in the getAcceptedChars() can be generated by the keyCode of this
     * key event, return the char; otherwise return '\0'.
     */
    @Test
    public void testLookup() {
        mMockNumberKeyListener = new MockNumberKeyListener(MockNumberKeyListener.DIGITS);
        KeyEvent event1 = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        SpannableString str = new SpannableString("012345");
        assertEquals('0', mMockNumberKeyListener.lookup(event1, str));

        mMockNumberKeyListener = new MockNumberKeyListener(MockNumberKeyListener.NOTHING);
        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
        str = new SpannableString("ABCD");
        assertEquals('\0', mMockNumberKeyListener.lookup(event2, str));

        try {
            mMockNumberKeyListener.lookup(null, str);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @Test
    public void testOk() {
        mMockNumberKeyListener = new MockNumberKeyListener(MockNumberKeyListener.DIGITS);

        assertTrue(mMockNumberKeyListener.callOk(mMockNumberKeyListener.getAcceptedChars(), '3'));
        assertFalse(mMockNumberKeyListener.callOk(mMockNumberKeyListener.getAcceptedChars(), 'e'));

        try {
            mMockNumberKeyListener.callOk(null, 'm');
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Check point:
     * 1. Press '0' key, '0' will be added to the text.
     * 2. Press an unaccepted key if it exists, it will not be added.
     * 3. remove NumberKeyListener and press '0' key, '0' will not be added.
     */
    @Test
    public void testPressKey() {
        final CharSequence text = "123456";
        final MockNumberKeyListener mockNumberKeyListener =
            new MockNumberKeyListener(MockNumberKeyListener.DIGITS);

        mActivity.runOnUiThread(() -> {
            mTextView.setText(text, BufferType.EDITABLE);
            mTextView.setKeyListener(mockNumberKeyListener);
            mTextView.requestFocus();
            Selection.setSelection(mTextView.getText(), 0, 0);
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("123456", mTextView.getText().toString());
        // press '0' key.
        CtsKeyEventUtil.sendKeys(mInstrumentation, mTextView, KeyEvent.KEYCODE_0);
        assertEquals("0123456", mTextView.getText().toString());

        // an unaccepted key if it exists.
        int keyCode = TextMethodUtils.getUnacceptedKeyCode(MockNumberKeyListener.DIGITS);
        if (-1 != keyCode) {
            CtsKeyEventUtil.sendKeys(mInstrumentation, mTextView, keyCode);
            // text of TextView will not be changed.
            assertEquals("0123456", mTextView.getText().toString());
        }

        mActivity.runOnUiThread(() -> {
            mTextView.setKeyListener(null);
            mTextView.requestFocus();
        });
        mInstrumentation.waitForIdleSync();
        // press '0' key.
        CtsKeyEventUtil.sendKeys(mInstrumentation, mTextView, KeyEvent.KEYCODE_0);
        assertEquals("0123456", mTextView.getText().toString());
    }

    /**
     * A mocked {@link android.text.method.NumberKeyListener} for testing purposes.
     *
     * Allows {@link NumberKeyListenerTest} to call
     * {@link android.text.method.NumberKeyListener#getAcceptedChars()},
     * {@link android.text.method.NumberKeyListener#lookup(KeyEvent, Spannable)}, and
     * {@link android.text.method.NumberKeyListener@ok(char[], char)}.
     */
    private static class MockNumberKeyListener extends NumberKeyListener {
        static final char[] DIGITS =
                new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        static final char[] NOTHING = new char[0];

        private final char[] mAcceptedChars;

        MockNumberKeyListener(char[] acceptedChars) {
            this.mAcceptedChars = acceptedChars;
        }

        @Override
        protected char[] getAcceptedChars() {
            return mAcceptedChars;
        }

        @Override
        protected int lookup(KeyEvent event, Spannable content) {
            return super.lookup(event, content);
        }

        public boolean callOk(char[] accept, char c) {
            return NumberKeyListener.ok(accept, c);
        }

        public int getInputType() {
            return 0;
        }
    }
}
