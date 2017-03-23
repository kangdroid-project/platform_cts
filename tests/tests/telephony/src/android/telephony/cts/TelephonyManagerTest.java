/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.telephony.cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.compatibility.common.util.TestThread;
import com.android.internal.telephony.PhoneConstants;

import dalvik.annotation.TestTarget;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TelephonyManagerTest {
    private TelephonyManager mTelephonyManager;
    private PackageManager mPackageManager;
    private boolean mOnCellLocationChangedCalled = false;
    private ServiceState mServiceState;
    private final Object mLock = new Object();
    private static final int TOLERANCE = 1000;
    private PhoneStateListener mListener;
    private static ConnectivityManager mCm;
    private static final String TAG = "TelephonyManagerTest";

    @Before
    public void setUp() throws Exception {
        mTelephonyManager =
                (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mCm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mPackageManager = (PackageManager) getContext().getPackageManager();
    }

    @After
    public void tearDown() throws Exception {
        if (mListener != null) {
            // unregister the listener
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Test
    public void testListen() throws Throwable {
        if (mCm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null) {
            Log.d(TAG, "Skipping test that requires ConnectivityManager.TYPE_MOBILE");
            return;
        }

        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            // TODO: temp workaround, need to adjust test to for CDMA
            return;
        }

        // Test register
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mListener = new PhoneStateListener() {
                    @Override
                    public void onCellLocationChanged(CellLocation location) {
                        if(!mOnCellLocationChangedCalled) {
                            synchronized (mLock) {
                                mOnCellLocationChangedCalled = true;
                                mLock.notify();
                            }
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CELL_LOCATION);
                CellLocation.requestLocationUpdate();
                Looper.loop();
            }
        });
        t.start();
        synchronized (mLock) {
            mLock.wait(TOLERANCE);
        }
        assertTrue(mOnCellLocationChangedCalled);

        // Test unregister
        t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();
                // unregister the listener
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
                mOnCellLocationChangedCalled = false;
                // unregister again, to make sure doing so does not call the listener
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
                CellLocation.requestLocationUpdate();
                Looper.loop();
            }
        });

        t.start();
        synchronized (mLock) {
            mLock.wait(TOLERANCE);
        }
        assertFalse(mOnCellLocationChangedCalled);
    }

    /**
     * The getter methods here are all related to the information about the telephony.
     * These getters are related to concrete location, phone, service provider company, so
     * it's no need to get details of these information, just make sure they are in right
     * condition(>0 or not null).
     */
    @Test
    public void testTelephonyManager() {
        assertTrue(mTelephonyManager.getNetworkType() >= TelephonyManager.NETWORK_TYPE_UNKNOWN);
        assertTrue(mTelephonyManager.getPhoneType() >= TelephonyManager.PHONE_TYPE_NONE);
        assertTrue(mTelephonyManager.getSimState() >= TelephonyManager.SIM_STATE_UNKNOWN);
        assertTrue(mTelephonyManager.getDataActivity() >= TelephonyManager.DATA_ACTIVITY_NONE);
        assertTrue(mTelephonyManager.getDataState() >= TelephonyManager.DATA_DISCONNECTED);
        assertTrue(mTelephonyManager.getCallState() >= TelephonyManager.CALL_STATE_IDLE);

        for (int i = 0; i < mTelephonyManager.getPhoneCount(); ++i) {
            assertTrue(mTelephonyManager.getSimState(i) >= TelephonyManager.SIM_STATE_UNKNOWN);
        }

        // Make sure devices without MMS service won't fail on this
        if (mTelephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE) {
            assertFalse(mTelephonyManager.getMmsUserAgent().isEmpty());
            assertFalse(mTelephonyManager.getMmsUAProfUrl().isEmpty());
        }

        // The following methods may return any value depending on the state of the device. Simply
        // call them to make sure they do not throw any exceptions.
        mTelephonyManager.getVoiceMailNumber();
        mTelephonyManager.getSimOperatorName();
        mTelephonyManager.getNetworkCountryIso();
        mTelephonyManager.getCellLocation();
        mTelephonyManager.getSimSerialNumber();
        mTelephonyManager.getSimOperator();
        mTelephonyManager.getNetworkOperatorName();
        mTelephonyManager.getSubscriberId();
        mTelephonyManager.getLine1Number();
        mTelephonyManager.getNetworkOperator();
        mTelephonyManager.getSimCountryIso();
        mTelephonyManager.getVoiceMailAlphaTag();
        mTelephonyManager.getNeighboringCellInfo();
        mTelephonyManager.isNetworkRoaming();
        mTelephonyManager.getDeviceId();
        mTelephonyManager.getDeviceId(mTelephonyManager.getDefaultSim());
        mTelephonyManager.getDeviceSoftwareVersion();
        mTelephonyManager.getImei();
        mTelephonyManager.getImei(mTelephonyManager.getDefaultSim());
        mTelephonyManager.getPhoneCount();
        mTelephonyManager.getDataEnabled();
        mTelephonyManager.getNetworkSpecifier();
        TelecomManager telecomManager = (TelecomManager) getContext()
                .getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle defaultAccount = telecomManager
                .getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL);
        mTelephonyManager.getVoicemailRingtoneUri(defaultAccount);
        mTelephonyManager.isVoicemailVibrationEnabled(defaultAccount);
        mTelephonyManager.getCarrierConfig();
    }

    @Test
    public void testCreateForPhoneAccountHandle(){
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Log.d(TAG, "Skipping test that requires FEATURE_TELEPHONY");
            return;
        }
        TelecomManager telecomManager = getContext().getSystemService(TelecomManager.class);
        PhoneAccountHandle handle =
                telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL);
        TelephonyManager telephonyManager = mTelephonyManager.createForPhoneAccountHandle(handle);
        assertEquals(mTelephonyManager.getSubscriberId(), telephonyManager.getSubscriberId());
    }

    @Test
    public void testCreateForPhoneAccountHandle_InvalidHandle(){
        PhoneAccountHandle handle =
                new PhoneAccountHandle(new ComponentName("com.example.foo", "bar"), "baz");
        assertNull(mTelephonyManager.createForPhoneAccountHandle(handle));
    }

    /**
     * Tests that the phone count returned is valid.
     */
    @Test
    public void testGetPhoneCount() {
        int phoneCount = mTelephonyManager.getPhoneCount();
        int phoneType = mTelephonyManager.getPhoneType();
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM:
            case TelephonyManager.PHONE_TYPE_CDMA:
                assertTrue("Phone count should be > 0", phoneCount > 0);
                break;
            case TelephonyManager.PHONE_TYPE_NONE:
                assertTrue("Phone count should be 0", phoneCount == 0 || phoneCount == 1);
                break;
            default:
                throw new IllegalArgumentException("Did you add a new phone type? " + phoneType);
        }
    }

    /**
     * Tests that the device properly reports either a valid IMEI if
     * GSM, a valid MEID or ESN if CDMA, or a valid MAC address if
     * only a WiFi device.
     */
    @Test
    public void testGetDeviceId() {
        String deviceId = mTelephonyManager.getDeviceId();
        verifyDeviceId(deviceId);
    }

    /**
     * Tests that the device properly reports either a valid IMEI if
     * GSM, a valid MEID or ESN if CDMA, or a valid MAC address if
     * only a WiFi device.
     */
    @Test
    public void testGetDeviceIdForSlotId() {
        String deviceId = mTelephonyManager.getDeviceId(mTelephonyManager.getDefaultSim());
        verifyDeviceId(deviceId);
        // Also verify that no exception is thrown for any slot id (including invalid ones)
        for (int i = -1; i <= mTelephonyManager.getPhoneCount(); i++) {
            mTelephonyManager.getDeviceId(i);
        }
    }

    private void verifyDeviceId(String deviceId) {
        int phoneType = mTelephonyManager.getPhoneType();
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM:
                assertGsmDeviceId(deviceId);
                break;

            case TelephonyManager.PHONE_TYPE_CDMA:
                // LTE device is using IMEI as device id
                if (mTelephonyManager.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    assertGsmDeviceId(deviceId);
                } else {
                    assertCdmaDeviceId(deviceId);
                }
                break;

            case TelephonyManager.PHONE_TYPE_NONE:
                boolean nwSupported = mCm.isNetworkSupported(mCm.TYPE_WIFI);
                // only check serial number & MAC address if device report wifi feature
                if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                    assertSerialNumber();
                    assertMacAddress(getWifiMacAddress());
                } else if (mCm.getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH) != null) {
                    assertSerialNumber();
                    assertMacAddress(getBluetoothMacAddress());
                } else {
                    assertTrue(mCm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET) != null);
                }
                break;

            default:
                throw new IllegalArgumentException("Did you add a new phone type? " + phoneType);
        }
    }

    private static void assertGsmDeviceId(String deviceId) {
        // IMEI may include the check digit
        String imeiPattern = "[0-9]{14,15}";
        assertTrue("IMEI device id " + deviceId + " does not match pattern " + imeiPattern,
                Pattern.matches(imeiPattern, deviceId));
        if (deviceId.length() == 15) {
            // if the ID is 15 digits, the 15th must be a check digit.
            assertImeiCheckDigit(deviceId);
        }
    }

    private static void assertImeiCheckDigit(String deviceId) {
        int expectedCheckDigit = getLuhnCheckDigit(deviceId.substring(0, 14));
        int actualCheckDigit = Character.digit(deviceId.charAt(14), 10);
        assertEquals("Incorrect check digit for " + deviceId, expectedCheckDigit, actualCheckDigit);
    }

    /**
     * Use decimal value (0-9) to index into array to get sum of its digits
     * needed by Lunh check.
     *
     * Example: DOUBLE_DIGIT_SUM[6] = 3 because 6 * 2 = 12 => 1 + 2 = 3
     */
    private static final int[] DOUBLE_DIGIT_SUM = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};

    /**
     * Calculate the check digit by starting from the right, doubling every
     * each digit, summing all the digits including the doubled ones, and
     * finding a number to make the sum divisible by 10.
     *
     * @param deviceId not including the check digit
     * @return the check digit
     */
    private static int getLuhnCheckDigit(String deviceId) {
        int sum = 0;
        int dontDoubleModulus = deviceId.length() % 2;
        for (int i = deviceId.length() - 1; i >= 0; --i) {
            int digit = Character.digit(deviceId.charAt(i), 10);
            if (i % 2 == dontDoubleModulus) {
                sum += digit;
            } else {
                sum += DOUBLE_DIGIT_SUM[digit];
            }
        }
        sum %= 10;
        return sum == 0 ? 0 : 10 - sum;
    }

    private static void assertCdmaDeviceId(String deviceId) {
        // CDMA device IDs may either be a 14-hex-digit MEID or an
        // 8-hex-digit ESN.  If it's an ESN, it may not be a
        // pseudo-ESN.
        if (deviceId.length() == 14) {
            assertMeidFormat(deviceId);
        } else if (deviceId.length() == 8) {
            assertHexadecimalEsnFormat(deviceId);
        } else {
            fail("device id on CDMA must be 14-digit hex MEID or 8-digit hex ESN.");
        }
    }

    private static void assertHexadecimalEsnFormat(String deviceId) {
        String esnPattern = "[0-9a-fA-F]{8}";
        assertTrue("ESN hex device id " + deviceId + " does not match pattern " + esnPattern,
                   Pattern.matches(esnPattern, deviceId));
        assertFalse("ESN hex device id " + deviceId + " must not be a pseudo-ESN",
                    "80".equals(deviceId.substring(0, 2)));
    }

    private static void assertMeidFormat(String deviceId) {
        // MEID must NOT include the check digit.
        String meidPattern = "[0-9a-fA-F]{14}";
        assertTrue("MEID device id " + deviceId + " does not match pattern " + meidPattern,
                   Pattern.matches(meidPattern, deviceId));
    }

    private void assertSerialNumber() {
        assertNotNull("Non-telephony devices must have a Build.SERIAL number.",
                Build.SERIAL);
        assertTrue("Hardware id must be no longer than 20 characters.",
                Build.SERIAL.length() <= 20);
        assertTrue("Hardware id must be alphanumeric.",
                Pattern.matches("[0-9A-Za-z]+", Build.SERIAL));
    }

    private void assertMacAddress(String macAddress) {
        String macPattern = "([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}";
        assertTrue("MAC Address " + macAddress + " does not match pattern " + macPattern,
                Pattern.matches(macPattern, macAddress));
    }

    /** @return mac address which requires the WiFi system to be enabled */
    private String getWifiMacAddress() {
        WifiManager wifiManager = (WifiManager) getContext()
                .getSystemService(Context.WIFI_SERVICE);

        boolean enabled = wifiManager.isWifiEnabled();

        try {
            if (!enabled) {
                wifiManager.setWifiEnabled(true);
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getMacAddress();

        } finally {
            if (!enabled) {
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    private String getBluetoothMacAddress() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return "";
        }

        return adapter.getAddress();
    }

    private static final String ISO_COUNTRY_CODE_PATTERN = "[a-z]{2}";

    @Test
    public void testGetNetworkCountryIso() {
        String countryCode = mTelephonyManager.getNetworkCountryIso();
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            assertTrue("Country code '" + countryCode + "' did not match "
                    + ISO_COUNTRY_CODE_PATTERN,
                    Pattern.matches(ISO_COUNTRY_CODE_PATTERN, countryCode));
        } else {
            // Non-telephony may still have the property defined if it has a SIM.
        }
    }

    @Test
    public void testGetSimCountryIso() {
        String countryCode = mTelephonyManager.getSimCountryIso();
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            assertTrue("Country code '" + countryCode + "' did not match "
                    + ISO_COUNTRY_CODE_PATTERN,
                    Pattern.matches(ISO_COUNTRY_CODE_PATTERN, countryCode));
        } else {
            // Non-telephony may still have the property defined if it has a SIM.
        }
    }

    @Test
    public void testGetServiceState() throws InterruptedException {
        if (mCm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null) {
            Log.d(TAG, "Skipping test that requires ConnectivityManager.TYPE_MOBILE");
            return;
        }

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mListener = new PhoneStateListener() {
                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        synchronized (mLock) {
                            mServiceState = serviceState;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_SERVICE_STATE);
                Looper.loop();
            }
        });
        t.start();
        synchronized (mLock) {
            mLock.wait(TOLERANCE);
        }

        assertEquals(mServiceState, mTelephonyManager.getServiceState());
    }

    /**
     * Tests that the device properly reports either a valid IMEI if GSM or null.
     */
    @Test
    public void testGetImei() {
        String imei = mTelephonyManager.getImei();
        if (mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_ABSENT && imei == null) {
            // If no SIM card is present, IMEI can be null.
            return;
        }
        verifyImei(imei, mTelephonyManager.getDeviceId());
    }

    /**
     * Tests that the device properly reports either a valid IMEI if GSM or null.
     */
    @Test
    public void testGetImeiForSlotId() {
        // Test for slot id = 0.
        String imei = mTelephonyManager.getImei(0);
        verifyImei(imei, mTelephonyManager.getDeviceId(0));
        // Also verify that no exception is thrown for any slot id (including invalid ones)
        for (int i = -1; i <= mTelephonyManager.getPhoneCount(); i++) {
            mTelephonyManager.getImei(i);
        }
    }

    private void verifyImei(String imei, String deviceId) {
        int phoneType = mTelephonyManager.getPhoneType();
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM:
                assertGsmDeviceId(imei);
                assertEquals(imei, deviceId);
                break;
            case TelephonyManager.PHONE_TYPE_CDMA:
                // LTE device is using IMEI as device id
                if (mTelephonyManager.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    assertGsmDeviceId(imei);
                    assertEquals(imei, deviceId);
                    break;
                }
                // Fall through
            case TelephonyManager.PHONE_TYPE_NONE:
                if (imei != null) {
                    assertGsmDeviceId(imei);
                }
                // An IMEI is not required for PHONE_TYPE_NONE or non-LTE PHONE_TYPE_CDMA
                break;
            default:
                throw new IllegalArgumentException("Did you add a new phone type? " + phoneType);
        }
    }

    /**
     * Tests sendDialerSpecialCode API.
     * Expects a security exception since the caller does not have carrier privileges or is not the
     * current default dialer app.
     */
    @Test
    public void testSendDialerSpecialCode() {
        try {
            mTelephonyManager.sendDialerSpecialCode("4636");
            fail("Expected SecurityException. App does not have carrier privileges or is not the "
                    + "default dialer app");
        } catch (SecurityException expected) {

        }
    }

    private static Context getContext() {
        return InstrumentationRegistry.getContext();
    }
}
