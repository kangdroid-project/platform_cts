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

package android.widget.cts;

import android.database.DataSetObserver;
import android.test.AndroidTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import static org.mockito.Mockito.*;

/**
 * Test {@link BaseAdapter}.
 */
public class BaseAdapterTest extends AndroidTestCase {
    public void testHasStableIds() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertFalse(baseAdapter.hasStableIds());
    }

    public void testDataSetObserver() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        DataSetObserver mockDataSetObserver = mock(DataSetObserver.class);

        verifyZeroInteractions(mockDataSetObserver);
        baseAdapter.notifyDataSetChanged();
        verifyZeroInteractions(mockDataSetObserver);

        baseAdapter.registerDataSetObserver(mockDataSetObserver);
        baseAdapter.notifyDataSetChanged();
        verify(mockDataSetObserver, times(1)).onChanged();

        reset(mockDataSetObserver);
        verifyZeroInteractions(mockDataSetObserver);
        baseAdapter.unregisterDataSetObserver(mockDataSetObserver);
        baseAdapter.notifyDataSetChanged();
        verifyZeroInteractions(mockDataSetObserver);
    }

    public void testNotifyDataSetInvalidated() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        DataSetObserver mockDataSetObserver = mock(DataSetObserver.class);

        verifyZeroInteractions(mockDataSetObserver);
        baseAdapter.notifyDataSetInvalidated();
        verifyZeroInteractions(mockDataSetObserver);

        baseAdapter.registerDataSetObserver(mockDataSetObserver);
        baseAdapter.notifyDataSetInvalidated();
        verify(mockDataSetObserver, times(1)).onInvalidated();
    }

    public void testAreAllItemsEnabled() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertTrue(baseAdapter.areAllItemsEnabled());
    }

    public void testIsEnabled() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertTrue(baseAdapter.isEnabled(0));
    }

    public void testGetDropDownView() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertNull(baseAdapter.getDropDownView(0, null, null));
    }

    public void testGetItemViewType() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertEquals(0, baseAdapter.getItemViewType(0));
    }

    public void testGetViewTypeCount() {
        BaseAdapter baseAdapter = new MockBaseAdapter();
        assertEquals(1, baseAdapter.getViewTypeCount());
    }

    public void testIsEmpty() {
        MockBaseAdapter baseAdapter = new MockBaseAdapter();

        baseAdapter.setCount(0);
        assertTrue(baseAdapter.isEmpty());

        baseAdapter.setCount(1);
        assertFalse(baseAdapter.isEmpty());
    }

    private static class MockBaseAdapter extends BaseAdapter {
        private int mCount = 0;

        public void setCount(int count) {
            mCount = count;
        }

        public int getCount() {
            return mCount;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
}
