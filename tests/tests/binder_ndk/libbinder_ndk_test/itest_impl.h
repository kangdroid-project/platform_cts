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

#include <test_package/BnTest.h>

#include "utilities.h"

using IEmpty = ::aidl::test_package::IEmpty;
using RegularPolygon = ::aidl::test_package::RegularPolygon;

class MyTest : public ::aidl::test_package::BnTest,
               public ThisShouldBeDestroyed {
 public:
  ::ndk::ScopedAStatus TestVoidReturn() override {
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus TestOneway() override {
    // This return code should be ignored since it is oneway.
    return ::ndk::ScopedAStatus(AStatus_fromStatus(STATUS_UNKNOWN_ERROR));
  }
  ::ndk::ScopedAStatus RepeatInt(int32_t in_value,
                                 int32_t* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatLong(int64_t in_value,
                                  int64_t* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatFloat(float in_value,
                                   float* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatDouble(double in_value,
                                    double* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatBoolean(bool in_value,
                                     bool* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatChar(char16_t in_value,
                                  char16_t* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatByte(int8_t in_value,
                                  int8_t* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatBinder(const ::ndk::SpAIBinder& in_value,
                                    ::ndk::SpAIBinder* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatInterface(
      const std::shared_ptr<IEmpty>& in_value,
      std::shared_ptr<IEmpty>* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatString(const std::string& in_value,
                                    std::string* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
  ::ndk::ScopedAStatus RepeatPolygon(const RegularPolygon& in_value,
                                     RegularPolygon* _aidl_return) override {
    *_aidl_return = in_value;
    return ::ndk::ScopedAStatus(AStatus_newOk());
  }
};