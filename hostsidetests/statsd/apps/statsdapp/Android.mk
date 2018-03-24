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

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := CtsStatsdApp
LOCAL_PRIVATE_PLATFORM_APIS := true

# don't include this package in any target
LOCAL_MODULE_TAGS := optional
# and when built explicitly put it in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := android.test.runner.stubs cts-junit org.apache.http.legacy

LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    ctstestrunner \
    compatibility-device-util \
    android-support-v4 \
    legacy-android-test \
    android-support-test \
    statsdprotolite

# tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts vts general-tests

# Exclude GlobalSettingsProto from being instrumented during the coverage build
# because instrumentation pushes us over the method size limit. Protos aren't
# generally interesting classes to measure so this is safe to exclude.
# TODO(kwekua): Remove this line after refactoring the class into smaller protos.
LOCAL_JACK_COVERAGE_EXCLUDE_FILTER := android.providers.settings.GlobalSettingsProto*

include $(BUILD_CTS_PACKAGE)
