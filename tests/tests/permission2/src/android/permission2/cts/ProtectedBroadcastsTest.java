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

package android.permission2.cts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

/**
 * Verify that applications can not send protected broadcasts.
 */
public class ProtectedBroadcastsTest extends AndroidTestCase {
    private static final String BROADCASTS[] = new String[] {
        Intent.ACTION_SCREEN_OFF,
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_USER_PRESENT,
        Intent.ACTION_TIME_TICK,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_PACKAGE_INSTALL,
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_RESTARTED,
        Intent.ACTION_PACKAGE_DATA_CLEARED,
        Intent.ACTION_UID_REMOVED,
        Intent.ACTION_CONFIGURATION_CHANGED,
        Intent.ACTION_BATTERY_CHANGED,
        Intent.ACTION_BATTERY_LOW,
        Intent.ACTION_BATTERY_OKAY,
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED,
        Intent.ACTION_SHUTDOWN,
        Intent.ACTION_DEVICE_STORAGE_LOW,
        Intent.ACTION_DEVICE_STORAGE_OK,
        Intent.ACTION_NEW_OUTGOING_CALL,
        Intent.ACTION_REBOOT,
        "android.intent.action.SERVICE_STATE",
        "android.intent.action.RADIO_TECHNOLOGY",
        "android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED",
        "android.intent.action.SIG_STR",
        "android.intent.action.ANY_DATA_STATE",
        "android.intent.action.DATA_CONNECTION_FAILED",
        "android.intent.action.SIM_STATE_CHANGED",
        "android.intent.action.NETWORK_SET_TIME",
        "android.intent.action.NETWORK_SET_TIMEZONE",
        "android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS",
        "android.intent.action.ACTION_MDN_STATE_CHANGED",
        "android.provider.Telephony.SPN_STRINGS_UPDATED"
    };

    /**
     * Verify that protected broadcast actions can't be sent.
     */
    public void testProcessOutgoingCall() {
        for (String action : BROADCASTS) {
            try {
                Intent intent = new Intent(action);
                getContext().sendBroadcast(intent);
                fail("expected security exception broadcasting action: " + action);
            } catch (SecurityException expected) {
                assertNotNull("security exception's error message.", expected.getMessage());
            }
        }
    }
}
