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

LOCAL_PATH := $(call my-dir)

# Copy secondary APK to CTS target directory.
include $(CLEAR_VARS)
LOCAL_MODULE := cts-classloaderfactory-secondary-jar
LOCAL_MODULE_STEM := classloaderfactory-secondary.jar
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH = $(TARGET_OUT_TESTCASES)
LOCAL_COMPATIBILITY_SUITE := cts vts general-tests
include $(BUILD_SYSTEM)/base_rules.mk
my_secondary_apk_src := $(call intermediates-dir-for,JAVA_LIBRARIES,CtsClassLoaderFactoryTestCasesSecondaryDex,,COMMON)/javalib.jar
$(eval $(call copy-one-file,$(my_secondary_apk_src),$(LOCAL_BUILT_MODULE)))
my_secondary_apk_src :=

include $(call all-makefiles-under,$(LOCAL_PATH))
