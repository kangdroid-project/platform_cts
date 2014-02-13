LOCAL_PATH := $(call my-dir)

test_executable := bionic-unit-tests-cts
list_executable := $(test_executable)_list

include $(CLEAR_VARS)

LOCAL_MODULE := $(test_executable)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest

LOCAL_ADDITION_DEPENDENCIES := \
    $(LOCAL_PATH)/Android.mk \

LOCAL_SHARED_LIBRARIES += \
    libstlport \
    libdl \

LOCAL_WHOLE_STATIC_LIBRARIES += \
    libBionicTests \

LOCAL_STATIC_LIBRARIES += \
    libgtest \
    libgtest_main \

LOCAL_CTS_TEST_PACKAGE := android.bionic
include $(BUILD_CTS_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := $(list_executable)

LOCAL_ADDITION_DEPENDENCIES := \
    $(LOCAL_PATH)/Android.mk \

# A main without the extra output from the gtest main.
LOCAL_SRC_FILES := \
    main.cpp \

LOCAL_LDLIBS += \
    -lrt \

LOCAL_WHOLE_STATIC_LIBRARIES += \
    libBionicTests \

include $(BUILD_HOST_NATIVE_TEST)
