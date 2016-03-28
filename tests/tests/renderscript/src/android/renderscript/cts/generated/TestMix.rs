/*
 * Copyright (C) 2016 The Android Open Source Project
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

// Don't edit this file!  It is auto-generated by frameworks/rs/api/generate.sh.

#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation gAllocInStop;
rs_allocation gAllocInFraction;

float __attribute__((kernel)) testMixFloatFloatFloatFloat(float inStart, unsigned int x) {
    float inStop = rsGetElementAt_float(gAllocInStop, x);
    float inFraction = rsGetElementAt_float(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float2 __attribute__((kernel)) testMixFloat2Float2Float2Float2(float2 inStart, unsigned int x) {
    float2 inStop = rsGetElementAt_float2(gAllocInStop, x);
    float2 inFraction = rsGetElementAt_float2(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float3 __attribute__((kernel)) testMixFloat3Float3Float3Float3(float3 inStart, unsigned int x) {
    float3 inStop = rsGetElementAt_float3(gAllocInStop, x);
    float3 inFraction = rsGetElementAt_float3(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float4 __attribute__((kernel)) testMixFloat4Float4Float4Float4(float4 inStart, unsigned int x) {
    float4 inStop = rsGetElementAt_float4(gAllocInStop, x);
    float4 inFraction = rsGetElementAt_float4(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half __attribute__((kernel)) testMixHalfHalfHalfHalf(half inStart, unsigned int x) {
    half inStop = rsGetElementAt_half(gAllocInStop, x);
    half inFraction = rsGetElementAt_half(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half2 __attribute__((kernel)) testMixHalf2Half2Half2Half2(half2 inStart, unsigned int x) {
    half2 inStop = rsGetElementAt_half2(gAllocInStop, x);
    half2 inFraction = rsGetElementAt_half2(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half3 __attribute__((kernel)) testMixHalf3Half3Half3Half3(half3 inStart, unsigned int x) {
    half3 inStop = rsGetElementAt_half3(gAllocInStop, x);
    half3 inFraction = rsGetElementAt_half3(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half4 __attribute__((kernel)) testMixHalf4Half4Half4Half4(half4 inStart, unsigned int x) {
    half4 inStop = rsGetElementAt_half4(gAllocInStop, x);
    half4 inFraction = rsGetElementAt_half4(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float2 __attribute__((kernel)) testMixFloat2Float2FloatFloat2(float2 inStart, unsigned int x) {
    float2 inStop = rsGetElementAt_float2(gAllocInStop, x);
    float inFraction = rsGetElementAt_float(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float3 __attribute__((kernel)) testMixFloat3Float3FloatFloat3(float3 inStart, unsigned int x) {
    float3 inStop = rsGetElementAt_float3(gAllocInStop, x);
    float inFraction = rsGetElementAt_float(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

float4 __attribute__((kernel)) testMixFloat4Float4FloatFloat4(float4 inStart, unsigned int x) {
    float4 inStop = rsGetElementAt_float4(gAllocInStop, x);
    float inFraction = rsGetElementAt_float(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half2 __attribute__((kernel)) testMixHalf2Half2HalfHalf2(half2 inStart, unsigned int x) {
    half2 inStop = rsGetElementAt_half2(gAllocInStop, x);
    half inFraction = rsGetElementAt_half(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half3 __attribute__((kernel)) testMixHalf3Half3HalfHalf3(half3 inStart, unsigned int x) {
    half3 inStop = rsGetElementAt_half3(gAllocInStop, x);
    half inFraction = rsGetElementAt_half(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}

half4 __attribute__((kernel)) testMixHalf4Half4HalfHalf4(half4 inStart, unsigned int x) {
    half4 inStop = rsGetElementAt_half4(gAllocInStop, x);
    half inFraction = rsGetElementAt_half(gAllocInFraction, x);
    return mix(inStart, inStop, inFraction);
}
