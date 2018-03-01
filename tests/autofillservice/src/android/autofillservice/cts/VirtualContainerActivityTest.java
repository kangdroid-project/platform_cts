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
 * limitations under the License.
 */
package android.autofillservice.cts;

import static android.autofillservice.cts.CannedFillResponse.NO_RESPONSE;
import static android.autofillservice.cts.Helper.ID_PASSWORD;
import static android.autofillservice.cts.Helper.ID_PASSWORD_LABEL;
import static android.autofillservice.cts.Helper.ID_USERNAME;
import static android.autofillservice.cts.Helper.ID_USERNAME_LABEL;
import static android.autofillservice.cts.Helper.assertTextAndValue;
import static android.autofillservice.cts.Helper.assertTextIsSanitized;
import static android.autofillservice.cts.Helper.assertTextOnly;
import static android.autofillservice.cts.Helper.dumpStructure;
import static android.autofillservice.cts.Helper.findNodeByResourceId;
import static android.autofillservice.cts.VirtualContainerView.ID_URL_BAR;
import static android.autofillservice.cts.VirtualContainerView.LABEL_CLASS;
import static android.autofillservice.cts.VirtualContainerView.TEXT_CLASS;
import static android.service.autofill.SaveInfo.SAVE_DATA_TYPE_PASSWORD;
import static android.service.autofill.SaveInfo.SAVE_DATA_TYPE_USERNAME;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.assist.AssistStructure.ViewNode;
import android.autofillservice.cts.CannedFillResponse.CannedDataset;
import android.autofillservice.cts.InstrumentedAutoFillService.FillRequest;
import android.autofillservice.cts.InstrumentedAutoFillService.SaveRequest;
import android.autofillservice.cts.VirtualContainerView.Line;
import android.content.ComponentName;
import android.graphics.Rect;
import android.service.autofill.SaveInfo;
import android.support.test.uiautomator.UiObject2;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

/**
 * Test case for an activity containing virtual children, either using the explicit Autofill APIs
 * or Compat mode.
 */
public class VirtualContainerActivityTest extends AutoFillServiceTestCase {

    @Rule
    public final AutofillActivityTestRule<VirtualContainerActivity> mActivityRule =
            new AutofillActivityTestRule<VirtualContainerActivity>(VirtualContainerActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            preActivityCreated();
        }
    };

    private final boolean mCompatMode;
    private VirtualContainerActivity mActivity;

    public VirtualContainerActivityTest() {
        this(false);
    }

    protected VirtualContainerActivityTest(boolean compatMode) {
        mCompatMode = compatMode;
    }

    @Before
    public void setActivity() {
        mActivity = mActivityRule.getActivity();
        postActivityLaunched(mActivity);
    }

    /**
     * Hook for subclass to customize test before activity is created.
     */
    protected void preActivityCreated() {}

    /**
     * Hook for subclass to customize activity after it's launched.
     */
    protected void postActivityLaunched(
            @SuppressWarnings("unused") VirtualContainerActivity activity) {
    }

    @Test
    public void testAutofillSync() throws Exception {
        autofillTest(true);
    }

    @Test
    public void testAutofillAsync() throws Exception {
        if (mCompatMode) return;

        autofillTest(false);
    }

    /**
     * Focus to username and expect window event
     */
    void focusToUsername() throws TimeoutException {
        mUiBot.waitForWindowChange(() -> mActivity.mUsername.changeFocus(true),
                Timeouts.UI_TIMEOUT.getMaxValue());
    }

    /**
     * Focus to username and expect no autofill window event
     */
    void focusToUsernameExpectNoWindowEvent() throws Throwable {
        // TODO should use waitForWindowChange() if we can filter out event of app Activity itself.
        mActivityRule.runOnUiThread(() -> mActivity.mUsername.changeFocus(true));
    }

    /**
     * Focus to password and expect window event
     */
    void focusToPassword() throws TimeoutException {
        mUiBot.waitForWindowChange(() -> mActivity.mPassword.changeFocus(true),
                Timeouts.UI_TIMEOUT.getMaxValue());
    }

    /**
     * Tests autofilling the virtual views, using the sync / async version of ViewStructure.addChild
     */
    private void autofillTest(boolean sync) throws Exception {
        if (sRanAlready) return;
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedDataset.Builder()
                .setField(ID_USERNAME, "dude")
                .setField(ID_PASSWORD, "sweet")
                .setPresentation(createPresentation("The Dude"))
                .build());
        mActivity.expectAutoFill("dude", "sweet");
        mActivity.mCustomView.setSync(sync);

        // Trigger auto-fill.
        focusToUsername();
        assertDatasetShown(mActivity.mUsername, "The Dude");

        // Play around with focus to make sure picker is properly drawn.
        focusToPassword();
        assertDatasetShown(mActivity.mPassword, "The Dude");
        focusToUsername();
        assertDatasetShown(mActivity.mUsername, "The Dude");

        // Make sure input was sanitized.
        final FillRequest request = sReplier.getNextFillRequest();
        final ViewNode urlBar = findNodeByResourceId(request.structure, ID_URL_BAR);
        final ViewNode usernameLabel = findNodeByResourceId(request.structure, ID_USERNAME_LABEL);
        final ViewNode username = findNodeByResourceId(request.structure, ID_USERNAME);
        final ViewNode passwordLabel = findNodeByResourceId(request.structure, ID_PASSWORD_LABEL);
        final ViewNode password = findNodeByResourceId(request.structure, ID_PASSWORD);

        assertUrlBarIsSanitized(urlBar);
        assertTextIsSanitized(username);
        assertTextIsSanitized(password);
        assertLabel(usernameLabel, "Username");
        assertLabel(passwordLabel, "Password");

        assertThat(usernameLabel.getClassName()).isEqualTo(LABEL_CLASS);
        assertThat(username.getClassName()).isEqualTo(TEXT_CLASS);
        assertThat(passwordLabel.getClassName()).isEqualTo(LABEL_CLASS);
        assertThat(password.getClassName()).isEqualTo(TEXT_CLASS);

        assertThat(username.getIdEntry()).isEqualTo(ID_USERNAME);
        assertThat(password.getIdEntry()).isEqualTo(ID_PASSWORD);

        final String[] autofillHints = username.getAutofillHints();
        if (mCompatMode) {
            assertThat(autofillHints).isNull();
            assertThat(username.getHtmlInfo()).isNull();
            assertThat(password.getHtmlInfo()).isNull();
        } else {
            // Make sure order is preserved and dupes not removed.
            assertThat(autofillHints).asList()
                    .containsExactly("c", "a", "a", "b", "a", "a")
                    .inOrder();
            try {
                VirtualContainerView.assertHtmlInfo(username);
                VirtualContainerView.assertHtmlInfo(password);
            } catch (AssertionError | RuntimeException e) {
                dumpStructure("HtmlInfo failed", request.structure);
                throw e;
            }
        }

        // Make sure initial focus was properly set.
        assertWithMessage("Username node is not focused").that(username.isFocused()).isTrue();
        assertWithMessage("Password node is focused").that(password.isFocused()).isFalse();

        // Auto-fill it.
        mUiBot.selectDataset("The Dude");

        // Check the results.
        mActivity.assertAutoFilled();
    }

    @Test
    public void testAutofillTwoDatasets() throws Exception {
        if (sRanAlready) return;
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_USERNAME, "dude")
                        .setField(ID_PASSWORD, "sweet")
                        .setPresentation(createPresentation("The Dude"))
                        .build())
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_USERNAME, "DUDE")
                        .setField(ID_PASSWORD, "SWEET")
                        .setPresentation(createPresentation("THE DUDE"))
                        .build())
                .build());
        mActivity.expectAutoFill("DUDE", "SWEET");

        // Trigger auto-fill.
        focusToUsername();
        sReplier.getNextFillRequest();
        assertDatasetShown(mActivity.mUsername, "The Dude", "THE DUDE");

        // Play around with focus to make sure picker is properly drawn.
        focusToPassword();
        assertDatasetShown(mActivity.mPassword, "The Dude", "THE DUDE");
        focusToUsername();
        assertDatasetShown(mActivity.mUsername, "The Dude", "THE DUDE");

        // Auto-fill it.
        mUiBot.selectDataset("THE DUDE");

        // Check the results.
        mActivity.assertAutoFilled();
    }

    @Test
    public void testAutofillOverrideDispatchProvideAutofillStructure() throws Exception {
        if (sRanAlready) return;
        mActivity.mCustomView.setOverrideDispatchProvideAutofillStructure(true);
        autofillTest(true);
    }

    @Test
    public void testAutofillManuallyOneDataset() throws Exception {
        if (mCompatMode) return; // TODO(b/73557072): not supported yet

        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedDataset.Builder()
                .setField(ID_USERNAME, "dude")
                .setField(ID_PASSWORD, "sweet")
                .setPresentation(createPresentation("The Dude"))
                .build());
        mActivity.expectAutoFill("dude", "sweet");

        // Trigger auto-fill.
        mActivity.requestAutofill(mActivity.mUsername);
        sReplier.getNextFillRequest();

        // Select datatest.
        mUiBot.selectDataset("The Dude");

        // Check the results.
        mActivity.assertAutoFilled();
    }

    @Test
    public void testAutofillManuallyTwoDatasetsPickFirst() throws Exception {
        autofillManuallyTwoDatasets(true);
    }

    @Test
    public void testAutofillManuallyTwoDatasetsPickSecond() throws Exception {
        autofillManuallyTwoDatasets(false);
    }

    private void autofillManuallyTwoDatasets(boolean pickFirst) throws Exception {
        if (mCompatMode) return; // TODO(b/73557072): not supported yet

        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_USERNAME, "dude")
                        .setField(ID_PASSWORD, "sweet")
                        .setPresentation(createPresentation("The Dude"))
                        .build())
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_USERNAME, "jenny")
                        .setField(ID_PASSWORD, "8675309")
                        .setPresentation(createPresentation("Jenny"))
                        .build())
                .build());
        if (pickFirst) {
            mActivity.expectAutoFill("dude", "sweet");
        } else {
            mActivity.expectAutoFill("jenny", "8675309");

        }

        // Trigger auto-fill.
        mActivity.getSystemService(AutofillManager.class).requestAutofill(
                mActivity.mCustomView, mActivity.mUsername.text.id, mActivity.mUsername.bounds);
        sReplier.getNextFillRequest();

        // Auto-fill it.
        final UiObject2 picker = mUiBot.assertDatasets("The Dude", "Jenny");
        mUiBot.selectDataset(picker, pickFirst ? "The Dude" : "Jenny");

        // Check the results.
        mActivity.assertAutoFilled();
    }

    @Test
    public void testAutofillCallbacks() throws Exception {
        if (sRanAlready) return;
        // Set service.
        enableService();
        final MyAutofillCallback callback = mActivity.registerCallback();

        // Set expectations.
        sReplier.addResponse(new CannedDataset.Builder()
                .setField(ID_USERNAME, "dude")
                .setField(ID_PASSWORD, "sweet")
                .setPresentation(createPresentation("The Dude"))
                .build());
        mActivity.expectAutoFill("dude", "sweet");

        // Trigger auto-fill.
        focusToUsername();
        sReplier.getNextFillRequest();

        callback.assertUiShownEvent(mActivity.mCustomView, mActivity.mUsername.text.id);

        // Change focus
        focusToPassword();
        callback.assertUiHiddenEvent(mActivity.mCustomView, mActivity.mUsername.text.id);
        callback.assertUiShownEvent(mActivity.mCustomView, mActivity.mPassword.text.id);
    }

    @Test
    public void testAutofillCallbackDisabled() throws Throwable {
        if (sRanAlready) return;
        // Set service.
        disableService();
        final MyAutofillCallback callback = mActivity.registerCallback();

        // Trigger auto-fill.
        focusToUsernameExpectNoWindowEvent();

        // Assert callback was called
        callback.assertUiUnavailableEvent(mActivity.mCustomView, mActivity.mUsername.text.id);
    }

    @Test
    public void testAutofillCallbackNoDatasets() throws Throwable {
        callbackUnavailableTest(NO_RESPONSE);
    }

    @Test
    public void testAutofillCallbackNoDatasetsButSaveInfo() throws Throwable {
        callbackUnavailableTest(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_USERNAME, ID_PASSWORD)
                .build());
    }

    private void callbackUnavailableTest(CannedFillResponse response) throws Throwable {
        if (sRanAlready) return;
        // Set service.
        enableService();
        final MyAutofillCallback callback = mActivity.registerCallback();

        // Set expectations.
        sReplier.addResponse(response);

        // Trigger auto-fill.
        focusToUsernameExpectNoWindowEvent();
        sReplier.getNextFillRequest();

        // Auto-fill it.
        mUiBot.assertNoDatasetsEver();

        // Assert callback was called
        callback.assertUiUnavailableEvent(mActivity.mCustomView, mActivity.mUsername.text.id);
    }

    @Test
    public void testSaveDialogNotShownWhenBackIsPressed() throws Exception {
        if (sRanAlready) return;
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_USERNAME, "dude")
                        .setField(ID_PASSWORD, "sweet")
                        .setPresentation(createPresentation("The Dude"))
                        .build())
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_USERNAME, ID_PASSWORD)
                .build());
        mActivity.expectAutoFill("dude", "sweet");

        // Trigger auto-fill.
        focusToUsername();
        sReplier.getNextFillRequest();
        assertDatasetShown(mActivity.mUsername, "The Dude");

        mUiBot.pressBack();

        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);
    }

    @Test
    public void testSave_childViewsGone() throws Throwable {
        saveTest(CommitType.CHILDREN_VIEWS_GONE);
    }

    @Test
    @Ignore("Disabled until b/73493342 is fixed")
    public void testSave_parentViewGone() throws Throwable {
        saveTest(CommitType.PARENT_VIEW_GONE);
    }

    @Test
    public void testSave_appCallsCommit() throws Throwable {
        saveTest(CommitType.EXPLICIT_COMMIT);
    }

    @Test
    public void testSave_submitButtonClicked() throws Throwable {
        if (mCompatMode) return; // TODO(b/73649008): implement it
        saveTest(CommitType.SUBMIT_BUTTON_CLICKED);
    }

    enum CommitType {
        CHILDREN_VIEWS_GONE,
        PARENT_VIEW_GONE,
        EXPLICIT_COMMIT,
        SUBMIT_BUTTON_CLICKED
    }

    private void saveTest(CommitType commitType) throws Throwable {
        if (sRanAlready) return;
        // Set service.
        enableService();

        // Set expectations.
        final CannedFillResponse.Builder response = new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_USERNAME, ID_PASSWORD);

        switch (commitType) {
            case CHILDREN_VIEWS_GONE:
            case PARENT_VIEW_GONE:
                response.setSaveInfoFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE);
                break;
            case SUBMIT_BUTTON_CLICKED:
                response
                    .setSaveInfoFlags(SaveInfo.FLAG_DONT_SAVE_ON_FINISH)
                    .setSaveTriggerId(mActivity.mCustomView.mLoginButtonId);
                break;
        }
        sReplier.addResponse(response.build());

        // Trigger auto-fill.
        focusToUsernameExpectNoWindowEvent();
        sReplier.getNextFillRequest();

        // Fill in some stuff
        mActivity.mUsername.setText("foo");
        mActivity.mPassword.setText("bar");
        switch (commitType) {
            case CHILDREN_VIEWS_GONE:
                mActivity.mUsername.changeVisibility(false);
                mActivity.mPassword.changeVisibility(false);
                break;
            case PARENT_VIEW_GONE:
                mActivity.runOnUiThread(() -> {
                    final ViewGroup parent = (ViewGroup) mActivity.mCustomView.getParent();
                    parent.removeView(mActivity.mCustomView);
                });
                break;
            case EXPLICIT_COMMIT:
                mActivity.getAutofillManager().commit();
                break;
            case SUBMIT_BUTTON_CLICKED:
                mActivity.mCustomView.clickLogin();
                break;
            default:
                throw new IllegalArgumentException("unknown type: " + commitType);
        }

        // Trigger save.
        mUiBot.saveForAutofill(true, SAVE_DATA_TYPE_PASSWORD);

        // Assert results
        final SaveRequest saveRequest = sReplier.getNextSaveRequest();
        final ViewNode username = findNodeByResourceId(saveRequest.structure, ID_USERNAME);
        final ViewNode password = findNodeByResourceId(saveRequest.structure, ID_PASSWORD);

        if (mCompatMode) {
            // TODO(b/73557456): temporarily not checking values as it's not working
            assertThat(username).isNotNull();
            assertThat(password).isNotNull();
            return;
        }

        assertTextAndValue(username, "foo");
        assertTextAndValue(password, "bar");
    }

    @Test
    public void testSaveNotShownWhenVirtualViewValueChanges() throws Throwable {
        if (mCompatMode) return; // TODO(b/72811561): figure out why it fails on compat mode

        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_USERNAME, ID_USERNAME)
                .setSaveInfoFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                .build());

        // Trigger auto-fill.
        focusToUsernameExpectNoWindowEvent();
        sReplier.getNextFillRequest();

        // Fill in some stuff
        mActivity.mUsername.setText("foo");

        // Trigger save.
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_USERNAME);
    }

    @Test
    public void testAppCannotFakePackageName() throws Exception {
        if (sRanAlready) return;
        // Set service.
        enableService();

        // Set expectations.
        sReplier.acceptRequestsFromPackage("MALICIOUS");
        mActivity.mCustomView.fakePackageName(new ComponentName("MALICIOUS", "AM.I"));
        sReplier.addResponse(new CannedDataset.Builder()
                .setField(ID_USERNAME, "dude")
                .setField(ID_PASSWORD, "sweet")
                .setPresentation(createPresentation("The Dude"))
                .build());

        // Trigger auto-fill.
        focusToUsername();
        assertDatasetShown(mActivity.mUsername, "The Dude");

        // Make sure package name was sanitized.
        final FillRequest request = sReplier.getNextFillRequest();
        assertThat(request.structure.getActivityComponent().getPackageName())
                .isEqualTo(mPackageName);
    }

    /**
     * Asserts the dataset picker is properly displayed in a give line.
     */
    private void assertDatasetShown(Line line, String... expectedDatasets) throws Exception {
        // TODO(b/73548352): temporary workaround until we figure out why there's a mismatch on the
        // values reported while running on compat mode.
        if (mCompatMode) return;

        final Rect pickerBounds = mUiBot.assertDatasets(expectedDatasets).getVisibleBounds();
        final Rect fieldBounds = line.getAbsCoordinates();
        assertWithMessage("vertical coordinates don't match; picker=%s, field=%s", pickerBounds,
                fieldBounds).that(pickerBounds.top).isEqualTo(fieldBounds.bottom);
        assertWithMessage("horizontal coordinates don't match; picker=%s, field=%s", pickerBounds,
                fieldBounds).that(pickerBounds.left).isEqualTo(fieldBounds.left);
    }

    private void assertLabel(ViewNode node, String expectedValue) {
        if (mCompatMode) {
            // Compat mode doesn't set AutofillValue of non-editable fields
            assertTextOnly(node, expectedValue);
        } else {
            assertTextAndValue(node, expectedValue);
        }
    }

    protected void assertUrlBarIsSanitized(ViewNode urlBar) {
        assertTextIsSanitized(urlBar);
        assertThat(urlBar.getWebDomain()).isNull();
        assertThat(urlBar.getWebScheme()).isNull();
    }

    // TODO(b/72811561): currently only one test pass at time
    protected static boolean sRanAlready = false;
}
