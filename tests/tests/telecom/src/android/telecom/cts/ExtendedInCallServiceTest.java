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
 */

package android.telecom.cts;

import static android.telecom.cts.TestUtils.*;

import android.content.Context;
import android.telecom.CallAudioState;
import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.ConnectionService;
import android.telecom.InCallService;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

import java.util.List;

/**
 * Extended suite of tests that use {@link CtsConnectionService} and {@link MockInCallService} to
 * verify the functionality of the Telecom service.
 */
public class ExtendedInCallServiceTest extends BaseTelecomTestWithMockServices {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mShouldTestTelecom) {
            setupConnectionService(null, FLAG_REGISTER | FLAG_ENABLE);
        }
    }

    public void testAddNewOutgoingCallAndThenDisconnect() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();
        inCallService.disconnectLastCall();

        assertNumCalls(inCallService, 0);
    }

    public void testMuteAndUnmutePhone() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_DIALING);

        assertMuteState(connection, false);

        // Explicitly call super implementation to enable detection of CTS coverage
        ((InCallService) inCallService).setMuted(true);

        assertMuteState(connection, true);
        assertMuteState(inCallService, true);

        inCallService.setMuted(false);
        assertMuteState(connection, false);
        assertMuteState(inCallService, false);
    }

    public void testSwitchAudioRoutes() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_DIALING);

        final int currentInvokeCount = mOnCallAudioStateChangedCounter.getInvokeCount();

        // Only test speaker and earpiece modes because the other modes are dependent on having
        // a bluetooth headset or wired headset connected.

        // Explicitly call super implementation to enable detection of CTS coverage
        ((InCallService) inCallService).setAudioRoute(CallAudioState.ROUTE_SPEAKER);
        mOnCallAudioStateChangedCounter.waitForCount(currentInvokeCount + 1,
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);
        assertAudioRoute(connection, CallAudioState.ROUTE_SPEAKER);
        assertAudioRoute(inCallService, CallAudioState.ROUTE_SPEAKER);

        inCallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE);
        mOnCallAudioStateChangedCounter.waitForCount(currentInvokeCount + 2,
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);
        assertAudioRoute(connection, CallAudioState.ROUTE_EARPIECE);
        assertAudioRoute(inCallService, CallAudioState.ROUTE_EARPIECE);
    }

    /**
     * Tests that DTMF Tones are sent from the {@link InCallService} to the
     * {@link ConnectionService} in the correct sequence.
     *
     * @see {@link Call#playDtmfTone(char)}
     * @see {@link Call#stopDtmfTone()}
     */
    public void testPlayAndStopDtmfTones() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_DIALING);

        assertDtmfString(connection, "");

        call.playDtmfTone('1');
        assertDtmfString(connection, "1");

        call.playDtmfTone('2');
        assertDtmfString(connection, "12");

        call.stopDtmfTone();
        assertDtmfString(connection, "12.");

        call.playDtmfTone('3');
        call.playDtmfTone('4');
        call.playDtmfTone('5');
        assertDtmfString(connection, "12.345");

        call.stopDtmfTone();
        assertDtmfString(connection, "12.345.");
    }

    public void testHoldAndUnholdCall() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_DIALING);

        connection.setActive();

        assertCallState(call, Call.STATE_ACTIVE);

        call.hold();
        assertCallState(call, Call.STATE_HOLDING);
        assertEquals(Connection.STATE_HOLDING, connection.getState());

        call.unhold();
        assertCallState(call, Call.STATE_ACTIVE);
        assertEquals(Connection.STATE_ACTIVE, connection.getState());
    }

    public void testAnswerIncomingCallAudioOnly() {
        if (!mShouldTestTelecom) {
            return;
        }

        addAndVerifyNewIncomingCall(getTestNumber(), null);
        final MockConnection connection = verifyConnectionForIncomingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_RINGING);
        assertConnectionState(connection, Connection.STATE_RINGING);

        call.answer(VideoProfile.STATE_AUDIO_ONLY);

        assertCallState(call, Call.STATE_ACTIVE);
        assertConnectionState(connection, Connection.STATE_ACTIVE);
    }

    public void testAnswerIncomingCallAsVideo_SendsCorrectVideoState() {
        if (!mShouldTestTelecom) {
            return;
        }

        addAndVerifyNewIncomingCall(getTestNumber(), null);
        final MockConnection connection = verifyConnectionForIncomingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_RINGING);
        assertConnectionState(connection, Connection.STATE_RINGING);

        call.answer(VideoProfile.STATE_BIDIRECTIONAL);

        assertCallState(call, Call.STATE_ACTIVE);
        assertConnectionState(connection, Connection.STATE_ACTIVE);
        assertEquals("Connection did not receive VideoState for answered call",
                VideoProfile.STATE_BIDIRECTIONAL, connection.videoState);
    }

    public void testRejectIncomingCall() {
        if (!mShouldTestTelecom) {
            return;
        }

        addAndVerifyNewIncomingCall(getTestNumber(), null);
        final MockConnection connection = verifyConnectionForIncomingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_RINGING);
        assertConnectionState(connection, Connection.STATE_RINGING);

        call.reject(false, null);

        assertCallState(call, Call.STATE_DISCONNECTED);
        assertConnectionState(connection, Connection.STATE_DISCONNECTED);
    }

    public void testCanAddCall_CannotAddForExistingDialingCall() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_DIALING);

        assertCanAddCall(inCallService, false,
                "Should not be able to add call with existing dialing call");
    }

    public void testCanAddCall_CanAddForExistingActiveCall() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_DIALING);

        connection.setActive();

        assertCallState(call, Call.STATE_ACTIVE);

        assertCanAddCall(inCallService, true,
                "Should be able to add call with only one active call");
    }

    public void testCanAddCall_CannotAddIfTooManyCalls() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection1 = verifyConnectionForOutgoingCall(0);
        final MockInCallService inCallService = mInCallCallbacks.getService();
        final Call call1 = inCallService.getLastCall();
        assertCallState(call1, Call.STATE_DIALING);

        connection1.setActive();

        assertCallState(call1, Call.STATE_ACTIVE);

        placeAndVerifyCall();
        final MockConnection connection2 = verifyConnectionForOutgoingCall(1);

        final Call call2 = inCallService.getLastCall();
        assertCallState(call2, Call.STATE_DIALING);
        connection2.setActive();
        assertCallState(call2, Call.STATE_ACTIVE);

        assertEquals("InCallService should have 2 calls", 2, inCallService.getCallCount());

        assertCanAddCall(inCallService, false,
                "Should not be able to add call with two calls already present");

        call1.hold();
        assertCallState(call1, Call.STATE_HOLDING);

        assertCanAddCall(inCallService, false,
                "Should not be able to add call with two calls already present");
    }

    public void testOnBringToForeground() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        verifyConnectionForOutgoingCall();

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_DIALING);

        assertEquals(0, mOnBringToForegroundCounter.getInvokeCount());

        final TelecomManager tm =
            (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);

        tm.showInCallScreen(false);

        mOnBringToForegroundCounter.waitForCount(1, WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);

        assertFalse((Boolean) mOnBringToForegroundCounter.getArgs(0)[0]);

        tm.showInCallScreen(true);

        mOnBringToForegroundCounter.waitForCount(2, WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);

        assertTrue((Boolean) mOnBringToForegroundCounter.getArgs(1)[0]);
    }

    public void testOnPostDialWaitAndContinue() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection = verifyConnectionForOutgoingCall();
        final MockInCallService inCallService = mInCallCallbacks.getService();
        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_DIALING);

        connection.setActive();
        assertCallState(call, Call.STATE_ACTIVE);

        final String postDialString = "12345";
        ((Connection) connection).setPostDialWait(postDialString);
        mOnPostDialWaitCounter.waitForCount(1, WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);

        assertEquals(postDialString, mOnPostDialWaitCounter.getArgs(0)[1]);
        assertEquals(postDialString, call.getRemainingPostDialSequence());

        final InvokeCounter counter = connection.getInvokeCounter(MockConnection.ON_POST_DIAL_WAIT);

        call.postDialContinue(true);
        counter.waitForCount(1);
        assertTrue((Boolean) counter.getArgs(0)[0]);

        call.postDialContinue(false);
        counter.waitForCount(2);
        assertFalse((Boolean) counter.getArgs(1)[0]);
    }

    public void testOnCannedTextResponsesLoaded() {
        if (!mShouldTestTelecom) {
            return;
        }

        addAndVerifyNewIncomingCall(getTestNumber(), null);
        verifyConnectionForIncomingCall();
        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_RINGING);

        // We can't do much to enforce the number and type of responses that are preloaded on
        // device, so the best we can do is to make sure that the call back is called and
        // that the returned list is non-empty.

        // This test should also verify that the callback is called as well, but unfortunately it
        // is never called right now (b/22952515).
        // mOnCannedTextResponsesLoadedCounter.waitForCount(1);

        assertGetCannedTextResponsesNotEmpty(call);
    }

    public void testGetCalls() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        final MockConnection connection1 = verifyConnectionForOutgoingCall(0);
        final MockInCallService inCallService = mInCallCallbacks.getService();
        final Call call1 = inCallService.getLastCall();
        assertCallState(call1, Call.STATE_DIALING);

        connection1.setActive();

        assertCallState(call1, Call.STATE_ACTIVE);

        List<Call> calls = inCallService.getCalls();
        assertEquals("InCallService.getCalls() should return list with 1 call.", 1, calls.size());
        assertEquals(call1, calls.get(0));

        addAndVerifyNewIncomingCall(getTestNumber(), null);
        verifyConnectionForIncomingCall();

        final Call call2 = inCallService.getLastCall();
        calls = inCallService.getCalls();
        assertEquals("InCallService.getCalls() should return list with 2 calls.", 2, calls.size());
        assertEquals(call1, calls.get(0));
        assertEquals(call2, calls.get(1));
    }

    private void assertGetCannedTextResponsesNotEmpty(final Call call) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return true;
                    }

                    @Override
                    public Object actual() {
                        return call.getCannedTextResponses() != null
                                && !call.getCannedTextResponses().isEmpty();
                    }

                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Call.getCannedTextResponses should not be empty");
    }

    private void assertCanAddCall(final InCallService inCallService, final boolean canAddCall,
            String message) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return canAddCall;
                    }

                    @Override
                    public Object actual() {
                        return inCallService.canAddCall();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                message
        );
    }
}
