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
package android.autofillservice.cts;

import static android.autofillservice.cts.Helper.assertTextIsSanitized;
import static android.autofillservice.cts.Helper.getContext;
import static android.autofillservice.cts.Helper.hasAutofillFeature;
import static android.autofillservice.cts.InstrumentedAutoFillServiceCompatMode.SERVICE_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.app.assist.AssistStructure.ViewNode;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.After;

/**
 * Test case for an activity containing virtual children but using the A11Y compat mode to implement
 * the Autofill APIs.
 */
public class VirtualContainerActivityCompatModeTest extends VirtualContainerActivityTest {
    private static final Context sContext = InstrumentationRegistry.getContext();

    public VirtualContainerActivityCompatModeTest() {
        super(true);
    }

    @After
    public void resetCompatMode() {
        sContext.getApplicationContext().setAutofillCompatibilityEnabled(false);
    }

    @Override
    protected void preActivityCreated() {
        sContext.getApplicationContext().setAutofillCompatibilityEnabled(true);
    }

    @Override
    protected void postActivityLaunched(VirtualContainerActivity activity) {
        // Set our own compat mode as well..
        // NOTE: because it's set here, we don't need to whitelist the package on Settings.
        activity.mCustomView.setCompatMode(true);
    }

    @Override
    protected void enableService() {
        Helper.enableAutofillService(getContext(), SERVICE_NAME);
        InstrumentedAutoFillServiceCompatMode.setIgnoreUnexpectedRequests(false);
    }

    @Override
    protected void disableService() {
        if (!hasAutofillFeature()) return;

        Helper.disableAutofillService(getContext(), SERVICE_NAME);
        InstrumentedAutoFillServiceCompatMode.setIgnoreUnexpectedRequests(true);
    }

    @Override
    protected void assertUrlBarIsSanitized(ViewNode urlBar) {
        assertTextIsSanitized(urlBar);
        assertThat(urlBar.getWebDomain()).isEqualTo("dev.null");
        assertThat(urlBar.getWebScheme()).isEqualTo("ftp");
    }
}
