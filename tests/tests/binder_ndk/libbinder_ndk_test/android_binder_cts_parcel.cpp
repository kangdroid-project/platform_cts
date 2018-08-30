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
#define LOG_TAG "Cts-NdkBinderTest"

#include <android/binder_ibinder.h>
#include <android/log.h>
#include <gtest/gtest.h>

#include "utilities.h"

#include <limits>
#include <vector>

class NdkBinderTest_AParcel : public NdkBinderTest {};

// These reads and writes an array of possible values all of the same type.
template <typename T, binder_status_t (*write)(AParcel*, const T),
          binder_status_t (*read)(const AParcel*, T*)>
void ExpectInOut(std::vector<T> in) {
  AIBinder* binder = SampleData::newBinder(
      [](transaction_code_t, const AParcel* in, AParcel* out) {
        T readTarget;
        EXPECT_OK(read(in, &readTarget));
        EXPECT_OK(write(out, readTarget));
        return STATUS_OK;
      },
      ExpectLifetimeTransactions(in.size()));

  for (const auto& value : in) {
    EXPECT_OK(SampleData::transact(binder, kCode,
                                   [&](AParcel* in) {
                                     EXPECT_OK(write(in, value));
                                     return STATUS_OK;
                                   },
                                   [&](const AParcel* out) {
                                     T readTarget;
                                     EXPECT_OK(read(out, &readTarget));
                                     EXPECT_EQ(value, readTarget);
                                     return STATUS_OK;
                                   }));
  }

  AIBinder_decStrong(binder);
}

template <typename T, binder_status_t (*write)(AParcel*, const T),
          binder_status_t (*read)(const AParcel*, T*)>
void ExpectInOutMinMax() {
  ExpectInOut<T, write, read>(
      {std::numeric_limits<T>::min(), std::numeric_limits<T>::max()});
}

TEST_F(NdkBinderTest_AParcel, ReadUnexpectedNullBinder) {
  AIBinder* binder = SampleData::newBinder(
      [](transaction_code_t, const AParcel* in, AParcel* /*out*/) {
        AIBinder* value = nullptr;
        binder_status_t ret = AParcel_readStrongBinder(in, &value);
        EXPECT_EQ(nullptr, value);
        EXPECT_EQ(STATUS_UNEXPECTED_NULL, ret);
        return ret;
      },
      ExpectLifetimeTransactions(1));

  EXPECT_EQ(STATUS_UNEXPECTED_NULL,
            SampleData::transact(binder, kCode, [&](AParcel* in) {
              EXPECT_OK(AParcel_writeStrongBinder(in, nullptr));
              return STATUS_OK;
            }));

  AIBinder_decStrong(binder);
}

TEST_F(NdkBinderTest_AParcel, BindersInMustComeOut) {
  AIBinder* binder = SampleData::newBinder();

  ExpectInOut<AIBinder*, AParcel_writeStrongBinder, AParcel_readStrongBinder>(
      {binder});
  // copy which is read when this binder is sent in a transaction to this
  // process
  AIBinder_decStrong(binder);
  // copy which is read when this binder is returned in a transaction within
  // this same process and is read again
  AIBinder_decStrong(binder);

  ExpectInOut<AIBinder*, AParcel_writeStrongBinder,
              AParcel_readNullableStrongBinder>({nullptr, binder});
  // copy which is read when this binder is sent in a transaction to this
  // process
  AIBinder_decStrong(binder);
  // copy which is read when this binder is returned in a transaction within
  // this same process and is read again
  AIBinder_decStrong(binder);

  AIBinder_decStrong(binder);
}

TEST_F(NdkBinderTest_AParcel, WhatGoesInMustComeOut) {
  ExpectInOut<int32_t, AParcel_writeInt32, AParcel_readInt32>(
      {-7, -1, 0, 1, 45});
  ExpectInOut<uint32_t, AParcel_writeUint32, AParcel_readUint32>(
      {0, 1, 2, 100});
  ExpectInOut<int64_t, AParcel_writeInt64, AParcel_readInt64>(
      {-7, -1, 0, 1, 45});
  ExpectInOut<uint64_t, AParcel_writeUint64, AParcel_readUint64>(
      {0, 1, 2, 100});
  ExpectInOut<float, AParcel_writeFloat, AParcel_readFloat>(
      {-1.0f, 0.0f, 1.0f, 0.24975586f, 0.3f});
  ExpectInOut<double, AParcel_writeDouble, AParcel_readDouble>(
      {-1.0, 0.0, 1.0, 0.24975586, 0.3});

  ExpectInOut<bool, AParcel_writeBool, AParcel_readBool>({true, false});
  ExpectInOut<char16_t, AParcel_writeChar, AParcel_readChar>(
      {L'\0', L'S', L'@', L'\n'});
  ExpectInOut<int8_t, AParcel_writeByte, AParcel_readByte>({-7, -1, 0, 1, 45});
}

TEST_F(NdkBinderTest_AParcel, ExtremeValues) {
  ExpectInOutMinMax<int32_t, AParcel_writeInt32, AParcel_readInt32>();
  ExpectInOutMinMax<uint32_t, AParcel_writeUint32, AParcel_readUint32>();
  ExpectInOutMinMax<int64_t, AParcel_writeInt64, AParcel_readInt64>();
  ExpectInOutMinMax<uint64_t, AParcel_writeUint64, AParcel_readUint64>();
  ExpectInOutMinMax<float, AParcel_writeFloat, AParcel_readFloat>();
  ExpectInOutMinMax<double, AParcel_writeDouble, AParcel_readDouble>();
  ExpectInOutMinMax<bool, AParcel_writeBool, AParcel_readBool>();
  ExpectInOutMinMax<char16_t, AParcel_writeChar, AParcel_readChar>();
  ExpectInOutMinMax<int8_t, AParcel_writeByte, AParcel_readByte>();
}

TEST_F(NdkBinderTest_AParcel, CantReadFromEmptyParcel) {
  AIBinder* binder = SampleData::newBinder(TransactionsReturn(STATUS_OK),
                                           ExpectLifetimeTransactions(1));

  EXPECT_OK(SampleData::transact(
      binder, kCode, WriteNothingToParcel, [&](const AParcel* out) {
        bool readTarget = false;
        EXPECT_EQ(-ENODATA, AParcel_readBool(out, &readTarget));
        EXPECT_FALSE(readTarget);
        return STATUS_OK;
      }));
  AIBinder_decStrong(binder);
}
