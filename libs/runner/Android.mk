# Copyright (C) 2012 The Android Open Source Project
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

# The legacy library that brings in android-support-test transitively

LOCAL_STATIC_JAVA_LIBRARIES := cts-test-runner

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := ctstestrunner

LOCAL_SDK_VERSION := current

include $(BUILD_STATIC_JAVA_LIBRARY)


# The library variant that brings in androidx.test transitively
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := cts-test-runner-axt

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := ctstestrunner-axt

LOCAL_SDK_VERSION := current

include $(BUILD_STATIC_JAVA_LIBRARY)


