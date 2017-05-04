# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := CtsFragmentTestCases

# don't include this package in any target
LOCAL_MODULE_TAGS := tests

# and when built explicitly put it in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

LOCAL_DEX_PREOPT := false

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_STATIC_JAVA_LIBRARIES += \
    android-support-test \
    mockito-target-minus-junit4 \
    android-common \
    compatibility-device-util \
    ctstestrunner
#LOCAL_STATIC_JAVA_LIBRARIES := compatibility-device-util android-support-test

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts general-tests

LOCAL_SDK_VERSION := current

include $(BUILD_CTS_PACKAGE)
