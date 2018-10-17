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

#include <android/binder_ibinder_jni.h>
#include <gtest/gtest.h>
#include <aidl/test_package/BnEmpty.h>
#include <aidl/test_package/BpTest.h>
#include <aidl/test_package/RegularPolygon.h>

#include "itest_impl.h"
#include "utilities.h"

using ::aidl::test_package::BpTest;
using ::aidl::test_package::ITest;
using ::aidl::test_package::RegularPolygon;
using ::ndk::ScopedAStatus;
using ::ndk::SharedRefBase;
using ::ndk::SpAIBinder;

struct Params {
  std::shared_ptr<ITest> iface;
  bool shouldBeRemote;
  std::string expectedName;
};

#define iface GetParam().iface
#define shouldBeRemote GetParam().shouldBeRemote

class NdkBinderTest_Aidl : public NdkBinderTest,
                           public ::testing::WithParamInterface<Params> {};

TEST_P(NdkBinderTest_Aidl, GotTest) { ASSERT_NE(nullptr, iface); }

TEST_P(NdkBinderTest_Aidl, SanityCheckSource) {
  std::string name;
  ASSERT_OK(iface->GetName(&name));
  EXPECT_EQ(GetParam().expectedName, name);
}

TEST_P(NdkBinderTest_Aidl, Remoteness) {
  ASSERT_EQ(shouldBeRemote, iface->isRemote());
}

TEST_P(NdkBinderTest_Aidl, UseBinder) {
  ASSERT_EQ(STATUS_OK, AIBinder_ping(iface->asBinder().get()));
}

TEST_P(NdkBinderTest_Aidl, Trivial) {
  ASSERT_OK(iface->TestVoidReturn());
  ASSERT_OK(iface->TestOneway());
}

TEST_P(NdkBinderTest_Aidl, Constants) {
  ASSERT_EQ(0, ITest::kZero);
  ASSERT_EQ(1, ITest::kOne);
  ASSERT_EQ(0xffffffff, ITest::kOnes);
  ASSERT_EQ(std::string(""), ITest::kEmpty);
  ASSERT_EQ(std::string("foo"), ITest::kFoo);
}

TEST_P(NdkBinderTest_Aidl, RepeatPrimitives) {
  {
    int32_t out;
    ASSERT_OK(iface->RepeatInt(3, &out));
    EXPECT_EQ(3, out);
  }

  {
    int64_t out;
    ASSERT_OK(iface->RepeatLong(3, &out));
    EXPECT_EQ(3, out);
  }

  {
    float out;
    ASSERT_OK(iface->RepeatFloat(2.0f, &out));
    EXPECT_EQ(2.0f, out);
  }

  {
    double out;
    ASSERT_OK(iface->RepeatDouble(3.0, &out));
    EXPECT_EQ(3.0, out);
  }

  {
    bool out;
    ASSERT_OK(iface->RepeatBoolean(true, &out));
    EXPECT_EQ(true, out);
  }

  {
    char16_t out;
    ASSERT_OK(iface->RepeatChar(L'@', &out));
    EXPECT_EQ(L'@', out);
  }

  {
    int8_t out;
    ASSERT_OK(iface->RepeatByte(3, &out));
    EXPECT_EQ(3, out);
  }
}

TEST_P(NdkBinderTest_Aidl, RepeatBinder) {
  SpAIBinder binder = iface->asBinder();
  SpAIBinder ret;

  ASSERT_OK(iface->RepeatBinder(binder, &ret));
  EXPECT_EQ(binder.get(), ret.get());

  ASSERT_OK(iface->RepeatBinder(nullptr, &ret));
  EXPECT_EQ(nullptr, ret.get());
}

TEST_P(NdkBinderTest_Aidl, RepeatInterface) {
  class MyEmpty : public ::aidl::test_package::BnEmpty {};

  std::shared_ptr<IEmpty> empty = SharedRefBase::make<MyEmpty>();

  std::shared_ptr<IEmpty> ret;
  ASSERT_OK(iface->RepeatInterface(empty, &ret));
  EXPECT_EQ(empty.get(), ret.get());

  ASSERT_OK(iface->RepeatInterface(nullptr, &ret));
  EXPECT_EQ(nullptr, ret.get());
}

TEST_P(NdkBinderTest_Aidl, RepeatString) {
  std::string res;

  EXPECT_OK(iface->RepeatString("", &res));
  EXPECT_EQ("", res);

  EXPECT_OK(iface->RepeatString("a", &res));
  EXPECT_EQ("a", res);

  EXPECT_OK(iface->RepeatString("say what?", &res));
  EXPECT_EQ("say what?", res);
}

TEST_P(NdkBinderTest_Aidl, ParcelableDefaults) {
  RegularPolygon polygon;

  EXPECT_EQ("square", polygon.name);
  EXPECT_EQ(4, polygon.numSides);
  EXPECT_EQ(1.0f, polygon.sideLength);
}

TEST_P(NdkBinderTest_Aidl, RepeatPolygon) {
  RegularPolygon defaultPolygon = {"hexagon", 6, 2.0f};
  RegularPolygon outputPolygon;
  ASSERT_OK(iface->RepeatPolygon(defaultPolygon, &outputPolygon));
  EXPECT_EQ("hexagon", outputPolygon.name);
  EXPECT_EQ(defaultPolygon.numSides, outputPolygon.numSides);
  EXPECT_EQ(defaultPolygon.sideLength, outputPolygon.sideLength);
}

TEST_P(NdkBinderTest_Aidl, InsAndOuts) {
  RegularPolygon defaultPolygon;
  ASSERT_OK(iface->RenamePolygon(&defaultPolygon, "Jerry"));
  EXPECT_EQ("Jerry", defaultPolygon.name);
}

std::shared_ptr<ITest> getLocalService() {
  // BpTest -> AIBinder -> test
  std::shared_ptr<MyTest> test = SharedRefBase::make<MyTest>();
  return BpTest::associate(test->asBinder());
}

std::shared_ptr<ITest> getNdkBinderTestJavaService(const std::string& method) {
  JNIEnv* env = GetEnv();
  if (env == nullptr) {
    std::cout << "No environment" << std::endl;
    return nullptr;
  }

  jclass cl = env->FindClass("android/binder/cts/NdkBinderTest");
  if (cl == nullptr) {
    std::cout << "No class" << std::endl;
    return nullptr;
  }

  jmethodID mid =
      env->GetStaticMethodID(cl, method.c_str(), "()Landroid/os/IBinder;");
  if (mid == nullptr) {
    std::cout << "No method id" << std::endl;
    return nullptr;
  }

  jobject object = env->CallStaticObjectMethod(cl, mid);
  if (object == nullptr) {
    std::cout << "Got null service from Java" << std::endl;
    return nullptr;
  }

  SpAIBinder binder = SpAIBinder(AIBinder_fromJavaBinder(env, object));

  return BpTest::associate(binder);
}

INSTANTIATE_TEST_CASE_P(LocalNative, NdkBinderTest_Aidl,
                        ::testing::Values(Params{getLocalService(),
                                                 false /*shouldBeRemote*/,
                                                 "CPP"}));
INSTANTIATE_TEST_CASE_P(
    LocalNativeFromJava, NdkBinderTest_Aidl,
    ::testing::Values(Params{
        getNdkBinderTestJavaService("getLocalNativeService"),
        false /*shouldBeRemote*/, "CPP"}));
INSTANTIATE_TEST_CASE_P(LocalJava, NdkBinderTest_Aidl,
                        ::testing::Values(Params{
                            getNdkBinderTestJavaService("getLocalJavaService"),
                            false /*shouldBeRemote*/, "JAVA"}));
INSTANTIATE_TEST_CASE_P(
    RemoteNative, NdkBinderTest_Aidl,
    ::testing::Values(Params{
        getNdkBinderTestJavaService("getRemoteNativeService"),
        true /*shouldBeRemote*/, "CPP"}));
INSTANTIATE_TEST_CASE_P(RemoteJava, NdkBinderTest_Aidl,
                        ::testing::Values(Params{
                            getNdkBinderTestJavaService("getRemoteJavaService"),
                            true /*shouldBeRemote*/, "JAVA"}));
