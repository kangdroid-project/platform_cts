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
 * limitations under the License
 */

package android.telephony.cts.embmstestapp;

import android.os.Parcel;

interface ICtsGroupCallMiddlewareControl {
    // Resets the state of the CTS middleware
    void reset();
    // Get a list of calls made to the middleware binder.
    // Looks like List<List<Object>>, where the first Object is always a String corresponding to
    // the method name.
    List getGroupCallSessionCalls();
    // Force all methods that can return an error to return this error.
    void forceErrorCode(int error);
    // Fire the error callback on the current active call
    void fireErrorOnGroupCall(int errorCode, String message);
    // Fire the error callback on the group call session
    void fireErrorOnSession(int errorCode, String message);
    // The following fire callbacks on the active group call, using the provided arguments
    void fireGroupCallStateChanged(int state, int reason);
    void fireBroadcastSignalStrengthUpdated(int signalStrength);
    // The following fire callbacks on the group call session.
    void fireAvailableSaisUpdated(in List currentSais, in List availableSais);
    void fireServiceInterfaceAvailable(String interfaceName, int index);
}