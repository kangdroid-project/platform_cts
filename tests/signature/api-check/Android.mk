# Copyright (C) 2017 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

# cts-api-signature-test java library
# ===================================

include $(CLEAR_VARS)

# don't include this package in any target
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src/java)

LOCAL_MODULE := cts-api-signature-test

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    cts-signature-common \
    repackaged.android.test.base \
    repackaged.android.test.runner \
    repackaged.android.test.mock \
    compatibility-device-util \

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libclassdescriptors
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := src/jni/classdescriptors.cpp
LOCAL_HEADER_LIBRARIES := jni_headers libopenjdkjvmti_headers
LOCAL_SDK_VERSION := current
LOCAL_NDK_STL_VARIANT := c++_static
include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
