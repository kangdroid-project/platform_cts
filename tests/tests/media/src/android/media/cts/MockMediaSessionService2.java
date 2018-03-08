/*
 * Copyright 2018 The Android Open Source Project
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

package android.media.cts;

import static junit.framework.Assert.fail;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaSession2;
import android.media.MediaSession2.CommandGroup;
import android.media.MediaSession2.ControllerInfo;
import android.media.MediaSession2.SessionCallback;
import android.media.MediaSessionService2;
import android.media.cts.TestServiceRegistry.SessionCallbackProxy;
import android.media.cts.TestUtils.SyncHandler;

import java.util.concurrent.Executor;

/**
 * Mock implementation of {@link android.media.MediaSessionService2} for testing.
 */
public class MockMediaSessionService2 extends MediaSessionService2 {
    // Keep in sync with the AndroidManifest.xml
    public static final String ID = "TestSession";

    private static final String DEFAULT_MEDIA_NOTIFICATION_CHANNEL_ID = "media_session_service";
    private static final int DEFAULT_MEDIA_NOTIFICATION_ID = 1001;

    private NotificationChannel mDefaultNotificationChannel;
    private MediaSession2 mSession;
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        TestServiceRegistry.getInstance().setServiceInstance(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public MediaSession2 onCreateSession(String sessionId) {
        final MockPlayer player = new MockPlayer(1);
        final SyncHandler handler = (SyncHandler) TestServiceRegistry.getInstance().getHandler();
        final Executor executor = (runnable) -> handler.post(runnable);
        SessionCallbackProxy sessionCallbackProxy = TestServiceRegistry.getInstance()
                .getSessionCallbackProxy();
        if (sessionCallbackProxy == null) {
            // Ensures non-null
            sessionCallbackProxy = new SessionCallbackProxy(this) {};
        }
        TestSessionServiceCallback callback =
                new TestSessionServiceCallback(sessionCallbackProxy);
        mSession = new MediaSession2.Builder(this)
                .setPlayer(player)
                .setSessionCallback(executor, callback)
                .setId(sessionId).build();
        return mSession;
    }

    @Override
    public void onDestroy() {
        TestServiceRegistry.getInstance().cleanUp();
        super.onDestroy();
    }

    @Override
    public MediaNotification onUpdateNotification() {
        if (mDefaultNotificationChannel == null) {
            mDefaultNotificationChannel = new NotificationChannel(
                    DEFAULT_MEDIA_NOTIFICATION_CHANNEL_ID,
                    DEFAULT_MEDIA_NOTIFICATION_CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mDefaultNotificationChannel);
        }
        Notification notification = new Notification.Builder(
                this, DEFAULT_MEDIA_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getPackageName())
                .setContentText("Dummt test notification")
                .setSmallIcon(android.R.drawable.sym_def_app_icon).build();
        return new MediaNotification(this, DEFAULT_MEDIA_NOTIFICATION_ID, notification);
    }

    private class TestSessionServiceCallback extends SessionCallback {
        private final SessionCallbackProxy mCallbackProxy;

        public TestSessionServiceCallback(SessionCallbackProxy callbackProxy) {
            super(MockMediaSessionService2.this);
            mCallbackProxy = callbackProxy;
        }

        @Override
        public CommandGroup onConnect(MediaSession2 session,
                ControllerInfo controller) {
            return mCallbackProxy.onConnect(controller);
        }
    }
}
