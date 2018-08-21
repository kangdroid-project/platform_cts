/*
 * Copyright (C) 2017 Google Inc.
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

package android.location.cts;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.android.compatibility.common.util.CddTest;

/**
 * Test the {@link Location} values.
 *
 * Test steps:
 * 1. Register for location updates.
 * 2. Wait for {@link #LOCATION_TO_COLLECT_COUNT} locations.
 *          3.1 Confirm locations have been found.
 * 3. Get LastKnownLocation, verified all fields are in the correct range.
 */
public class GnssLocationValuesTest extends GnssTestCase {

  private static final String TAG = "GnssLocationValuesTest";
  private static final int LOCATION_TO_COLLECT_COUNT = 5;
  private TestLocationListener mLocationListener;
  private static final int LOCATION_UNCERTIANTY_MIN_YEAR = 2017;
  private boolean extendedLocationAccuracyExpected = false;
  // TODO(b/65458848): Re-tighten the limit to 0.001 when sufficient devices in the market comply
  private static final double MINIMUM_SPEED_FOR_BEARING = 1.000;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mTestLocationManager = new TestLocationManager(getContext());
    int gnssYearOfHardware = mTestLocationManager.getLocationManager().getGnssYearOfHardware();
    if(gnssYearOfHardware >= LOCATION_UNCERTIANTY_MIN_YEAR) {
      extendedLocationAccuracyExpected = true;
    }
    mLocationListener = new TestLocationListener(LOCATION_TO_COLLECT_COUNT);
  }

  @Override
  protected void tearDown() throws Exception {
    // Unregister listeners
    if (mLocationListener != null) {
      mTestLocationManager.removeLocationUpdates(mLocationListener);
    }
    super.tearDown();
  }

  /**
   * Those accuracy fields are new O-features,
   * only test them if the hardware is later than 2017
   */
  public void testAccuracyFields() throws Exception {
    SoftAssert softAssert = new SoftAssert(TAG);
    mTestLocationManager.requestLocationUpdates(mLocationListener);
    boolean success = mLocationListener.await();
    SoftAssert.failOrWarning(isMeasurementTestStrict(),
        "Time elapsed without getting the GNSS locations."
            + " Possibly, the test has been run deep indoors."
            + " Consider retrying test outdoors.",
        success);

    for (Location location : mLocationListener.getReceivedLocationList()) {
      checkLocationAccuracyFields(softAssert, location,
          extendedLocationAccuracyExpected);
    }

    softAssert.assertAll();
  }

  @CddTest(requirement="7.3.3/C-3-4")
  public static void checkLocationAccuracyFields(SoftAssert softAssert,
      Location location, boolean extendedLocationAccuracyExpected) {
    softAssert.assertTrue("All locations generated by the LocationManager "
        + "should have a horizontal accuracy.", location.hasAccuracy());
    if (location.hasAccuracy()) {
      softAssert.assertTrue("Location Accuracy should be greater than 0.",
          location.getAccuracy() > 0);
    }

    if (!extendedLocationAccuracyExpected) {
      return;
    }
    Log.i(TAG, "This is a device from 2017 or later.");

    if (location.hasSpeed() && location.getSpeed() > MINIMUM_SPEED_FOR_BEARING) {
      softAssert.assertTrue("When speed is greater than 0, all GNSS locations generated by "
          + "the LocationManager must have bearing accuracies.", location.hasBearingAccuracy());
      if (location.hasBearingAccuracy()) {
        softAssert.assertTrue("Bearing Accuracy should be greater than 0.",
            location.getBearingAccuracyDegrees() > 0);
      }
    }

    softAssert.assertTrue("All GNSS locations generated by the LocationManager "
        + "must have a speed accuracy.", location.hasSpeedAccuracy());
    if (location.hasSpeedAccuracy()) {
      softAssert.assertTrue("Speed Accuracy should be greater than 0.",
          location.getSpeedAccuracyMetersPerSecond() > 0);
    }
    softAssert.assertTrue("All GNSS locations generated by the LocationManager "
        + "must have a vertical accuracy.", location.hasVerticalAccuracy());
    if (location.hasVerticalAccuracy()) {
      softAssert.assertTrue("Vertical Accuracy should be greater than 0.",
          location.getVerticalAccuracyMeters() > 0);
    }
  }

  /**
   * Get the location info from the device
   * check whether all fields' value make sense
   */
  public void testLocationRegularFields() throws Exception {
    mTestLocationManager.requestLocationUpdates(mLocationListener);
    boolean success = mLocationListener.await();
    SoftAssert.failOrWarning(isMeasurementTestStrict(),
        "Time elapsed without getting the GNSS locations."
            + " Possibly, the test has been run deep indoors."
            + " Consider retrying test outdoors.",
        success);

    SoftAssert softAssert = new SoftAssert(TAG);
    for (Location location : mLocationListener.getReceivedLocationList()) {
      checkLocationRegularFields(softAssert, location);
    }

    softAssert.assertAll();
  }

  public static void checkLocationRegularFields(SoftAssert softAssert, Location location) {
    // For the altitude: the unit is meter
    // The lowest exposed land on Earth is at the Dead Sea shore, at -413 meters.
    // Whilst University of Tokyo Atacama Obsevatory is on 5,640m above sea level.

    softAssert.assertTrue("All GNSS locations generated by the LocationManager "
        + "must have altitudes.", location.hasAltitude());
    if(location.hasAltitude()) {
      softAssert.assertTrue("Altitude should be greater than -500 (meters).",
          location.getAltitude() >= -500);
      softAssert.assertTrue("Altitude should be less than 6000 (meters).",
          location.getAltitude() < 6000);
    }

    // It is guaranteed to be in the range [0.0, 360.0] if the device has a bearing.
    // The API will return 0.0 if there is no bearing
    if (location.hasSpeed() && location.getSpeed() > MINIMUM_SPEED_FOR_BEARING) {
      softAssert.assertTrue("When speed is greater than 0, all GNSS locations generated by "
        + "the LocationManager must have bearings.", location.hasBearing());
      if(location.hasBearing()) {
        softAssert.assertTrue("Bearing should be in the range of [0.0, 360.0]",
            location.getBearing() >= 0 && location.getBearing() <= 360);
      }
    }

    softAssert.assertTrue("ElapsedRaltimeNanos should be great than 0.",
        location.getElapsedRealtimeNanos() > 0);

    assertEquals("gps", location.getProvider());
    assertTrue(location.getTime() > 0);

    softAssert.assertTrue("Longitude should be in the range of [-180.0, 180.0] degrees",
        location.getLongitude() >= -180 && location.getLongitude() <= 180);

    softAssert.assertTrue("Latitude should be in the range of [-90.0, 90.0] degrees",
        location.getLatitude() >= -90 && location.getLatitude() <= 90);

    softAssert.assertTrue("All GNSS locations generated by the LocationManager "
        + "must have speeds.", location.hasSpeed());

    // For the speed, during the cts test device shouldn't move faster than 1m/s
    if(location.hasSpeed()) {
      softAssert.assertTrue("In the test enviorment, speed should be in the range of [0, 1] m/s",
          location.getSpeed() >= 0 && location.getSpeed() <= 1);
    }

  }

}
