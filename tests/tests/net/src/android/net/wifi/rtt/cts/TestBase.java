/*
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

package android.net.wifi.rtt.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Wi-Fi RTT CTS test cases. Provides a uniform configuration and event management
 * facility.
 */
public class TestBase extends AndroidTestCase {
    protected static final String TAG = "WifiRttCtsTests";

    // wait for Wi-Fi to enable and/or connect
    private static final int WAIT_FOR_WIFI_ENABLE_SECS = 10;

    // wait for Wi-Fi RTT to become available
    private static final int WAIT_FOR_RTT_CHANGE_SECS = 10;

    // wait for Wi-Fi scan results to become available
    private static final int WAIT_FOR_SCAN_RESULTS_SECS = 20;

    protected WifiRttManager mWifiRttManager;
    protected WifiManager mWifiManager;
    private LocationManager mLocationManager;
    private WifiManager.WifiLock mWifiLock;

    protected IntentFilter mRttIntent;
    protected IntentFilter mWifiStateIntent;
    protected IntentFilter mWifiConnectedIntent;

    private final HandlerThread mHandlerThread = new HandlerThread("SingleDeviceTest");
    protected final Executor mExecutor;
    {
        mHandlerThread.start();
        mExecutor = new HandlerExecutor(new Handler(mHandlerThread.getLooper()));
    }

    /**
     * Returns a flag indicating whether or not Wi-Fi RTT should be tested. Wi-Fi RTT
     * should be tested if the feature is supported on the current device.
     */
    static boolean shouldTestWifiRtt(Context context) {
        // TODO b/74457054: enable when t/31350604 resolved
        return false;
//        final PackageManager pm = context.getPackageManager();
//        return pm.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!shouldTestWifiRtt(getContext())) {
            return;
        }

        mLocationManager = (LocationManager) getContext().getSystemService(
                Context.LOCATION_SERVICE);
        assertTrue("RTT testing requires Location to be enabled",
                mLocationManager.isLocationEnabled());

        mWifiRttManager = (WifiRttManager) getContext().getSystemService(
                Context.WIFI_RTT_RANGING_SERVICE);
        assertNotNull("Wi-Fi RTT Manager", mWifiRttManager);

        mRttIntent = new IntentFilter();
        mRttIntent.addAction(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);

        mWifiStateIntent = new IntentFilter();
        mWifiStateIntent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mWifiConnectedIntent = new IntentFilter();
        mWifiConnectedIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull("Wi-Fi Manager", mWifiManager);
        mWifiLock = mWifiManager.createWifiLock(TAG);
        mWifiLock.acquire();
        assertTrue("Location scans must be enabled", mWifiManager.isScanAlwaysAvailable());
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            WifiEnableBroadcastReceiver receiver = new WifiEnableBroadcastReceiver(true);
            mContext.registerReceiver(receiver, mWifiStateIntent);
            receiver.waitForDesiredState();
            mContext.unregisterReceiver(receiver);
        }

        if (!mWifiManager.getConnectionInfo().getSupplicantState().equals(
                SupplicantState.COMPLETED)) {
            WifiConnectedBroadcastReceiver receiver = new WifiConnectedBroadcastReceiver();
            mContext.registerReceiver(receiver, mWifiConnectedIntent);
            receiver.waitForConnected();
            mContext.unregisterReceiver(receiver);
        }

        if (!mWifiRttManager.isAvailable()) {
            WifiRttBroadcastReceiver receiver = new WifiRttBroadcastReceiver();
            mContext.registerReceiver(receiver, mRttIntent);
            assertTrue("Timeout waiting for Wi-Fi RTT to change status",
                    receiver.waitForStateChange());
            assertTrue("Wi-Fi RTT is not available (should be)", mWifiRttManager.isAvailable());
            mContext.unregisterReceiver(receiver);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (!shouldTestWifiRtt(getContext())) {
            super.tearDown();
            return;
        }

        super.tearDown();
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {
        private CountDownLatch mBlocker = new CountDownLatch(1);
        private String mAction;
        private int mTimeout;

        SyncBroadcastReceiver(String action, int timeout) {
            mAction = action;
            mTimeout = timeout;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAction.equals(intent.getAction())) {
                mBlocker.countDown();
            }
        }

        boolean waitForStateChange() throws InterruptedException {
            return mBlocker.await(mTimeout, TimeUnit.SECONDS);
        }
    };

    class WifiEnableBroadcastReceiver extends BroadcastReceiver {
        private CountDownLatch mBlocker = new CountDownLatch(1);
        private boolean mDesiredState;

        WifiEnableBroadcastReceiver(boolean desiredState) {
            mDesiredState = desiredState;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                if (mWifiManager.isWifiEnabled() == mDesiredState) {
                    mBlocker.countDown();
                }
            }
        }

        boolean waitForDesiredState() throws InterruptedException {
            return mBlocker.await(WAIT_FOR_WIFI_ENABLE_SECS, TimeUnit.SECONDS);
        }
    }

    class WifiConnectedBroadcastReceiver extends BroadcastReceiver {
        private CountDownLatch mBlocker = new CountDownLatch(1);

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (ni.getState() == NetworkInfo.State.CONNECTED) {
                    mBlocker.countDown();
                }
            }
        }

        boolean waitForConnected() throws InterruptedException {
            return mBlocker.await(WAIT_FOR_WIFI_ENABLE_SECS, TimeUnit.SECONDS);
        }
    }

    class WifiRttBroadcastReceiver extends SyncBroadcastReceiver {
        WifiRttBroadcastReceiver() {
            super(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED, WAIT_FOR_RTT_CHANGE_SECS);
        }
    }

    class WifiScansBroadcastReceiver extends SyncBroadcastReceiver {
        WifiScansBroadcastReceiver() {
            super(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION, WAIT_FOR_SCAN_RESULTS_SECS);
        }
    }

    class ResultCallback extends RangingResultCallback {
        private CountDownLatch mBlocker = new CountDownLatch(1);
        private int mCode; // 0: success, otherwise RangingResultCallback STATUS_CODE_*.
        private List<RangingResult> mResults;

        @Override
        public void onRangingFailure(int code) {
            mCode = code;
            mResults = null; // not necessary since intialized to null - but for completeness
            mBlocker.countDown();
        }

        @Override
        public void onRangingResults(List<RangingResult> results) {
            mCode = 0; // not necessary since initialized to 0 - but for completeness
            mResults = results;
            mBlocker.countDown();
        }

        /**
         * Waits for the listener callback to be called - or an error (timeout, interruption).
         * Returns true on callback called, false on error (timeout, interruption).
         */
        boolean waitForCallback() throws InterruptedException {
            return mBlocker.await(WAIT_FOR_RTT_CHANGE_SECS, TimeUnit.SECONDS);
        }

        /**
         * Returns the code of the callback operation. Will be 0 for success (onRangingResults
         * called), else (if onRangingFailure called) will be one of the STATUS_CODE_* values.
         */
        int getCode() {
            return mCode;
        }

        /**
         * Returns the list of ranging results. In cases of error (getCode() != 0) will return null.
         */
        List<RangingResult> getResults() {
            return mResults;
        }
    }

    /**
     * Start a scan and return a list of observed ScanResults (APs).
     */
    protected List<ScanResult> scanAps() throws InterruptedException {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        WifiScansBroadcastReceiver receiver = new WifiScansBroadcastReceiver();
        mContext.registerReceiver(receiver, intentFilter);

        mWifiManager.startScan();
        receiver.waitForStateChange();
        mContext.unregisterReceiver(receiver);
        return mWifiManager.getScanResults();
    }

    /**
     * Start a scan and return the test AP with the specified SSID and which supports IEEE 802.11mc.
     * If the AP is not found re-attempts the scan maxScanRetries times (i.e. total number of
     * scans can be maxScanRetries + 1).
     *
     * Returns null if test AP is not found in the specified number of scans.
     *
     * @param bssid The BSSID of the test AP
     * @param maxScanRetries Maximum number of scans retries (in addition to first scan).
     */
    protected ScanResult scanForTestAp(String bssid, int maxScanRetries)
            throws InterruptedException {
        int scanCount = 0;
        while (scanCount <= maxScanRetries) {
            for (ScanResult scanResult : scanAps()) {
                if (!scanResult.is80211mcResponder()) {
                    continue;
                }
                if (!bssid.equals(scanResult.BSSID)) {
                    continue;
                }

                return scanResult;
            }

            scanCount++;
        }

        return null;
    }
}
