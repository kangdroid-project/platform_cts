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

#include <android/binder_ibinder.h>
#include <gtest/gtest.h>

#include <functional>

// Helpers for testing

#define EXPECT_OK(THING) EXPECT_EQ(STATUS_OK, (THING))
#define ASSERT_OK(THING) ASSERT_EQ(STATUS_OK, (THING))

// placeholder
constexpr transaction_code_t kCode = +1 + 918;

// Usually, things at this level would be generated by the aidl compiler. This
// class is merely to make testing the API easier.

struct SampleData;

typedef std::function<void(SampleData*)> OnDestroyFunc;
typedef std::function<binder_status_t(transaction_code_t code,
                                      const AParcel* in, AParcel* out)>
    OnTransactFunc;

typedef std::function<binder_status_t(AParcel*)> WriteParcel;
typedef std::function<binder_status_t(const AParcel*)> ReadParcel;

static inline binder_status_t WriteNothingToParcel(AParcel*) {
  return STATUS_OK;
}
static inline binder_status_t ReadNothingFromParcel(const AParcel*) {
  return STATUS_OK;
}

struct SampleData {
  static size_t numInstances();

  static const char* kDescriptor;
  static const AIBinder_Class* kClass;

  static const char* kAnotherDescriptor;
  static const AIBinder_Class* kAnotherClass;

  SampleData(const OnTransactFunc& oT = nullptr,
             const OnDestroyFunc& oD = nullptr)
      : onTransact(oT), onDestroy(oD) {}

  // This is called when the class is transacted on if non-null.
  // Otherwise, STATUS_FAILED_TRANSACTION is returned.
  OnTransactFunc onTransact;

  // This is called when the class is destroyed if non-null.
  OnDestroyFunc onDestroy;

  // Automatically updated by this class whenever a transaction is received.
  int numberTransactions = 0;

  __attribute__((warn_unused_result)) static AIBinder* newBinder(
      OnTransactFunc onTransact = nullptr, OnDestroyFunc onDestroy = nullptr) {
    SampleData* data = new SampleData(onTransact, onDestroy);
    return AIBinder_new(kClass, static_cast<void*>(data));
  };

  // Helper method to simplify transaction logic
  static binder_status_t transact(AIBinder* binder, transaction_code_t code,
                                  WriteParcel writeFunc = WriteNothingToParcel,
                                  ReadParcel readFunc = ReadNothingFromParcel,
                                  binder_flags_t flags = 0) {
    AParcel* in;
    binder_status_t status = AIBinder_prepareTransaction(binder, &in);
    if (status != STATUS_OK) return status;

    status = writeFunc(in);
    if (status != STATUS_OK) {
      AParcel_delete(in);
      return status;
    }

    AParcel* out;
    status = AIBinder_transact(binder, code, &in, &out, flags);
    if (status != STATUS_OK) return status;

    status = readFunc(out);
    AParcel_delete(out);

    return status;
  }
};

static inline OnDestroyFunc ExpectLifetimeTransactions(size_t count) {
  return [count](SampleData* data) {
    EXPECT_EQ(count, data->numberTransactions)
        << "Expected " << count
        << " transaction(s), but over the lifetime of this object, it received "
        << data->numberTransactions;
  };
}

static inline OnTransactFunc TransactionsReturn(binder_status_t result) {
  return
      [result](transaction_code_t, const AParcel*, AParcel*) { return result; };
}

class NdkBinderTest : public ::testing::Test {
 public:
  void SetUp() override { EXPECT_EQ(0, SampleData::numInstances()); }
  void TearDown() override { EXPECT_EQ(0, SampleData::numInstances()); }
};
