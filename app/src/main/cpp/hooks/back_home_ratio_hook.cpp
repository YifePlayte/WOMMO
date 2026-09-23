//
// WOMMO native hook: back-home ratio for the Flutter launcher.
//
// The legacy launcher seeded MIUI maml app icons with a "ratio" variable on
// the way back home (FancyDrawableCompat.updateRatio -> MamlUtils.updateVariable
// -> "ratio").  The value was the horizontal offset of the flying icon from the
// home icon, normalised by the device width:
//
//     ratio = (floatingCenterX - homeIconCenterX) / deviceWidth
//
// and maml themes consumed it as the landing animation's direction/speed seed.
// The Flutter launcher kept the back-home commands ("back_home_start" /
// "back_home_finish") but dropped the variable seeding, so the value stays at
// its default and theme icons lose their initial state.
//
// This hook restores the seed natively:
//
//   * FolmeAnimationDriver._updateAnimationProperties receives the closing
//     window's live geometry every frame; the center X carries the gesture's
//     direction and speed (a diagonal swipe drags the window left or right),
//     exactly like the legacy floating icon rect did.
//   * FolmeAnimationDriver._checkAndNotifyBackHome supplies the hero origin
//     (+0xd8, the home icon's left edge) and the icon width (+0xe8), so the
//     offset is measured against the icon centre.
//   * GestureAnimCalculator.getVisibleScreenWidth supplies the normalisation
//     width, so the ratio stays in the legacy [-1, 1] device-width units.
//   * librust_maml_sdk.so's send_command is intercepted; on "back_home_start"
//     it writes "ratio" through put_variable_number before the command is
//     forwarded, mirroring the legacy order (variable, then command).  The
//     seed is latched per gesture: every controller of one return sees the
//     same value.
//
// Both Dart functions are located by instruction fingerprint and hooked with a
// register-preserving trampoline (Dart AOT uses x15 as its frame pointer, so a
// plain C replacement would corrupt the caller's frame).  Everything fails
// closed: a missing fingerprint, an unreadable driver or an unknown
// put_variable_number argument layout leaves the stock behavior untouched.

#include "hooks.h"

#include "../log.h"
#include "../lsposed_hook_backend.h"

#include <dlfcn.h>
#include <inttypes.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

// Runtime logs are muted: hooks sit on hot paths and the log traffic is
// not worth the IO.  Uncomment the define below to re-enable them when
// debugging on device.
// #define WOMMO_RATIO_DEBUG
#ifdef WOMMO_RATIO_DEBUG
#define WOMMO_RATIO_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_RATIO_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_RATIO_LOGW(...) ((void)0)
#define WOMMO_RATIO_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

// ---------------------------------------------------------------------------
// Captured state
// ---------------------------------------------------------------------------

constexpr uint32_t kStateIdle = 0u;
constexpr uint32_t kStateReady = 1u;

double g_screen_width = 0.0;
double g_hero_x = 0.0;
double g_hero_width = 0.0;
double g_window_center_x = 0.0;
double g_window_width = 0.0;
uint32_t g_width_state = kStateIdle;
uint32_t g_hero_state = kStateIdle;
uint32_t g_window_state = kStateIdle;
uint64_t g_window_update_ms = 0u;

// The seed is latched once per back-home gesture so every maml controller of
// the same return sees the same value.
double g_gesture_ratio = 0.0;
uint32_t g_gesture_valid = 0u;
uint64_t g_gesture_start_ms = 0u;

uint32_t g_ratio_log_count = 0u;
uint32_t g_width_log_count = 0u;

// ---------------------------------------------------------------------------
// Dart heap readers
//
// Dart AOT loads every field at offset+7 (tagged access) and rebuilds full
// pointers from 32-bit words with `add reg, reg, x28, lsl #32`, so the readers
// below mirror both conventions.
// ---------------------------------------------------------------------------

bool ReadUnaligned(const void* address, void* output, size_t size) {
    if (address == nullptr || output == nullptr || size == 0u) return false;
    memcpy(output, address, size);
    return true;
}

bool ReadDouble(uintptr_t address, double* value) {
    if (address < 0x1000u || value == nullptr) return false;
    return ReadUnaligned(reinterpret_cast<const void*>(address), value,
                         sizeof(double));
}

bool ReadCompressedPointer(uintptr_t object, uint32_t field_offset,
                           uint64_t heap_base, uintptr_t* pointer) {
    if (object < 0x100000000ull || pointer == nullptr) return false;
    uint32_t low = 0u;
    const uintptr_t load = object + field_offset + 7u;
    if (!ReadUnaligned(reinterpret_cast<const void*>(load), &low, sizeof(low))) {
        return false;
    }
    const uintptr_t rebuilt =
            static_cast<uintptr_t>(low) | (heap_base << 32);
    if (rebuilt < 0x100000000ull) return false;
    *pointer = rebuilt;
    return true;
}

// ---------------------------------------------------------------------------
// Dart trampolines
// ---------------------------------------------------------------------------

extern "C" void WommoRecordVisibleScreenWidth(uint64_t self,
                                              uint64_t heap_base);
extern "C" void WommoRecordBackHomeDriver(uint64_t driver,
                                          uint64_t heap_base,
                                          double current);
extern "C" void WommoRecordAnimProps(uint64_t stack_args, double d0, double d1,
                                     double d2, double d3, double d4, double d5,
                                     double d6, double d7);
extern "C" void* g_get_visible_screen_width_original;
extern "C" void* g_check_and_notify_back_home_original;
extern "C" void* g_update_animation_properties_original;

void* g_get_visible_screen_width_original = nullptr;
void* g_check_and_notify_back_home_original = nullptr;
void* g_update_animation_properties_original = nullptr;

uint64_t MonotonicMillis() {
    struct timespec now {};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return static_cast<uint64_t>(now.tv_sec) * 1000u +
            static_cast<uint64_t>(now.tv_nsec) / 1000000u;
}

// Save the integer and floating argument registers, call the recorder, then
// restore every register (including x15, Dart's frame pointer) and continue to
// the original trampoline.
#define WOMMO_SAVE_ARGUMENTS()                       \
    "sub sp, sp, #0xe0\n"                            \
    "stp x0, x1, [sp, #0x00]\n"                      \
    "stp x2, x3, [sp, #0x10]\n"                      \
    "stp x4, x5, [sp, #0x20]\n"                      \
    "stp x6, x7, [sp, #0x30]\n"                      \
    "stp x8, x9, [sp, #0x40]\n"                      \
    "stp x10, x11, [sp, #0x50]\n"                    \
    "stp x12, x13, [sp, #0x60]\n"                    \
    "stp x14, x15, [sp, #0x70]\n"                    \
    "stp x16, x17, [sp, #0x80]\n"                    \
    "stp x18, x30, [sp, #0x90]\n"                    \
    "stp d0, d1, [sp, #0xa0]\n"                      \
    "stp d2, d3, [sp, #0xb0]\n"                      \
    "stp d4, d5, [sp, #0xc0]\n"                      \
    "stp d6, d7, [sp, #0xd0]\n"

#define WOMMO_RESTORE_ARGUMENTS()                    \
    "ldp d6, d7, [sp, #0xd0]\n"                      \
    "ldp d4, d5, [sp, #0xc0]\n"                      \
    "ldp d2, d3, [sp, #0xb0]\n"                      \
    "ldp d0, d1, [sp, #0xa0]\n"                      \
    "ldp x18, x30, [sp, #0x90]\n"                    \
    "ldp x16, x17, [sp, #0x80]\n"                    \
    "ldp x14, x15, [sp, #0x70]\n"                    \
    "ldp x12, x13, [sp, #0x60]\n"                    \
    "ldp x10, x11, [sp, #0x50]\n"                    \
    "ldp x8, x9, [sp, #0x40]\n"                      \
    "ldp x6, x7, [sp, #0x30]\n"                      \
    "ldp x4, x5, [sp, #0x20]\n"                      \
    "ldp x2, x3, [sp, #0x10]\n"                      \
    "ldp x0, x1, [sp, #0x00]\n"                      \
    "add sp, sp, #0xe0\n"

extern "C" __attribute__((naked)) void HookGetVisibleScreenWidth() {
    __asm__ volatile(
            ".inst 0xd503245f\n"  // bti c
            WOMMO_SAVE_ARGUMENTS()
            "mov x0, x1\n"
            "mov x1, x28\n"
            "bl WommoRecordVisibleScreenWidth\n"
            WOMMO_RESTORE_ARGUMENTS()
            "adrp x16, g_get_visible_screen_width_original\n"
            "ldr x16, [x16, :lo12:g_get_visible_screen_width_original]\n"
            "br x16\n");
}

extern "C" __attribute__((naked)) void HookUpdateAnimationProps() {
    __asm__ volatile(
            ".inst 0xd503245f\n"  // bti c
            WOMMO_SAVE_ARGUMENTS()
            "mov x0, x15\n"
            "bl WommoRecordAnimProps\n"
            WOMMO_RESTORE_ARGUMENTS()
            "adrp x16, g_update_animation_properties_original\n"
            "ldr x16, [x16, :lo12:g_update_animation_properties_original]\n"
            "br x16\n");
}

extern "C" __attribute__((naked)) void HookCheckAndNotifyBackHome() {
    __asm__ volatile(
            ".inst 0xd503245f\n"  // bti c
            WOMMO_SAVE_ARGUMENTS()
            "mov x0, x1\n"
            "mov x1, x28\n"
            "bl WommoRecordBackHomeDriver\n"
            WOMMO_RESTORE_ARGUMENTS()
            "adrp x16, g_check_and_notify_back_home_original\n"
            "ldr x16, [x16, :lo12:g_check_and_notify_back_home_original]\n"
            "br x16\n");
}

void WommoRecordBackHomeDriver(uint64_t driver, uint64_t heap_base,
                               double current) {
    (void)heap_base;
    (void)current;
    if (driver < 0x100000000ull) return;
    // The driver stores the hero origin at +0xd8 (x) and +0xe0 (y); the values
    // track the home icon's column/row in logical pixels.
    double hero_x = 0.0;
    double hero_y = 0.0;
    double hero_width = 0.0;
    if (!ReadDouble(static_cast<uintptr_t>(driver) + 0xd8u + 7u, &hero_x) ||
            !ReadDouble(static_cast<uintptr_t>(driver) + 0xe0u + 7u, &hero_y) ||
            !ReadDouble(static_cast<uintptr_t>(driver) + 0xe8u + 7u,
                        &hero_width) ||
            !(hero_x > -20000.0 && hero_x < 20000.0) ||
            !(hero_y > -20000.0 && hero_y < 20000.0) ||
            !(hero_width > 0.0 && hero_width < 2000.0)) {
        return;
    }
    __atomic_store(&g_hero_x, &hero_x, __ATOMIC_RELEASE);
    __atomic_store(&g_hero_width, &hero_width, __ATOMIC_RELEASE);
    __atomic_store_n(&g_hero_state, kStateReady, __ATOMIC_RELEASE);
    (void)current;
}

// _updateAnimationProperties receives the animated window values; d2 is the
// window center X and d1 the window Width.  The values carry the gesture's
// direction and velocity.
void WommoRecordAnimProps(uint64_t stack_args, double window_width,
                          double window_height, double window_x, double window_y,
                          double d4, double d5, double d6, double d7) {
    (void)stack_args;
    (void)window_height;
    (void)window_y;
    (void)d4;
    (void)d5;
    (void)d6;
    (void)d7;
    if (!(window_width > 0.0 && window_width < 20000.0) ||
            !(window_x > -20000.0 && window_x < 20000.0)) {
        return;
    }
    __atomic_store(&g_window_center_x, &window_x, __ATOMIC_RELEASE);
    __atomic_store(&g_window_width, &window_width, __ATOMIC_RELEASE);
    __atomic_store_n(&g_window_update_ms, MonotonicMillis(), __ATOMIC_RELEASE);
    __atomic_store_n(&g_window_state, kStateReady, __ATOMIC_RELEASE);
    static uint32_t log_count = 0u;
    const uint32_t count = __atomic_add_fetch(&log_count, 1u, __ATOMIC_RELAXED);
    if (count <= 2u) {
        WOMMO_RATIO_LOGW("back_home_ratio: window w=%.1f x=%.1f y=%.1f", window_width,
                   window_x, window_y);
    }
}

void WommoRecordVisibleScreenWidth(uint64_t self, uint64_t heap_base) {
    uintptr_t config = 0u;
    double width = 0.0;
    if (!ReadCompressedPointer(static_cast<uintptr_t>(self), 0x80u, heap_base,
                               &config) ||
            !ReadDouble(config + 7u, &width) ||
            !(width > 100.0 && width < 20000.0)) {
        return;
    }
    __atomic_store(&g_screen_width, &width, __ATOMIC_RELEASE);
    __atomic_store_n(&g_width_state, kStateReady, __ATOMIC_RELEASE);
    const uint32_t count = __atomic_add_fetch(&g_width_log_count, 1u,
                                              __ATOMIC_RELAXED);
    if (count <= 1u) {
        WOMMO_RATIO_LOGW("back_home_ratio: visible screen width %.1f", width);
    }
}

// ---------------------------------------------------------------------------
// Dart patch installation
// ---------------------------------------------------------------------------

// GestureAnimCalculator.getVisibleScreenWidth:
//   ldur w0, [x1, #0x87] ; add x0, x0, x28, lsl #32 ; ldur d0, [x0, #7] ; ret
constexpr uint32_t kVisibleScreenWidthPattern[] = {
    0xb8487020u, 0x8b1c8000u, 0xfc407000u, 0xd65f03c0u,
};
constexpr size_t kVisibleScreenWidthPatternLength =
        sizeof(kVisibleScreenWidthPattern) / sizeof(uint32_t);

// FolmeAnimationDriver._checkAndNotifyBackHome:
//   stp x29, x30, [x15, #-0x10]! ; mov x29, x15 ; sub x15, x15, #0x28
//   stur x1, [x29, #-8]          ; stur d0, [x29, #-0x10]
//   ldur w0, [x1, #0x87]         ; add x0, x0, x28, lsl #32
//   ldr x16, [x27, #0x40]
constexpr uint32_t kCheckNotifyPattern[] = {
    0xa9bf79fdu, 0xaa0f03fdu, 0xd100a1efu, 0xf81f83a1u,
    0xfc1f03a0u, 0xb8487020u, 0x8b1c8000u, 0xf9402370u,
};
constexpr size_t kCheckNotifyPatternLength =
        sizeof(kCheckNotifyPattern) / sizeof(uint32_t);

// FolmeAnimationDriver._updateAnimationProperties:
//   stp x29, x30, [x15, #-0x10]! ; mov x29, x15 ; sub x15, x15, #0x10
//   mov x4, x1                   ; stur x1, [x29, #-0x10]
//   ldur w6, [x4, #0x63]         ; add x6, x6, x28, lsl #32
//   stur x6, [x29, #-8]
constexpr uint32_t kUpdatePropsPattern[] = {
    0xa9bf79fdu, 0xaa0f03fdu, 0xd10041efu, 0xaa0103e4u,
    0xf81f03a1u, 0xb8463086u, 0x8b1c80c6u,
};
constexpr size_t kUpdatePropsPatternLength =
        sizeof(kUpdatePropsPattern) / sizeof(uint32_t);

constexpr size_t kHookPatchSize = 16u;

bool InstallDartHooks(const dart::Image& image) {
    if (g_get_visible_screen_width_original != nullptr &&
            g_check_and_notify_back_home_original != nullptr &&
            g_update_animation_properties_original != nullptr) {
        return true;
    }

    uintptr_t width_getter = 0u;
    uint32_t width_matches = 0u;
    if (!dart::FindUniqueWordPattern(image, kVisibleScreenWidthPattern,
                                     kVisibleScreenWidthPatternLength,
                                     &width_getter, &width_matches) ||
            !dart::RangeInExecutableSegment(image, width_getter,
                                            kHookPatchSize)) {
        WOMMO_RATIO_LOGW("back_home_ratio: screen width pattern rejected "
                   "(matches=%u), keeping stock behavior",
                   width_matches);
        return false;
    }

    uintptr_t check_notify = 0u;
    uint32_t check_matches = 0u;
    if (!dart::FindUniqueWordPattern(image, kCheckNotifyPattern,
                                     kCheckNotifyPatternLength, &check_notify,
                                     &check_matches) ||
            !dart::RangeInExecutableSegment(image, check_notify,
                                            kHookPatchSize)) {
        WOMMO_RATIO_LOGW("back_home_ratio: check-notify pattern rejected "
                   "(matches=%u), keeping stock behavior",
                   check_matches);
        return false;
    }

    if (g_get_visible_screen_width_original == nullptr) {
        if (!ProtectHookRange(width_getter, kHookPatchSize)) {
            WOMMO_RATIO_LOGE("back_home_ratio: cannot protect width hook page");
            return false;
        }
        void* backup = nullptr;
        if (InstallInlineHook(reinterpret_cast<void*>(width_getter),
                              reinterpret_cast<void*>(HookGetVisibleScreenWidth),
                              &backup) != kHookSuccess ||
                backup == nullptr) {
            WOMMO_RATIO_LOGE("back_home_ratio: failed to hook screen width getter");
            return false;
        }
        __atomic_store_n(&g_get_visible_screen_width_original, backup,
                         __ATOMIC_RELEASE);
        WOMMO_RATIO_LOGW("back_home_ratio: width hook at 0x%" PRIxPTR
                   " (file offset 0x%" PRIxPTR ")",
                   width_getter, width_getter - image.bias);
    }

    uintptr_t update_props = 0u;
    uint32_t props_matches = 0u;
    if (!dart::FindUniqueWordPattern(image, kUpdatePropsPattern,
                                     kUpdatePropsPatternLength, &update_props,
                                     &props_matches) ||
            !dart::RangeInExecutableSegment(image, update_props,
                                            kHookPatchSize)) {
        WOMMO_RATIO_LOGW("back_home_ratio: update props pattern rejected "
                   "(matches=%u), keeping stock behavior",
                   props_matches);
        return false;
    }

    if (g_update_animation_properties_original == nullptr) {
        if (!ProtectHookRange(update_props, kHookPatchSize)) {
            WOMMO_RATIO_LOGE("back_home_ratio: cannot protect props hook page");
            return false;
        }
        void* backup = nullptr;
        if (InstallInlineHook(reinterpret_cast<void*>(update_props),
                              reinterpret_cast<void*>(HookUpdateAnimationProps),
                              &backup) != kHookSuccess ||
                backup == nullptr) {
            WOMMO_RATIO_LOGE("back_home_ratio: failed to hook update properties");
            return false;
        }
        __atomic_store_n(&g_update_animation_properties_original, backup,
                         __ATOMIC_RELEASE);
        WOMMO_RATIO_LOGW("back_home_ratio: props hook at 0x%" PRIxPTR
                   " (file offset 0x%" PRIxPTR ")",
                   update_props, update_props - image.bias);
    }

    if (g_check_and_notify_back_home_original == nullptr) {
        if (!ProtectHookRange(check_notify, kHookPatchSize)) {
            WOMMO_RATIO_LOGE("back_home_ratio: cannot protect check-notify hook page");
            return false;
        }
        void* backup = nullptr;
        if (InstallInlineHook(
                    reinterpret_cast<void*>(check_notify),
                    reinterpret_cast<void*>(HookCheckAndNotifyBackHome),
                    &backup) != kHookSuccess ||
                backup == nullptr) {
            WOMMO_RATIO_LOGE("back_home_ratio: failed to hook "
                       "_checkAndNotifyBackHome");
            return false;
        }
        __atomic_store_n(&g_check_and_notify_back_home_original, backup,
                         __ATOMIC_RELEASE);
        WOMMO_RATIO_LOGW("back_home_ratio: check-notify hook at 0x%" PRIxPTR
                   " (file offset 0x%" PRIxPTR ")",
                   check_notify, check_notify - image.bias);
    }

    return true;
}

// ---------------------------------------------------------------------------
// librust_maml_sdk.so hooks
// ---------------------------------------------------------------------------

using SendCommandFn = uint64_t (*)(uint64_t, uint64_t, uint64_t, uint64_t,
                                   uint64_t, uint64_t, uint64_t, uint64_t);
using PutVariableNumberFn = uint64_t (*)(uint64_t, uint64_t, uint64_t, uint64_t,
                                         uint64_t, uint64_t, uint64_t, uint64_t,
                                         double, double);

SendCommandFn g_original_send_command = nullptr;
PutVariableNumberFn g_original_put_variable_number = nullptr;

constexpr char kBackHomeStartCommand[] = "back_home_start";
constexpr char kRatioName[] = "ratio";
constexpr size_t kRatioNameLength = sizeof(kRatioName) - 1u;

// 0 = unknown, 1 = (length, pointer), 2 = (pointer, length).
uint32_t g_put_variable_layout = 0u;
uint32_t g_put_variable_type = 0u;

constexpr uint64_t kPointerThreshold = 0x10000ull;

bool SplitPointerAndLength(uint64_t first, uint64_t second, uint64_t* pointer) {
    const bool first_is_pointer = first > kPointerThreshold;
    const bool second_is_pointer = second > kPointerThreshold;
    if (first_is_pointer == second_is_pointer) return false;
    *pointer = first_is_pointer ? first : second;
    return true;
}

void NotePutVariableNumber(uint64_t name_arg, uint64_t length_arg,
                           uint64_t type) {
    (void)type;
    static uint32_t log_count = 0u;
    const uint32_t count = __atomic_add_fetch(&log_count, 1u, __ATOMIC_RELAXED);
    if (count <= 4u) {
        WOMMO_RATIO_LOGW("back_home_ratio: put_variable_number first=%#" PRIx64
                   " second=%#" PRIx64 " type=%" PRIu64,
                   name_arg, length_arg, type & 3u);
    }
    if (__atomic_load_n(&g_put_variable_layout, __ATOMIC_ACQUIRE) != 0u) return;
    const bool name_arg_is_pointer = name_arg > kPointerThreshold;
    const bool length_arg_is_pointer = length_arg > kPointerThreshold;
    if (name_arg_is_pointer == length_arg_is_pointer) return;
    const uint32_t layout = name_arg_is_pointer ? 2u : 1u;
    __atomic_store_n(&g_put_variable_layout, layout, __ATOMIC_RELEASE);
}

void NotePutVariableType(uint64_t type) {
    if (__atomic_load_n(&g_put_variable_type, __ATOMIC_RELAXED) == 0u) {
        __atomic_store_n(&g_put_variable_type, (type & 1u), __ATOMIC_RELEASE);
    }
}

extern "C" uint64_t HookedPutVariableNumber(uint64_t id, uint64_t first,
                                            uint64_t second, uint64_t type,
                                            uint64_t a4, uint64_t a5,
                                            uint64_t a6, uint64_t a7,
                                            double value, double d1) {
    NotePutVariableNumber(first, second, type);
    NotePutVariableType(type);
    PutVariableNumberFn original =
            __atomic_load_n(&g_original_put_variable_number, __ATOMIC_ACQUIRE);
    if (original == nullptr) return 0u;
    return original(id, first, second, type, a4, a5, a6, a7, value, d1);
}

// The legacy seed is the flying icon's horizontal offset from the home icon,
// normalised by the device width.  The flying icon is the closing app window,
// so its live center (captured from the SF animation status) carries the
// gesture's direction and speed; the hero origin is the home icon.
bool ComputeRatio(double* ratio) {
    double width = 0.0;
    double hero_x = 0.0;
    double hero_width = 0.0;
    double window_x = 0.0;
    __atomic_load(&g_screen_width, &width, __ATOMIC_ACQUIRE);
    __atomic_load(&g_hero_x, &hero_x, __ATOMIC_ACQUIRE);
    __atomic_load(&g_hero_width, &hero_width, __ATOMIC_ACQUIRE);
    __atomic_load(&g_window_center_x, &window_x, __ATOMIC_ACQUIRE);
    const uint64_t update_ms =
            __atomic_load_n(&g_window_update_ms, __ATOMIC_ACQUIRE);
    if (!(width > 100.0) || !(hero_width > 1.0) || !(hero_width < 2000.0) ||
            __atomic_load_n(&g_hero_state, __ATOMIC_ACQUIRE) != kStateReady ||
            __atomic_load_n(&g_window_state, __ATOMIC_ACQUIRE) != kStateReady) {
        return false;
    }
    if (update_ms == 0u || MonotonicMillis() - update_ms > 100u) {
        return false;
    }
    // The hero origin is the icon's left edge; the legacy target is its centre.
    const double icon_center_x = hero_x + hero_width * 0.5;
    double value = (window_x - icon_center_x) / width;
    if (value > 1.0) value = 1.0;
    if (value < -1.0) value = -1.0;
    *ratio = value;
    return true;
}

void ApplyBackHomeRatio(uint64_t maml_id) {
    const uint32_t layout =
            __atomic_load_n(&g_put_variable_layout, __ATOMIC_ACQUIRE);
    PutVariableNumberFn put = __atomic_load_n(
            &g_original_put_variable_number, __ATOMIC_ACQUIRE);
    if (layout == 0u || put == nullptr) return;
    if (__atomic_load_n(&g_width_state, __ATOMIC_ACQUIRE) != kStateReady ||
            __atomic_load_n(&g_hero_state, __ATOMIC_ACQUIRE) != kStateReady) {
        return;
    }
    // Latch the seed once per gesture: all maml controllers of one return must
    // see the same initial value.
    const uint64_t now_ms = MonotonicMillis();
    const bool new_gesture =
            __atomic_load_n(&g_gesture_valid, __ATOMIC_ACQUIRE) == 0u ||
            now_ms - __atomic_load_n(&g_gesture_start_ms, __ATOMIC_ACQUIRE) >
                    400u;
    if (new_gesture) {
        double captured = 0.0;
        if (!ComputeRatio(&captured)) return;
        __atomic_store(&g_gesture_ratio, &captured, __ATOMIC_RELEASE);
        __atomic_store_n(&g_gesture_start_ms, now_ms, __ATOMIC_RELEASE);
        __atomic_store_n(&g_gesture_valid, 1u, __ATOMIC_RELEASE);
    }
    double ratio = 0.0;
    if (__atomic_load_n(&g_gesture_valid, __ATOMIC_ACQUIRE) == 1u) {
        __atomic_load(&g_gesture_ratio, &ratio, __ATOMIC_ACQUIRE);
    } else {
        return;
    }
    const uint64_t name = reinterpret_cast<uint64_t>(kRatioName);
    const uint64_t length = kRatioNameLength;
    const uint64_t type =
            __atomic_load_n(&g_put_variable_type, __ATOMIC_RELAXED);
    uint64_t result = 0u;
    if (layout == 1u) {
        result = put(maml_id, length, name, type, 0u, 0u, 0u, 0u, ratio, 0.0);
    } else {
        result = put(maml_id, name, length, type, 0u, 0u, 0u, 0u, ratio, 0.0);
    }
    (void)result;
    const uint32_t count = __atomic_add_fetch(&g_ratio_log_count, 1u,
                                              __ATOMIC_RELAXED);
    if (count <= 8u) {
        double width = 0.0;
        double hero_x = 0.0;
        double hero_width = 0.0;
        double window_x = 0.0;
        __atomic_load(&g_screen_width, &width, __ATOMIC_ACQUIRE);
        __atomic_load(&g_hero_x, &hero_x, __ATOMIC_ACQUIRE);
        __atomic_load(&g_hero_width, &hero_width, __ATOMIC_ACQUIRE);
        __atomic_load(&g_window_center_x, &window_x, __ATOMIC_ACQUIRE);
        WOMMO_RATIO_LOGW("back_home_ratio: id=%" PRIu64
                   " ratio=%.3f window_x=%.1f hero=%.1f+%.1f width=%.1f "
                   "result=%#" PRIx64,
                   maml_id, ratio, window_x, hero_x, hero_width, width, result);
    }
}

extern "C" uint64_t HookedSendCommand(uint64_t id, uint64_t first,
                                      uint64_t second, uint64_t a3, uint64_t a4,
                                      uint64_t a5, uint64_t a6, uint64_t a7) {
    uint64_t command_pointer = 0u;
    if (SplitPointerAndLength(first, second, &command_pointer)) {
        const char* command =
                reinterpret_cast<const char*>(command_pointer);
        if (strcmp(command, kBackHomeStartCommand) == 0) {
            ApplyBackHomeRatio(id);
        }
    }
    SendCommandFn original =
            __atomic_load_n(&g_original_send_command, __ATOMIC_ACQUIRE);
    if (original == nullptr) return 0u;
    return original(id, first, second, a3, a4, a5, a6, a7);
}

bool InstallMamlSdkHooks(void* library_handle) {
    if (g_original_send_command != nullptr &&
            g_original_put_variable_number != nullptr) {
        return true;
    }
    void* library = library_handle;
    if (library == nullptr) {
        library = dlopen("librust_maml_sdk.so", RTLD_NOLOAD | RTLD_NOW);
    }
    if (library == nullptr) {
        WOMMO_RATIO_LOGW("back_home_ratio: librust_maml_sdk.so not loaded, "
                   "keeping stock behavior");
        return false;
    }

    void* send_command = dlsym(library, "send_command");
    void* put_variable_number = dlsym(library, "put_variable_number");
    if (send_command == nullptr || put_variable_number == nullptr) {
        WOMMO_RATIO_LOGW("back_home_ratio: maml sdk symbols missing "
                   "(send=%p put=%p), keeping stock behavior",
                   send_command, put_variable_number);
        return false;
    }

    if (!ProtectHookRange(reinterpret_cast<uintptr_t>(send_command),
                          kHookPatchSize) ||
            !ProtectHookRange(reinterpret_cast<uintptr_t>(put_variable_number),
                              kHookPatchSize)) {
        WOMMO_RATIO_LOGE("back_home_ratio: cannot protect maml sdk hook pages");
        return false;
    }

    void* backup = nullptr;
    if (InstallInlineHook(send_command,
                          reinterpret_cast<void*>(HookedSendCommand),
                          &backup) != kHookSuccess ||
            backup == nullptr) {
        WOMMO_RATIO_LOGE("back_home_ratio: failed to hook send_command");
        return false;
    }
    __atomic_store_n(&g_original_send_command,
                     reinterpret_cast<SendCommandFn>(backup),
                     __ATOMIC_RELEASE);

    backup = nullptr;
    if (InstallInlineHook(put_variable_number,
                          reinterpret_cast<void*>(HookedPutVariableNumber),
                          &backup) != kHookSuccess ||
            backup == nullptr) {
        WOMMO_RATIO_LOGE("back_home_ratio: failed to hook put_variable_number");
        return false;
    }
    __atomic_store_n(&g_original_put_variable_number,
                     reinterpret_cast<PutVariableNumberFn>(backup),
                     __ATOMIC_RELEASE);

    WOMMO_RATIO_LOGW("back_home_ratio: maml sdk hooks installed");
    return true;
}

uint32_t g_install_state = 0u;

}  // namespace

bool InstallBackHomeRatioHook(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_install_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_install_state == 2u;
    }
    // The maml SDK is usually loaded after libapp.so, so the SDK part is
    // allowed to be deferred to the library-load callback; only the Dart part
    // decides whether the install failed.
    InstallMamlSdkHooks(nullptr);
    const bool dart_hooks = InstallDartHooks(image);
    __atomic_store_n(&g_install_state, dart_hooks ? 2u : 0u, __ATOMIC_RELEASE);
    return dart_hooks;
}

// The maml SDK is loaded lazily by the launcher, usually after libapp.so, so
// the library-load callback retries this part later.
bool InstallBackHomeRatioSdkHooks(void* library_handle) {
    return InstallMamlSdkHooks(library_handle);
}

}  // namespace wommo::hooks
