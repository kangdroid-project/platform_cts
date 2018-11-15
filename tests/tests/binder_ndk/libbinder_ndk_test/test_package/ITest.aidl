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

package test_package;

import test_package.IEmpty;
import test_package.RegularPolygon;

// This test interface is used in order to test the all of the things that AIDL can generate which
// build on top of the NDK.
//
// Repeat => return the same value. This is used to keep the clients/tests simple.
interface ITest {
    const int kZero = 0;
    const int kOne = 1;
    const int kOnes = 0xffffffff;
    const String kEmpty = "";
    const String kFoo = "foo";

    String GetName();

    void TestVoidReturn();
    oneway void TestOneway();

    int GiveMeMyCallingPid();
    int GiveMeMyCallingUid();

    // This must be called before calling one of the give-me methods below
    oneway void CacheCallingInfoFromOneway();
    int GiveMeMyCallingPidFromOneway();
    int GiveMeMyCallingUidFromOneway();

    // Sending/receiving primitive types.
    int RepeatInt(int value);
    long RepeatLong(long value);
    float RepeatFloat(float value);
    double RepeatDouble(double value);
    boolean RepeatBoolean(boolean value);
    char RepeatChar(char value);
    byte RepeatByte(byte value);

    IBinder RepeatBinder(IBinder value);
    @nullable IBinder RepeatNullableBinder(@nullable IBinder value);
    IEmpty RepeatInterface(IEmpty value);
    @nullable IEmpty RepeatNullableInterface(@nullable IEmpty value);

    ParcelFileDescriptor RepeatFd(in ParcelFileDescriptor fd);

    String RepeatString(String value);

    RegularPolygon RepeatPolygon(in RegularPolygon value);

    // Testing inout
    void RenamePolygon(inout RegularPolygon value, String newName);

    // Arrays
    boolean[] RepeatBooleanArray(in boolean[] input, out boolean[] repeated);
    byte[] RepeatByteArray(in byte[] input, out byte[] repeated);
    char[] RepeatCharArray(in char[] input, out char[] repeated);
    int[] RepeatIntArray(in int[] input, out int[] repeated);
    long[] RepeatLongArray(in long[] input, out long[] repeated);
    float[] RepeatFloatArray(in float[] input, out float[] repeated);
    double[] RepeatDoubleArray(in double[] input, out double[] repeated);
    String[] RepeatStringArray(in String[] input, out String[] repeated);
}
