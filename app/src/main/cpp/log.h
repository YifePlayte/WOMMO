//
// WOMMO native hook: shared logging helpers.
//
// HyperOS logd filters INFO/DEBUG messages from the launcher process, so all
// diagnostics must go through WARN or ERROR.

#pragma once

#include <android/log.h>

namespace wommo {

constexpr char kLogTag[] = "WommoNative";
constexpr int kLogLevel = ANDROID_LOG_WARN;

}  // namespace wommo

#define WOMMO_LOGW(...) \
    __android_log_print(::wommo::kLogLevel, ::wommo::kLogTag, __VA_ARGS__)
#define WOMMO_LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, ::wommo::kLogTag, __VA_ARGS__)
