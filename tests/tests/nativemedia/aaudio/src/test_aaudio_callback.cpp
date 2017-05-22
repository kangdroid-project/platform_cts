/*
 * Copyright 2017 The Android Open Source Project
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

#define LOG_NDEBUG 0
#define LOG_TAG "AAudioTest"

#include <atomic>
#include <tuple>

#include <aaudio/AAudio.h>
#include <android/log.h>
#include <gtest/gtest.h>

#include "test_aaudio.h"
#include "utils.h"

static int32_t measureLatency(AAudioStream *stream) {
    int64_t presentationTime = 0;
    int64_t presentationPosition = 0;
    int64_t now = getNanoseconds();
    int32_t sampleRate = AAudioStream_getSampleRate(stream);
    int64_t framesWritten = AAudioStream_getFramesWritten(stream);
    aaudio_result_t result = AAudioStream_getTimestamp(stream,
                                                       CLOCK_MONOTONIC,
                                                       &presentationPosition,
                                                       &presentationTime);
    if (result < 0) {
        return result;
    }
    // Calculate when the last frame written would be played.
    int64_t deltaFrames = framesWritten - presentationPosition;
    EXPECT_GE(framesWritten, presentationPosition);
    int64_t calculatedDeltaNanos = deltaFrames * NANOS_PER_SECOND / sampleRate;
    int64_t calculatedTimeNanos = presentationTime +  calculatedDeltaNanos;
    int64_t latencyNanos = calculatedTimeNanos - now;
    int32_t latencyMillis = (int32_t) ((latencyNanos + NANOS_PER_MILLISECOND - 1)
                            / NANOS_PER_MILLISECOND);
    return latencyMillis;
}

using CbTestParams = std::tuple<aaudio_sharing_mode_t, int32_t>;

static std::string getTestName(const ::testing::TestParamInfo<CbTestParams>& info) {
    return std::string{sharingModeToString(std::get<0>(info.param))} +
            "_" + std::to_string(std::get<1>(info.param));
}

template<typename T>
class AAudioStreamCallbackTest : public ::testing::TestWithParam<CbTestParams> {
  protected:
    struct AAudioCallbackTestData {
        int32_t expectedFramesPerCallback;
        int32_t actualFramesPerCallback;
        int32_t minLatency;
        int32_t maxLatency;
        std::atomic<aaudio_result_t> callbackError;
        std::atomic<int32_t> callbackCount;

        AAudioCallbackTestData() {
            reset(0);
        }
        void reset(int32_t expectedFramesPerCb) {
            expectedFramesPerCallback = expectedFramesPerCb;
            actualFramesPerCallback = 0;
            minLatency = INT32_MAX;
            maxLatency = 0;
            callbackError = AAUDIO_OK;
            callbackCount = 0;
        }
        void updateFrameCount(int32_t numFrames) {
            if (numFrames != expectedFramesPerCallback) {
                // record unexpected framecounts
                actualFramesPerCallback = numFrames;
            } else if (actualFramesPerCallback == 0) {
                // record at least one frame count
                actualFramesPerCallback = numFrames;
            }
        }
        void updateLatency(int32_t latency) {
            if (latency <= 0) return;
            minLatency = std::min(minLatency, latency);
            maxLatency = std::max(maxLatency, latency);
        }
    };

    static void MyErrorCallbackProc(AAudioStream *stream, void *userData, aaudio_result_t error);

    AAudioStreamBuilder* builder() const { return mHelper->builder(); }
    AAudioStream* stream() const { return mHelper->stream(); }
    const StreamBuilderHelper::Parameters& actual() const { return mHelper->actual(); }

    std::unique_ptr<T> mHelper;
    bool mSetupSuccesful = false;
    std::unique_ptr<AAudioCallbackTestData> mCbData;
};

template<typename T>
void AAudioStreamCallbackTest<T>::MyErrorCallbackProc(
        AAudioStream* /*stream*/, void *userData, aaudio_result_t error) {
    AAudioCallbackTestData *myData = static_cast<AAudioCallbackTestData*>(userData);
    myData->callbackError = error;
}


class AAudioInputStreamCallbackTest : public AAudioStreamCallbackTest<InputStreamBuilderHelper> {
  protected:
    void SetUp() override;

    static aaudio_data_callback_result_t MyDataCallbackProc(
            AAudioStream *stream, void *userData, void *audioData, int32_t numFrames);
};

aaudio_data_callback_result_t AAudioInputStreamCallbackTest::MyDataCallbackProc(
        AAudioStream* /*stream*/, void *userData, void* /*audioData*/, int32_t numFrames) {
    AAudioCallbackTestData *myData = static_cast<AAudioCallbackTestData*>(userData);
    myData->updateFrameCount(numFrames);
    // No latency measurement as there is no API for querying capture position.
    myData->callbackCount++;
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AAudioInputStreamCallbackTest::SetUp() {
    aaudio_sharing_mode_t requestedSharingMode = std::get<0>(GetParam());
    mHelper.reset(new InputStreamBuilderHelper(requestedSharingMode));
    mHelper->initBuilder();

    int32_t framesPerDataCallback = std::get<1>(GetParam());
    mCbData.reset(new AAudioCallbackTestData());
    AAudioStreamBuilder_setErrorCallback(builder(), &MyErrorCallbackProc, mCbData.get());
    AAudioStreamBuilder_setDataCallback(builder(), &MyDataCallbackProc, mCbData.get());
    if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
        AAudioStreamBuilder_setFramesPerDataCallback(builder(), framesPerDataCallback);
    }

    mSetupSuccesful = false;
    mHelper->createAndVerifyStream(&mSetupSuccesful);
}

// Test Reading from an AAudioStream using a Callback
TEST_P(AAudioInputStreamCallbackTest, testRecording) {
    ASSERT_TRUE(mSetupSuccesful);

    // TODO Why does getDeviceId() always return 0?
    // EXPECT_NE(AAUDIO_DEVICE_UNSPECIFIED, AAudioStream_getDeviceId(stream));

    const int32_t framesPerDataCallback = std::get<1>(GetParam());
    const int32_t actualFramesPerDataCallback = AAudioStream_getFramesPerDataCallback(stream());
    if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
        ASSERT_EQ(framesPerDataCallback, actualFramesPerDataCallback);
    }

    mCbData->reset(actualFramesPerDataCallback);

    mHelper->startStream();
    sleep(2); // let the stream run

    ASSERT_EQ(mCbData->callbackError, AAUDIO_OK);
    ASSERT_GT(mCbData->callbackCount, 10);

    mHelper->stopStream();

    int32_t oldCallbackCount = mCbData->callbackCount;
    EXPECT_GT(oldCallbackCount, 10);
    sleep(1);
    EXPECT_EQ(oldCallbackCount, mCbData->callbackCount); // expect not advancing

    if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
        ASSERT_EQ(framesPerDataCallback, mCbData->actualFramesPerCallback);
    }

    ASSERT_EQ(mCbData->callbackError, AAUDIO_OK);
}

INSTANTIATE_TEST_CASE_P(SM, AAudioInputStreamCallbackTest,
        ::testing::Values(
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, AAUDIO_UNSPECIFIED),
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, 109),  // arbitrary prime number < 192
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, 223)), // arbitrary prime number > 192
        &getTestName);


class AAudioOutputStreamCallbackTest : public AAudioStreamCallbackTest<OutputStreamBuilderHelper> {
  protected:
    void SetUp() override;

    static aaudio_data_callback_result_t MyDataCallbackProc(
            AAudioStream *stream, void *userData, void *audioData, int32_t numFrames);
};

// Callback function that fills the audio output buffer.
aaudio_data_callback_result_t AAudioOutputStreamCallbackTest::MyDataCallbackProc(
        AAudioStream *stream, void *userData, void *audioData, int32_t numFrames) {
    int32_t samplesPerFrame = AAudioStream_getSamplesPerFrame(stream);
    int32_t numSamples = samplesPerFrame * numFrames;
    if (AAudioStream_getFormat(stream) == AAUDIO_FORMAT_PCM_I16) {
        int16_t *shortData = (int16_t *) audioData;
        for (int i = 0; i < numSamples; i++) *shortData++ = 0;
    } else if (AAudioStream_getFormat(stream) == AAUDIO_FORMAT_PCM_FLOAT) {
        float *floatData = (float *) audioData;
        for (int i = 0; i < numSamples; i++) *floatData++ = 0.0f;
    }

    AAudioCallbackTestData *myData = static_cast<AAudioCallbackTestData*>(userData);
    myData->updateFrameCount(numFrames);
    myData->updateLatency(measureLatency(stream));
    myData->callbackCount++;
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AAudioOutputStreamCallbackTest::SetUp() {
    aaudio_sharing_mode_t requestedSharingMode = std::get<0>(GetParam());
    mHelper.reset(new OutputStreamBuilderHelper(requestedSharingMode));
    mHelper->initBuilder();

    int32_t framesPerDataCallback = std::get<1>(GetParam());
    mCbData.reset(new AAudioCallbackTestData());
    AAudioStreamBuilder_setErrorCallback(builder(), &MyErrorCallbackProc, mCbData.get());
    AAudioStreamBuilder_setDataCallback(builder(), &MyDataCallbackProc, mCbData.get());
    if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
        AAudioStreamBuilder_setFramesPerDataCallback(builder(), framesPerDataCallback);
    }

    mSetupSuccesful = false;
    mHelper->createAndVerifyStream(&mSetupSuccesful);
}

// Test Writing to an AAudioStream using a Callback
TEST_P(AAudioOutputStreamCallbackTest, testPlayback) {
    ASSERT_TRUE(mSetupSuccesful);

    // TODO Why does getDeviceId() always return 0?
    // EXPECT_NE(AAUDIO_DEVICE_UNSPECIFIED, AAudioStream_getDeviceId(stream));

    const int32_t framesPerDataCallback = std::get<1>(GetParam());
    const int32_t actualFramesPerDataCallback = AAudioStream_getFramesPerDataCallback(stream());
    if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
        ASSERT_EQ(framesPerDataCallback, actualFramesPerDataCallback);
    }

    // Start/stop more than once to see if it fails after the first time.
    // Write some data and measure the rate to see if the timing is OK.
    for (int loopIndex = 0; loopIndex < 2; loopIndex++) {
        mCbData->reset(actualFramesPerDataCallback);

        mHelper->startStream();
        sleep(2); // let the stream run

        ASSERT_EQ(mCbData->callbackError, AAUDIO_OK);
        ASSERT_GT(mCbData->callbackCount, 10);

        // For more coverage, alternate pausing and stopping.
        if ((loopIndex & 1) == 0) {
            mHelper->pauseStream();
        } else {
            mHelper->stopStream();
        }

        int32_t oldCallbackCount = mCbData->callbackCount;
        EXPECT_GT(oldCallbackCount, 10);
        sleep(1);
        EXPECT_EQ(oldCallbackCount, mCbData->callbackCount); // expect not advancing

        if (framesPerDataCallback != AAUDIO_UNSPECIFIED) {
            ASSERT_EQ(framesPerDataCallback, mCbData->actualFramesPerCallback);
        }

        EXPECT_GE(mCbData->minLatency, 1);   // Absurdly low
        EXPECT_LE(mCbData->maxLatency, 300); // Absurdly high, should be < 30
                                               // Note that on some devices it's 200-something
        //printf("latency: %d, %d\n", mCbData->minLatency, mCbData->maxLatency);
    }

    ASSERT_EQ(mCbData->callbackError, AAUDIO_OK);
}

INSTANTIATE_TEST_CASE_P(SM, AAudioOutputStreamCallbackTest,
        ::testing::Values(
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, AAUDIO_UNSPECIFIED),
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, 109),  // arbitrary prime number < 192
                std::make_tuple(AAUDIO_SHARING_MODE_SHARED, 223)), // arbitrary prime number > 192
        &getTestName);
