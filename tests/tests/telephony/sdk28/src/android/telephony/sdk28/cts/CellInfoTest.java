/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.telephony.sdk28.cts;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Parcel;
import android.telephony.CellInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.util.Log;


import java.util.List;
import java.util.Objects;

public class CellInfoTest extends AndroidTestCase {
    private static final String TAG = "CellInfoTest";

    private static final int MAX_WAIT_SECONDS = 15;
    private static final int POLL_INTERVAL_MILLIS = 1000;

    private TelephonyManager mTm;
    private PackageManager mPm;

    private boolean isCamped() {
        ServiceState ss = mTm.getServiceState();
        if (ss == null) return false;
        return (ss.getState() == ServiceState.STATE_IN_SERVICE
                || ss.getState() == ServiceState.STATE_EMERGENCY_ONLY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mPm = getContext().getPackageManager();
    }

    public void testCellInfoSdk28() {
        if (!mPm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Log.d(TAG, "Skipping test that requires FEATURE_TELEPHONY");
            return;
        }

        if (!isCamped()) fail("Device is not camped to a cell");

        List<CellInfo> cellInfo = mTm.getAllCellInfo();

        // getAllCellInfo should never return null, and there should be at least one entry.
        assertNotNull("TelephonyManager.getAllCellInfo() returned NULL CellInfo", cellInfo);
        assertFalse("TelephonyManager.getAllCellInfo() returned an empty list", cellInfo.isEmpty());

        final long initialTime = cellInfo.get(0).getTimeStamp();

        for(int i = 0; i < MAX_WAIT_SECONDS; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS); // 1 second
            } catch (InterruptedException ie) {
                fail("Thread was interrupted");
            }
            List<CellInfo> newCellInfo = mTm.getAllCellInfo();
            assertNotNull("TelephonyManager.getAllCellInfo() returned NULL CellInfo", newCellInfo);
            assertFalse("TelephonyManager.getAllCellInfo() returned an empty list",
                    newCellInfo.isEmpty());
            // Test that new CellInfo has been retrieved from the modem
            if (newCellInfo.get(0).getTimeStamp() != initialTime) return;
        }
        fail("CellInfo failed to update after " + MAX_WAIT_SECONDS + " seconds.");
    }
}
