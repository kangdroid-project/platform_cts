/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.car.cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.content.pm.CarPackageManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.platform.test.annotations.RequiresDevice;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RequiresDevice
@RunWith(AndroidJUnit4.class)
public class CarPackageManagerTest extends CarApiTestBase {

    private CarPackageManager mCarPm;
    private static String TAG = CarPackageManagerTest.class.getSimpleName();

    /** Name of the meta-data attribute for the automotive application XML resource */
    private static final String METADATA_ATTRIBUTE = "android.car.application";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mCarPm = (CarPackageManager) getCar().getCarManager(Car.PACKAGE_SERVICE);
    }

    @Test
    public void testActivityDistractionOptimized() throws Exception {
       assertFalse(mCarPm.isActivityDistractionOptimized("com.basic.package", "DummyActivity"));
       // Real system activity is not allowed as well.
       assertFalse(mCarPm.isActivityDistractionOptimized("com.android.phone", "CallActivity"));

       try {
           mCarPm.isActivityDistractionOptimized("com.android.settings", null);
           fail();
       } catch (IllegalArgumentException expected) {
           // Expected.
       }
       try {
           mCarPm.isActivityDistractionOptimized(null, "Any");
           fail();
       } catch (IllegalArgumentException expected) {
           // Expected.
       }
       try {
           mCarPm.isActivityDistractionOptimized(null, null);
           fail();
       } catch (IllegalArgumentException expected) {
           // Expected.
       }
   }

    @Test
    public void testSystemActivitiesAllowed() throws CarNotConnectedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(
                PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

        for (PackageInfo info : packages) {
            if (info.applicationInfo == null) {
                continue;
            }
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    ((info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)) {

                Bundle metaData = info.applicationInfo.metaData;
                if (metaData == null || metaData.getInt(METADATA_ATTRIBUTE, 0) == 0) {
                    continue;  // No car metadata, ignoring this app.
                }

                if (info.activities != null && info.activities.length > 0) {
                    String activity = info.activities[0].name;
                    String packageName = info.packageName;
                    assertTrue("Failed for package: " + packageName + ", activity: " + activity,
                            mCarPm.isActivityDistractionOptimized(packageName, activity));
                }
            }
        }
    }

    @Test
    @Ignore // Enable when b/120125891 is fixed
    public void testServiceDistractionOptimized() throws Exception {
        assertFalse(mCarPm.isServiceDistractionOptimized("com.basic.package", ""));
        assertTrue(mCarPm.isServiceDistractionOptimized("com.android.settings", "Any"));
        assertTrue(mCarPm.isServiceDistractionOptimized("com.android.settings", ""));
        assertTrue(mCarPm.isServiceDistractionOptimized("com.android.settings", null));

        try {
            mCarPm.isServiceDistractionOptimized(null, "Any");
            fail();
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

}
