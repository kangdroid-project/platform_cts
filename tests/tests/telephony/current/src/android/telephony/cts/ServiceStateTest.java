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
package android.telephony.cts;

import static android.telephony.ServiceState.DUPLEX_MODE_FDD;
import static android.telephony.ServiceState.DUPLEX_MODE_TDD;
import static android.telephony.ServiceState.DUPLEX_MODE_UNKNOWN;
import static android.telephony.ServiceState.STATE_OUT_OF_SERVICE;
import static android.telephony.ServiceState.STATE_POWER_OFF;
import static android.telephony.ServiceState.ROAMING_TYPE_DOMESTIC;
import static android.telephony.ServiceState.ROAMING_TYPE_NOT_ROAMING;

import static org.junit.Assert.assertNotEquals;

import android.os.Parcel;
import android.telephony.AccessNetworkConstants;
import android.telephony.LteVopsSupportInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;

import java.util.List;

public class ServiceStateTest extends AndroidTestCase {
    private static final String OPERATOR_ALPHA_LONG = "CtsOperatorLong";
    private static final String OPERATOR_ALPHA_SHORT = "CtsOp";
    private static final String OPERATOR_NUMERIC = "02871";
    private static final int SYSTEM_ID = 123;
    private static final int NETWORK_ID = 456;
    private static final int CHANNEL_NUMBER_BAND_66 = 66436;
    private static final int CHANNEL_NUMBER_BAND_33 = 36000;
    private static final int[] CELL_BANDWIDTH = {1, 2, 3};

    public void testDescribeContents() {
        ServiceState serviceState = new ServiceState();
        assertEquals(0, serviceState.describeContents());
    }

    public void testSetStateOff() {
        ServiceState serviceState = new ServiceState();
        serviceState.setStateOff();
        assertEquals(STATE_POWER_OFF, serviceState.getState());
        checkOffStatus(serviceState);
    }

    public void testSetStateOutOfService() {
        ServiceState serviceState = new ServiceState();
        serviceState.setStateOutOfService();
        assertEquals(STATE_OUT_OF_SERVICE, serviceState.getState());
        checkOffStatus(serviceState);
    }

    public void testSetState() {
        ServiceState serviceState = new ServiceState();
        serviceState.setState(ServiceState.STATE_IN_SERVICE);
        assertEquals(ServiceState.STATE_IN_SERVICE, serviceState.getState());
    }

    public void testGetRoaming() {
        ServiceState serviceState = new ServiceState();
        serviceState.setRoaming(false);
        assertFalse(serviceState.getRoaming());
        serviceState.setRoaming(true);
        assertTrue(serviceState.getRoaming());
    }

    public void testGetIsManualSelection() {
        ServiceState serviceState = new ServiceState();
        serviceState.setIsManualSelection(false);
        assertFalse(serviceState.getIsManualSelection());
        serviceState.setIsManualSelection(true);
        assertTrue(serviceState.getIsManualSelection());
    }

    public void testGetOperator() {
        ServiceState serviceState = new ServiceState();
        serviceState.setOperatorName(OPERATOR_ALPHA_LONG, OPERATOR_ALPHA_SHORT, OPERATOR_NUMERIC);
        assertEquals(OPERATOR_ALPHA_LONG, serviceState.getOperatorAlphaLong());
        assertEquals(OPERATOR_ALPHA_SHORT, serviceState.getOperatorAlphaShort());
        assertEquals(OPERATOR_NUMERIC, serviceState.getOperatorNumeric());
    }

    public void testGetCdma() {
        ServiceState serviceState = new ServiceState();
        serviceState.setCdmaSystemAndNetworkId(SYSTEM_ID, NETWORK_ID);
        assertEquals(SYSTEM_ID, serviceState.getCdmaSystemId());
        assertEquals(NETWORK_ID, serviceState.getCdmaNetworkId());
    }

    public void testGetChannelNumber() {
        ServiceState serviceState = new ServiceState();
        serviceState.setChannelNumber(CHANNEL_NUMBER_BAND_66);
        assertEquals(CHANNEL_NUMBER_BAND_66, serviceState.getChannelNumber());
    }

    public void testGetCellBandwidths() {
        ServiceState serviceState = new ServiceState();
        serviceState.setCellBandwidths(CELL_BANDWIDTH);
        assertEquals(CELL_BANDWIDTH, serviceState.getCellBandwidths());
    }

    public void testGetDuplexMode() {
        ServiceState serviceState = new ServiceState();
        NetworkRegistrationInfo nri = new NetworkRegistrationInfo.Builder()
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_GSM)
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .build();
        serviceState.addNetworkRegistrationInfo(nri);
        assertEquals(DUPLEX_MODE_UNKNOWN, serviceState.getDuplexMode());

        nri = new NetworkRegistrationInfo.Builder()
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_LTE)
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .build();
        serviceState.addNetworkRegistrationInfo(nri);

        assertEquals(DUPLEX_MODE_FDD, serviceState.getDuplexMode());

        serviceState.setChannelNumber(CHANNEL_NUMBER_BAND_33);
        assertEquals(DUPLEX_MODE_TDD, serviceState.getDuplexMode());
    }

    public void testToString() {
        ServiceState serviceState = new ServiceState();
        assertNotNull(serviceState.toString());
    }

    public void testCopyConstructor() {
        ServiceState serviceState = getServiceStateWithOperatorName("name", "numeric");
        assertEquals(serviceState, new ServiceState(serviceState));
    }

    public void testParcelConstructor() {
        ServiceState serviceState = getServiceStateWithOperatorName("name", "numeric");
        Parcel stateParcel = Parcel.obtain();
        serviceState.writeToParcel(stateParcel, 0);
        stateParcel.setDataPosition(0);
        assertEquals(serviceState, new ServiceState(stateParcel));
    }

    public void testHashCode() {
        ServiceState serviceStateA = getServiceStateWithOperatorName("a", "b");
        ServiceState serviceStateB = getServiceStateWithOperatorName("a", "b");
        ServiceState serviceStateC = getServiceStateWithOperatorName("c", "d");

        // well-written hashCode functions shouldn't produce "0"
        assertNotEquals(serviceStateA.hashCode(), 0);
        assertNotEquals(serviceStateB.hashCode(), 0);

        // If serviceStateA.equals(serviceStateB), then serviceStateA.hashCode()
        // should equal serviceStateB.hashCode().
        assertEquals(serviceStateA.hashCode(), serviceStateB.hashCode());
        assertEquals(serviceStateA, serviceStateB);

        // If serviceStateA.hashCode() != serviceStateC.hashCode(), then
        // serviceStateA.equals(serviceStateB) should be false.
        assertNotEquals(serviceStateA.hashCode(), serviceStateC.hashCode());
        assertNotEquals(serviceStateA, serviceStateC);
    }

    public void testRoaming() {
        ServiceState notRoaming = getServiceStateWithRoamingTypes(ROAMING_TYPE_NOT_ROAMING,
                                                                    ROAMING_TYPE_NOT_ROAMING);
        ServiceState dataRoaming = getServiceStateWithRoamingTypes(ROAMING_TYPE_DOMESTIC,
                                                                    ROAMING_TYPE_NOT_ROAMING);
        ServiceState voiceRoaming = getServiceStateWithRoamingTypes(ROAMING_TYPE_NOT_ROAMING,
                                                                    ROAMING_TYPE_DOMESTIC);
        ServiceState dataVoiceRoaming = getServiceStateWithRoamingTypes(ROAMING_TYPE_NOT_ROAMING,
                                                                    ROAMING_TYPE_DOMESTIC);

        assertFalse(notRoaming.getRoaming());
        assertTrue(dataRoaming.getRoaming());
        assertTrue(voiceRoaming.getRoaming());
        assertTrue(dataVoiceRoaming.getRoaming());
    }

    public void testIsManualSelection() {
        ServiceState serviceState = new ServiceState();
        serviceState.setIsManualSelection(false);
        assertFalse(serviceState.getIsManualSelection());
        serviceState.setIsManualSelection(true);
        assertTrue(serviceState.getIsManualSelection());
    }

    private ServiceState getServiceStateWithOperatorName(String name, String numeric) {
        ServiceState serviceState = new ServiceState();
        serviceState.setOperatorName(name, name, numeric);
        return serviceState;
    }

    private ServiceState getServiceStateWithRoamingTypes(int dataRoaming, int voiceRoaming) {
        ServiceState serviceState = new ServiceState();
        serviceState.setDataRoamingType(dataRoaming);
        serviceState.setVoiceRoamingType(voiceRoaming);
        return serviceState;
    }

    /**
     * Check the ServiceState fields in STATE_OUT_OF_SERVICE or STATE_POWER_OFF
     */
    private void checkOffStatus(ServiceState s) {
        assertFalse(s.getRoaming());
        assertNull(s.getOperatorAlphaLong());
        assertNull(s.getOperatorAlphaShort());
        assertNull(s.getOperatorNumeric());
        assertFalse(s.getIsManualSelection());
    }

    public void testGetRegistrationInfo() {
        ServiceState serviceState = new ServiceState();
        NetworkRegistrationInfo nri = new NetworkRegistrationInfo.Builder()
                .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_LTE)
                .setRegistrationState(NetworkRegistrationInfo.REGISTRATION_STATE_HOME)
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .build();
        serviceState.addNetworkRegistrationInfo(nri);

        assertEquals(nri, serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN));
        assertNull(serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WLAN));
        assertNull(serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_CS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN));
        assertNull(serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_CS, AccessNetworkConstants.TRANSPORT_TYPE_WLAN));

        List<NetworkRegistrationInfo> nris = serviceState.getNetworkRegistrationInfoList();
        assertEquals(1, nris.size());
        assertEquals(nri, nris.get(0));

        nri = new NetworkRegistrationInfo.Builder()
                .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_IWLAN)
                .setRegistrationState(NetworkRegistrationInfo.REGISTRATION_STATE_HOME)
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .build();
        serviceState.addNetworkRegistrationInfo(nri);
        assertEquals(nri, serviceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WLAN));

        nris = serviceState.getNetworkRegistrationInfoListForDomain(
                NetworkRegistrationInfo.DOMAIN_PS);
        assertEquals(2, nris.size());
        assertEquals(nri, nris.get(1));

        nris = serviceState.getNetworkRegistrationInfoList();
        assertEquals(2, nris.size());

        nris = serviceState.getNetworkRegistrationInfoListForTransportType(
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
        assertEquals(1, nris.size());
        assertEquals(nri, nris.get(0));
    }

    public void testLteVopsSupportInfo() {
        LteVopsSupportInfo lteVopsSupportInfo =
                new LteVopsSupportInfo(LteVopsSupportInfo.LTE_STATUS_NOT_AVAILABLE,
                        LteVopsSupportInfo.LTE_STATUS_NOT_AVAILABLE);

        NetworkRegistrationInfo wwanDataRegState = new NetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                0, 0, 0, true, null, null, 0, false, false, false, lteVopsSupportInfo, false);

        ServiceState ss = new ServiceState();

        ss.addNetworkRegistrationInfo(wwanDataRegState);

        assertEquals(ss.getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN), wwanDataRegState);

        lteVopsSupportInfo =
                new LteVopsSupportInfo(LteVopsSupportInfo.LTE_STATUS_SUPPORTED,
                        LteVopsSupportInfo.LTE_STATUS_NOT_SUPPORTED);

        wwanDataRegState = new NetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                0, 0, 0, true, null, null, 0, false, false, false, lteVopsSupportInfo, false);
        ss.addNetworkRegistrationInfo(wwanDataRegState);
        assertEquals(ss.getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN), wwanDataRegState);
        assertEquals(ss.getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN).getDataSpecificInfo().getLteVopsSupportInfo(),
            lteVopsSupportInfo);
    }
}
