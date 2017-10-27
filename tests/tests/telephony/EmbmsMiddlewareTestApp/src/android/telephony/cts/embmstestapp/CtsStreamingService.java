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

package android.telephony.cts.embmstestapp;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.mbms.MbmsErrors;
import android.telephony.mbms.MbmsStreamingSessionCallback;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.telephony.mbms.vendor.MbmsStreamingServiceBase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CtsStreamingService extends Service {
    private static final Set<String> ALLOWED_PACKAGES = new HashSet<String>() {{
        add("android.telephony.cts");
    }};

    public static final String CONTROL_INTERFACE_ACTION =
            "android.telephony.cts.embmstestapp.ACTION_CONTROL_MIDDLEWARE";

    private static final String TAG = "EmbmsTestStreaming";

    private static final StreamingServiceInfo STREAMING_SERVICE_INFO;
    static {
        String id = "StreamingServiceId";
        Map<Locale, String> localeDict = new HashMap<Locale, String>() {{
            put(Locale.US, "Entertainment Source 1");
            put(Locale.CANADA, "Entertainment Source 1, eh?");
        }};
        List<Locale> locales = new ArrayList<Locale>() {{
            add(Locale.CANADA);
            add(Locale.US);
        }};
        STREAMING_SERVICE_INFO = new StreamingServiceInfo(localeDict, "class1", locales,
                id, new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000));
    }

    private static final int SEND_STREAMING_SERVICES_LIST = 1;

    private MbmsStreamingSessionCallback mAppCallback;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Handler.Callback mWorkerCallback = (msg) -> {
        switch (msg.what) {
            case SEND_STREAMING_SERVICES_LIST:
                List<StreamingServiceInfo> services = (List<StreamingServiceInfo>) msg.obj;
                if (mAppCallback!= null) {
                    mAppCallback.onStreamingServicesUpdated(services);
                }
                break;
        }
        return true;
    };
    private List<List> mReceivedCalls = new LinkedList<>();

    private final MbmsStreamingServiceBase mStreamingServiceImpl = new MbmsStreamingServiceBase() {
        @Override
        public int initialize(MbmsStreamingSessionCallback callback, int subId) {
            mReceivedCalls.add(Arrays.asList("initialize", subId));

            int packageUid = Binder.getCallingUid();
            String[] packageNames = getPackageManager().getPackagesForUid(packageUid);
            if (packageNames == null) {
                return MbmsErrors.InitializationErrors.ERROR_APP_PERMISSIONS_NOT_GRANTED;
            }
            boolean isUidAllowed = Arrays.stream(packageNames).anyMatch(ALLOWED_PACKAGES::contains);
            if (!isUidAllowed) {
                return MbmsErrors.InitializationErrors.ERROR_APP_PERMISSIONS_NOT_GRANTED;
            }

            mHandler.post(() -> {
                if (mAppCallback == null) {
                    mAppCallback = callback;
                } else {
                    callback.onError(
                            MbmsErrors.InitializationErrors.ERROR_DUPLICATE_INITIALIZE, "");
                    return;
                }
                callback.onMiddlewareReady();
            });
            return MbmsErrors.SUCCESS;
        }

        @Override
        public int requestUpdateStreamingServices(int subscriptionId, List<String> serviceClasses) {
            mReceivedCalls.add(Arrays.asList("requestUpdateStreamingServices", subscriptionId,
                    serviceClasses));
            List<StreamingServiceInfo> serviceInfos =
                    Collections.singletonList(STREAMING_SERVICE_INFO);

            mHandler.removeMessages(SEND_STREAMING_SERVICES_LIST);
            mHandler.sendMessage(
                    mHandler.obtainMessage(SEND_STREAMING_SERVICES_LIST, serviceInfos));
            return MbmsErrors.SUCCESS;
        }

        @Override
        public int startStreaming(int subscriptionId, String serviceId,
                StreamingServiceCallback callback) {
            // TODO
            return MbmsErrors.SUCCESS;
        }

        @Override
        public Uri getPlaybackUri(int subscriptionId, String serviceId) {
            // TODO
            return null;
        }

        @Override
        public void stopStreaming(int subscriptionId, String serviceId) {
            // TODO
        }

        @Override
        public void dispose(int subscriptionId) {
            // TODO
        }

        @Override
        public void onAppCallbackDied(int uid, int subscriptionId) {
            mAppCallback = null;
        }
    };

    private final IBinder mControlInterface = new ICtsMiddlewareControl.Stub() {
        @Override
        public void reset() throws RemoteException {
            mReceivedCalls.clear();
            mHandler.removeCallbacksAndMessages(null);
            mAppCallback = null;
        }

        @Override
        public List getStreamingSessionCalls() throws RemoteException {
            return mReceivedCalls;
        }
    };

    @Override
    public void onDestroy() {
        super.onCreate();
        mHandlerThread.quitSafely();
        logd("CtsStreamingService onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (CONTROL_INTERFACE_ACTION.equals(intent.getAction())) {
            logd("CtsStreamingService control interface bind");
            return mControlInterface;
        }

        logd("CtsStreamingService onBind");
        if (mHandlerThread != null && mHandlerThread.isAlive()) {
            return mStreamingServiceImpl;
        }

        mHandlerThread = new HandlerThread("CtsStreamingServiceWorker");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), mWorkerCallback);
        return mStreamingServiceImpl;
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    private void checkInitialized() {
        if (mAppCallback == null) {
            throw new IllegalStateException("Not yet initialized");
        }
    }
}
