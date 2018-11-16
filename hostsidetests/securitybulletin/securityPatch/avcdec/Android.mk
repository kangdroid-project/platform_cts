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
#=========================================================================
# NOTE: This module uses the libavc's testbench from external folder
# without creating a copy of the testbench locally. Hence LOCAL_SRC_FILES
# is pointed to external folder.
# This module is dependent on external/libavc/test
#=========================================================================
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := testavcdec
LOCAL_SRC_FILES := ../../../../../external/libavc/test/decoder/main.c
LOCAL_MULTILIB := 32
LOCAL_MODULE_STEM_32 := $(LOCAL_MODULE)32
LOCAL_C_INCLUDES := external/libavc/common
LOCAL_C_INCLUDES += external/libavc/decoder
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_SHARED_LIBRARIES += libstagefright_soft_avcdec
LOCAL_STATIC_LIBRARIES := libavcdec

# Tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts sts vts
LOCAL_CTS_TEST_PACKAGE := android.security.cts

LOCAL_ARM_MODE := arm
LOCAL_CFLAGS += -Wall -Werror
LOCAL_CFLAGS += -DPROFILE_ENABLE -fPIC -DMD5_DISABLE
include $(BUILD_CTS_EXECUTABLE)
