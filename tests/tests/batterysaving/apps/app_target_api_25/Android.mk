# Copyright (C) 2018 The Android Open Source Project
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

LOCAL_PACKAGE_NAME := CtsBatterySavingAppTargetApi25

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

LOCAL_SRC_FILES := $(call all-java-files-under, ../app_target_api_current/src)

LOCAL_STATIC_JAVA_LIBRARIES := \
    BatterySavingCtsCommon \
    androidx.test.rules \
    androidx.legacy_legacy-support-v4 \
    mockito-target-minus-junit4 \
    compatibility-device-util-axt \
    ub-uiautomator

LOCAL_SDK_VERSION := test_current
LOCAL_MIN_SDK_VERSION := 23

# tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts vts general-tests

include $(BUILD_CTS_PACKAGE)
