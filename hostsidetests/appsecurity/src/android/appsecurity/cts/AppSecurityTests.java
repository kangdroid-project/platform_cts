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

package android.appsecurity.cts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.AppModeInstant;
import com.android.ddmlib.Log;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Set of tests that verify various security checks involving multiple apps are
 * properly enforced.
 */
@RunWith(DeviceJUnit4ClassRunner.class)
public class AppSecurityTests extends BaseAppSecurityTest {

    // testSharedUidDifferentCerts constants
    private static final String SHARED_UI_APK = "CtsSharedUidInstall.apk";
    private static final String SHARED_UI_PKG = "com.android.cts.shareuidinstall";
    private static final String SHARED_UI_DIFF_CERT_APK = "CtsSharedUidInstallDiffCert.apk";
    private static final String SHARED_UI_DIFF_CERT_PKG =
        "com.android.cts.shareuidinstalldiffcert";

    // testAppUpgradeDifferentCerts constants
    private static final String SIMPLE_APP_APK = "CtsSimpleAppInstall.apk";
    private static final String SIMPLE_APP_PKG = "com.android.cts.simpleappinstall";
    private static final String SIMPLE_APP_DIFF_CERT_APK = "CtsSimpleAppInstallDiffCert.apk";

    // testAppFailAccessPrivateData constants
    private static final String APP_WITH_DATA_APK = "CtsAppWithData.apk";
    private static final String APP_WITH_DATA_PKG = "com.android.cts.appwithdata";
    private static final String APP_WITH_DATA_CLASS =
            "com.android.cts.appwithdata.CreatePrivateDataTest";
    private static final String APP_WITH_DATA_CREATE_METHOD =
            "testCreatePrivateData";
    private static final String APP_WITH_DATA_CHECK_NOEXIST_METHOD =
            "testEnsurePrivateDataNotExist";
    private static final String APP_ACCESS_DATA_APK = "CtsAppAccessData.apk";
    private static final String APP_ACCESS_DATA_PKG = "com.android.cts.appaccessdata";

    // testInstrumentationDiffCert constants
    private static final String TARGET_INSTRUMENT_APK = "CtsTargetInstrumentationApp.apk";
    private static final String TARGET_INSTRUMENT_PKG = "com.android.cts.targetinstrumentationapp";
    private static final String INSTRUMENT_DIFF_CERT_APK = "CtsInstrumentationAppDiffCert.apk";
    private static final String INSTRUMENT_DIFF_CERT_PKG =
            "com.android.cts.instrumentationdiffcertapp";
    private static final String INSTRUMENT_DIFF_CERT_CLASS =
            "com.android.cts.instrumentationdiffcertapp.InstrumentationFailToRunTest";

    // testPermissionDiffCert constants
    private static final String DECLARE_PERMISSION_APK = "CtsPermissionDeclareApp.apk";
    private static final String DECLARE_PERMISSION_PKG = "com.android.cts.permissiondeclareapp";
    private static final String DECLARE_PERMISSION_COMPAT_APK = "CtsPermissionDeclareAppCompat.apk";
    private static final String DECLARE_PERMISSION_COMPAT_PKG = "com.android.cts.permissiondeclareappcompat";

    private static final String PERMISSION_DIFF_CERT_APK = "CtsUsePermissionDiffCert.apk";
    private static final String PERMISSION_DIFF_CERT_PKG =
        "com.android.cts.usespermissiondiffcertapp";

    private static final String LOG_TAG = "AppSecurityTests";

    @Before
    public void setUp() throws Exception {
        Utils.prepareSingleUser(getDevice());
        assertNotNull(getBuild());
    }

    /**
     * Test that an app that declares the same shared uid as an existing app, cannot be installed
     * if it is signed with a different certificate.
     */
    @Test
    @AppModeFull(reason = "Instant applications can't define shared UID")
    public void testSharedUidDifferentCerts() throws Exception {
        Log.i(LOG_TAG, "installing apks with shared uid, but different certs");
        try {
            getDevice().uninstallPackage(SHARED_UI_PKG);
            getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);

            new InstallMultiple().addApk(SHARED_UI_APK).run();
            new InstallMultiple().addApk(SHARED_UI_DIFF_CERT_APK)
                    .runExpectingFailure("INSTALL_FAILED_SHARED_USER_INCOMPATIBLE");
        } finally {
            getDevice().uninstallPackage(SHARED_UI_PKG);
            getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);
        }
    }

    /**
     * Test that an app update cannot be installed over an existing app if it has a different
     * certificate.
     */
    @Test
    @AppModeFull(reason = "'full' portion of the hostside test")
    public void testAppUpgradeDifferentCerts_full() throws Exception {
        testAppUpgradeDifferentCerts(false);
    }
    @Test
    @AppModeInstant(reason = "'instant' portion of the hostside test")
    public void testAppUpgradeDifferentCerts_instant() throws Exception {
        testAppUpgradeDifferentCerts(true);
    }
    private void testAppUpgradeDifferentCerts(boolean instant) throws Exception {
        Log.i(LOG_TAG, "installing app upgrade with different certs");
        try {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
            getDevice().uninstallPackage(SIMPLE_APP_DIFF_CERT_APK);

            new InstallMultiple(instant).addApk(SIMPLE_APP_APK).run();
            new InstallMultiple(instant).addApk(SIMPLE_APP_DIFF_CERT_APK)
                    .runExpectingFailure("INSTALL_FAILED_UPDATE_INCOMPATIBLE");
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
            getDevice().uninstallPackage(SIMPLE_APP_DIFF_CERT_APK);
        }
    }

    /**
     * Test that an app cannot access another app's private data.
     */
    @Test
    @AppModeFull(reason = "'full' portion of the hostside test")
    public void testAppFailAccessPrivateData_full() throws Exception {
        testAppFailAccessPrivateData(false);
    }
    @Test
    @AppModeInstant(reason = "'instant' portion of the hostside test")
    public void testAppFailAccessPrivateData_instant() throws Exception {
        testAppFailAccessPrivateData(true);
    }
    private void testAppFailAccessPrivateData(boolean instant)
            throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to access another app's private data");
        try {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
            getDevice().uninstallPackage(APP_ACCESS_DATA_PKG);

            new InstallMultiple().addApk(APP_WITH_DATA_APK).run();
            runDeviceTests(APP_WITH_DATA_PKG, APP_WITH_DATA_CLASS, APP_WITH_DATA_CREATE_METHOD);

            new InstallMultiple(instant).addApk(APP_ACCESS_DATA_APK).run();
            runDeviceTests(APP_ACCESS_DATA_PKG, null, null, instant);
        } finally {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
            getDevice().uninstallPackage(APP_ACCESS_DATA_PKG);
        }
    }

    /**
     * Test that uninstall of an app removes its private data.
     */
    @Test
    @AppModeFull(reason = "'full' portion of the hostside test")
    public void testUninstallRemovesData_full() throws Exception {
        testUninstallRemovesData(false);
    }
    @Test
    @AppModeInstant(reason = "'instant' portion of the hostside test")
    public void testUninstallRemovesData_instant() throws Exception {
        testUninstallRemovesData(true);
    }
    private void testUninstallRemovesData(boolean instant) throws Exception {
        Log.i(LOG_TAG, "Uninstalling app, verifying data is removed.");
        try {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);

            new InstallMultiple(instant).addApk(APP_WITH_DATA_APK).run();
            runDeviceTests(
                    APP_WITH_DATA_PKG, APP_WITH_DATA_CLASS, APP_WITH_DATA_CREATE_METHOD);

            getDevice().uninstallPackage(APP_WITH_DATA_PKG);

            new InstallMultiple(instant).addApk(APP_WITH_DATA_APK).run();
            runDeviceTests(
                    APP_WITH_DATA_PKG, APP_WITH_DATA_CLASS, APP_WITH_DATA_CHECK_NOEXIST_METHOD);
        } finally {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
        }
    }

    /**
     * Test that an app cannot instrument another app that is signed with different certificate.
     */
    @Test
    @AppModeFull(reason = "'full' portion of the hostside test")
    public void testInstrumentationDiffCert_full() throws Exception {
        testInstrumentationDiffCert(false, false);
    }
    @Test
    @AppModeInstant(reason = "'instant' portion of the hostside test")
    public void testInstrumentationDiffCert_instant() throws Exception {
        testInstrumentationDiffCert(false, true);
        testInstrumentationDiffCert(true, false);
        testInstrumentationDiffCert(true, true);
    }
    private void testInstrumentationDiffCert(boolean targetInstant, boolean instrumentInstant)
            throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to instrument another app");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(TARGET_INSTRUMENT_PKG);
            getDevice().uninstallPackage(INSTRUMENT_DIFF_CERT_PKG);

            new InstallMultiple(targetInstant).addApk(TARGET_INSTRUMENT_APK).run();
            new InstallMultiple(instrumentInstant).addApk(INSTRUMENT_DIFF_CERT_APK).run();

            // if we've installed either the instrumentation or target as an instant application,
            // starting an instrumentation will just fail instead of throwing a security exception
            // because neither the target nor instrumentation packages can see one another
            final String methodName = (targetInstant|instrumentInstant)
                    ? "testInstrumentationNotAllowed_fail"
                    : "testInstrumentationNotAllowed_exception";
            runDeviceTests(INSTRUMENT_DIFF_CERT_PKG, INSTRUMENT_DIFF_CERT_CLASS, methodName);
        } finally {
            getDevice().uninstallPackage(TARGET_INSTRUMENT_PKG);
            getDevice().uninstallPackage(INSTRUMENT_DIFF_CERT_PKG);
        }
    }

    /**
     * Test that an app cannot use a signature-enforced permission if it is signed with a different
     * certificate than the app that declared the permission.
     */
    @Test
    @AppModeFull(reason = "Only the platform can define permissions obtainable by instant applications")
    public void testPermissionDiffCert() throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to use permission of another app");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(DECLARE_PERMISSION_PKG);
            getDevice().uninstallPackage(DECLARE_PERMISSION_COMPAT_PKG);
            getDevice().uninstallPackage(PERMISSION_DIFF_CERT_PKG);

            new InstallMultiple().addApk(DECLARE_PERMISSION_APK).run();
            new InstallMultiple().addApk(DECLARE_PERMISSION_COMPAT_APK).run();

            new InstallMultiple().addApk(PERMISSION_DIFF_CERT_APK).run();
            runDeviceTests(PERMISSION_DIFF_CERT_PKG, null);
        } finally {
            getDevice().uninstallPackage(DECLARE_PERMISSION_PKG);
            getDevice().uninstallPackage(DECLARE_PERMISSION_COMPAT_PKG);
            getDevice().uninstallPackage(PERMISSION_DIFF_CERT_PKG);
        }
    }

    /**
     * Tests that an arbitrary file cannot be installed using the 'cmd' command.
     */
    @Test
    @AppModeFull(reason = "'full' portion of the hostside test")
    public void testAdbInstallFile_full() throws Exception {
        testAdbInstallFile(false);
    }
    @Test
    @AppModeInstant(reason = "'instant' portion of the hostside test")
    public void testAdbInstallFile_instant() throws Exception {
        testAdbInstallFile(true);
    }
    private void testAdbInstallFile(boolean instant) throws Exception {
        String output = getDevice().executeShellCommand(
                "cmd package install"
                        + (instant ? " --instant" : " --full")
                        + " -S 1024 /data/local/tmp/foo.apk");
        assertTrue("Error text", output.contains("Error"));
    }
}
