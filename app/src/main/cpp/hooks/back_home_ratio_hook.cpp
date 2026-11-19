//
// WOMMO native hook: back-home ratio for the Flutter launcher.
//
// The legacy launcher seeded maml app icons with a "ratio" variable right
// before it fired the back-home maml commands:
//
//     ratio = (flyingIconCenterX - homeIconCenterX) / deviceWidth
//
// computed once when the return gesture passed half of its progress, then
// clamped to [-1, 1].  FancyDrawableCompat.updateRatio forwarded it through
// MamlUtils.updateVariable(root, "ratio", value), immediately before
// MamlUtils.notifyBackHome("back_home_start"), so the theme icon started its
// landing animation from the direction and speed the floating icon had.
//
// The Flutter launcher kept the same notification point: onFrame() drives
// FolmeAnimationDriver, _checkAndNotifyBackHome turns the driver's progress pair
// into a 0..1 value and calls MamlIconCache.notifyBackHome once that value
// passes 0.5.  notifyBackHome sends "back_home_start" / "back_home_finish"
// through MamlController.sendCommand, but nothing writes the "ratio" variable
// any more, so the maml landing animation starts from its default state.
//
// This hook restores the seed:
//
//   * FolmeAnimationDriver._checkAndNotifyBackHome runs on every frame of the
//     return gesture and owns the receiver, so the driver can be read there.
//   * The flying-icon rect of the current frame is the rect of the driver's
//     flight item: driver[0x4] -> [0x1c].  The rect the flight lands on is the
//     field popOriginTargetRect returns, and its offset is taken from that
//     getter's fingerprint because it moved between launcher builds (8.01.02:
//     0xa4, 8.01.03: 0xbc).  These are the same rects
//     _getSfCurrentRectOrHeroRect resolves and _updateAnimationProperties paints
//     every frame.
//   * GestureAnimCalculator.getVisibleScreenWidth supplies the normalisation
//     width.  All three values are logical pixels, so the ratio is the legacy
//     device-width fraction without any pixel-ratio conversion.
//   * librust_maml_sdk.so's send_command is intercepted; on "back_home_start"
//     it writes "ratio" through put_variable_number before the command is
//     forwarded, mirroring the legacy order (variable, then command).
//
// _checkAndNotifyBackHome is located by instruction fingerprint and hooked with
// a register-preserving trampoline (Dart AOT uses x15 as its frame pointer, so a
// plain C replacement would corrupt the caller's frame).  Everything fails
// closed: a missing fingerprint, an unreadable driver or an unknown
// put_variable_number argument layout leaves the stock behavior untouched.

#include "hooks.h"

#include "../log.h"
#include <nhk/madvise_guard.h>
#include <nhk/native_api.h>

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

// FolmeAnimationDriver instance fields.  The flight item holds the live
// flying-icon rect; the target rect field is derived at install time from
// popOriginTargetRect.  Both are logical pixels, like the visible screen width.
constexpr uint32_t kDriverFlightItemOffset = 0x04u;
constexpr uint32_t kFlightItemRectOffset = 0x1cu;
// Filled from the popOriginTargetRect fingerprint at install time; the field
// moved between launcher builds (8.01.02 reads 0xa4, 8.01.03 reads 0xbc).
uint32_t g_target_rect_offset = 0u;
// GestureAnimCalculator.visibleScreenWidth (logical pixels).
constexpr uint32_t kCalculatorWidthOffset = 0x80u;

// The rects must come from the frame that notified the maml icons.
constexpr uint64_t kFrameFreshnessMs = 120u;

double g_screen_width = 0.0;
uint32_t g_width_state = kStateIdle;

double g_flight_left = 0.0;
double g_flight_top = 0.0;
double g_flight_right = 0.0;
double g_flight_bottom = 0.0;
uint32_t g_flight_state = kStateIdle;
uint64_t g_flight_update_ms = 0u;

double g_target_left = 0.0;
double g_target_top = 0.0;
double g_target_right = 0.0;
double g_target_bottom = 0.0;
uint32_t g_target_state = kStateIdle;
uint64_t g_target_update_ms = 0u;

#ifdef WOMMO_RATIO_DEBUG
uint32_t g_ratio_log_count = 0u;
uint32_t g_frame_log_count = 0u;
#endif

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

bool ReadRect(uintptr_t object, uint32_t field_offset, uint64_t heap_base,
              double* left, double* top, double* right, double* bottom) {
    uintptr_t rect = 0u;
    return ReadCompressedPointer(object, field_offset, heap_base, &rect) &&
            ReadDouble(rect + 7u, left) && ReadDouble(rect + 15u, top) &&
            ReadDouble(rect + 23u, right) && ReadDouble(rect + 31u, bottom);
}

bool RectLooksSane(double left, double top, double right, double bottom) {
    return left > -20000.0 && left < 20000.0 && top > -20000.0 &&
            top < 20000.0 && right > left && right < 20000.0 &&
            bottom > top && bottom < 20000.0;
}

// ---------------------------------------------------------------------------
// Dart trampolines
// ---------------------------------------------------------------------------

extern "C" void WommoRecordVisibleScreenWidth(uint64_t self,
                                              uint64_t heap_base);
extern "C" void WommoRecordBackHomeDriver(uint64_t driver,
                                          uint64_t heap_base,
                                          double progress);
extern "C" void* g_get_visible_screen_width_original;
extern "C" void* g_check_and_notify_back_home_original;

void* g_get_visible_screen_width_original = nullptr;
void* g_check_and_notify_back_home_original = nullptr;

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

// _checkAndNotifyBackHome(driver, driver, state, progress) runs on every frame
// of the return gesture.  The flying-icon rect lives on the driver's flight
// item and the landing rect on the driver; both are refreshed before the
// launcher's own notify pass in the same call.
void WommoRecordBackHomeDriver(uint64_t driver, uint64_t heap_base,
                               double progress) {
    (void)progress;
    if (driver < 0x100000000ull) return;
    const uint64_t now_ms = MonotonicMillis();

    double flight_left = 0.0;
    double flight_top = 0.0;
    double flight_right = 0.0;
    double flight_bottom = 0.0;
    uintptr_t flight_item = 0u;
    const bool flight_valid =
            ReadCompressedPointer(static_cast<uintptr_t>(driver),
                                  kDriverFlightItemOffset, heap_base,
                                  &flight_item) &&
            ReadRect(flight_item, kFlightItemRectOffset, heap_base, &flight_left,
                     &flight_top, &flight_right, &flight_bottom) &&
            RectLooksSane(flight_left, flight_top, flight_right, flight_bottom);

    double target_left = 0.0;
    double target_top = 0.0;
    double target_right = 0.0;
    double target_bottom = 0.0;
    const bool target_valid =
            g_target_rect_offset != 0u &&
            ReadRect(static_cast<uintptr_t>(driver), g_target_rect_offset,
                     heap_base, &target_left, &target_top, &target_right,
                     &target_bottom) &&
            RectLooksSane(target_left, target_top, target_right, target_bottom);

    if (flight_valid) {
        __atomic_store(&g_flight_left, &flight_left, __ATOMIC_RELEASE);
        __atomic_store(&g_flight_top, &flight_top, __ATOMIC_RELEASE);
        __atomic_store(&g_flight_right, &flight_right, __ATOMIC_RELEASE);
        __atomic_store(&g_flight_bottom, &flight_bottom, __ATOMIC_RELEASE);
        __atomic_store_n(&g_flight_update_ms, now_ms, __ATOMIC_RELEASE);
        __atomic_store_n(&g_flight_state, kStateReady, __ATOMIC_RELEASE);
    }
    if (target_valid) {
        __atomic_store(&g_target_left, &target_left, __ATOMIC_RELEASE);
        __atomic_store(&g_target_top, &target_top, __ATOMIC_RELEASE);
        __atomic_store(&g_target_right, &target_right, __ATOMIC_RELEASE);
        __atomic_store(&g_target_bottom, &target_bottom, __ATOMIC_RELEASE);
        __atomic_store_n(&g_target_update_ms, now_ms, __ATOMIC_RELEASE);
        __atomic_store_n(&g_target_state, kStateReady, __ATOMIC_RELEASE);
    }
#ifdef WOMMO_RATIO_DEBUG
    const uint32_t count = __atomic_add_fetch(&g_frame_log_count, 1u,
                                              __ATOMIC_RELAXED);
    if (count <= 200u) {
        WOMMO_RATIO_LOGW("back_home_ratio: frame p=%.3f flight=%.1f..%.1f "
                         "target=%.1f..%.1f valid=%u/%u",
                         progress, flight_left, flight_right, target_left,
                         target_right, flight_valid ? 1u : 0u,
                         target_valid ? 1u : 0u);
    }
#endif
}

void WommoRecordVisibleScreenWidth(uint64_t self, uint64_t heap_base) {
    uintptr_t config = 0u;
    double width = 0.0;
    if (!ReadCompressedPointer(static_cast<uintptr_t>(self),
                               kCalculatorWidthOffset, heap_base, &config) ||
            !ReadDouble(config + 7u, &width) ||
            !(width > 100.0 && width < 20000.0)) {
        return;
    }
    __atomic_store(&g_screen_width, &width, __ATOMIC_RELEASE);
    __atomic_store_n(&g_width_state, kStateReady, __ATOMIC_RELEASE);
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
//   stur x1, [x29, #-8]          ; stur d0, [x29, #-16]
//   ldur w0, [x1, #0x87]         ; add x0, x0, x28, lsl #32
//   ldr x16, [x27, #0x40]
//
// The tagged load of the state field is the only word that moves between
// launcher builds (8.01.03.308 reads #0x8f), so its imm9 is wildcarded; the
// frame layout around it stays exact so an unrelated function cannot match.
constexpr uint32_t kCheckNotifyImmMask = 0xffe00fffu;
constexpr nhk::dart::PatternWord kCheckNotifyPattern[] = {
    {0xa9bf79fdu, 0xffffffffu},
    {0xaa0f03fdu, 0xffffffffu},
    {0xd100a1efu, 0xffffffffu},
    {0xf81f83a1u, 0xffffffffu},
    {0xfc1f03a0u, 0xffffffffu},
    {0xb8487020u, kCheckNotifyImmMask},
    {0x8b1c8000u, 0xffffffffu},
    {0xf9402370u, 0xffffffffu},
};
constexpr size_t kCheckNotifyPatternLength =
        sizeof(kCheckNotifyPattern) / sizeof(kCheckNotifyPattern[0]);

// FolmeAnimationDriver.popOriginTargetRect: the getter that returns the rect
// the current flight lands on.  Its body loads the target field from the
// receiver, so the immediate in word 4 is the field this build stores it in.
// Only the immediates are wildcarded; the tail (`ldur x1, [x29, #-8]` after the
// null check) separates it from the two sibling getters that share the prologue.
constexpr uint32_t kTargetRectFieldMask = 0xffe00fffu;
constexpr uint32_t kPoolLoadMask = 0xffc0001fu;
constexpr uint32_t kBranchMask = 0xff00001fu;
constexpr uint32_t kBranchLinkMask = 0xfc000000u;
constexpr nhk::dart::PatternWord kTargetRectPattern[] = {
    {0xa9bf79fdu, 0xffffffffu},
    {0xaa0f03fdu, 0xffffffffu},
    {0xd10081efu, 0xffffffffu},
    {0xf81f03a1u, 0xffffffffu},
    {0xb84c3020u, kTargetRectFieldMask},
    {0x8b1c8000u, 0xffffffffu},
    {0xf81f83a0u, 0xffffffffu},
    {0xf95d1370u, kPoolLoadMask},
    {0x6b10001fu, 0xffffffffu},
    {0x54000300u, kBranchMask},
    {0xf95cfb70u, kPoolLoadMask},
    {0xf95cfb7eu, kPoolLoadMask},
    {0xa90041feu, 0xffffffffu},
    {0x94000000u, kBranchLinkMask},
    {0x00000000u, 0x00000000u},
    {0xf85f83a1u, 0xffffffffu},
};
constexpr size_t kTargetRectPatternLength =
        sizeof(kTargetRectPattern) / sizeof(kTargetRectPattern[0]);

constexpr size_t kHookPatchSize = 16u;

bool InstallDartHooks(const dart::Image& image) {
    if (g_get_visible_screen_width_original != nullptr &&
            g_check_and_notify_back_home_original != nullptr) {
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
    if (!dart::FindUniqueMaskedPattern(image, kCheckNotifyPattern,
                                       kCheckNotifyPatternLength, &check_notify,
                                       &check_matches) ||
            !dart::RangeInExecutableSegment(image, check_notify,
                                            kHookPatchSize)) {
        WOMMO_RATIO_LOGW("back_home_ratio: check-notify pattern rejected "
                   "(matches=%u), keeping stock behavior",
                   check_matches);
        return false;
    }

    uintptr_t target_getter = 0u;
    uint32_t target_matches = 0u;
    if (!dart::FindUniqueMaskedPattern(image, kTargetRectPattern,
                                       kTargetRectPatternLength, &target_getter,
                                       &target_matches) ||
            !dart::RangeInExecutableSegment(image, target_getter,
                                            kHookPatchSize)) {
        WOMMO_RATIO_LOGW("back_home_ratio: target rect pattern rejected "
                   "(matches=%u), keeping stock behavior",
                   target_matches);
        return false;
    }
    const uint32_t target_field =
            ((*reinterpret_cast<const uint32_t*>(target_getter + 16u) >> 12) &
                    0x1ffu) -
            7u;
    if (target_field < 0x40u || target_field > 0x400u) {
        WOMMO_RATIO_LOGW("back_home_ratio: target rect field %#x rejected",
                         target_field);
        return false;
    }
    __atomic_store_n(&g_target_rect_offset, target_field, __ATOMIC_RELEASE);
    WOMMO_RATIO_LOGW("back_home_ratio: target rect field at +%#x",
                     target_field);

    if (g_get_visible_screen_width_original == nullptr) {
        if (!nhk::ProtectHookRange(width_getter, kHookPatchSize)) {
            WOMMO_RATIO_LOGE("back_home_ratio: cannot protect width hook page");
            return false;
        }
        void* backup = nullptr;
        if (nhk::InstallInlineHook(reinterpret_cast<void*>(width_getter),
                              reinterpret_cast<void*>(HookGetVisibleScreenWidth),
                              &backup) != nhk::kHookSuccess ||
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

    if (g_check_and_notify_back_home_original == nullptr) {
        if (!nhk::ProtectHookRange(check_notify, kHookPatchSize)) {
            WOMMO_RATIO_LOGE("back_home_ratio: cannot protect check-notify hook page");
            return false;
        }
        void* backup = nullptr;
        if (nhk::InstallInlineHook(
                    reinterpret_cast<void*>(check_notify),
                    reinterpret_cast<void*>(HookCheckAndNotifyBackHome),
                    &backup) != nhk::kHookSuccess ||
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
constexpr size_t kBackHomeStartCommandLength =
        sizeof(kBackHomeStartCommand) - 1u;
constexpr char kRatioName[] = "ratio";
constexpr size_t kRatioNameLength = sizeof(kRatioName) - 1u;

// 1 = (length, pointer), 2 = (pointer, length).  The shipped
// librust_maml_sdk.so takes the length in x1 and the name pointer in x2, so the
// default is already usable before the launcher pushes any widget variable;
// an observation from the launcher's own calls corrects it if a build differs.
uint32_t g_put_variable_layout = 1u;
uint32_t g_put_variable_type = 0u;

constexpr uint64_t kPointerThreshold = 0x10000ull;

bool SplitPointerAndLength(uint64_t first, uint64_t second, uint64_t* pointer,
                           uint64_t* length) {
    const bool first_is_pointer = first > kPointerThreshold;
    const bool second_is_pointer = second > kPointerThreshold;
    if (first_is_pointer == second_is_pointer) return false;
    *pointer = first_is_pointer ? first : second;
    *length = first_is_pointer ? second : first;
    return true;
}

void NotePutVariableNumber(uint64_t name_arg, uint64_t length_arg,
                           uint64_t type) {
    (void)type;
    const bool name_arg_is_pointer = name_arg > kPointerThreshold;
    const bool length_arg_is_pointer = length_arg > kPointerThreshold;
    if (name_arg_is_pointer == length_arg_is_pointer) return;
    const uint32_t layout = name_arg_is_pointer ? 2u : 1u;
    const uint32_t previous =
            __atomic_exchange_n(&g_put_variable_layout, layout, __ATOMIC_ACQ_REL);
    if (previous != layout) {
        WOMMO_RATIO_LOGW("back_home_ratio: put_variable_number layout %u -> %u "
                         "type=%" PRIu64, previous, layout, type);
    }
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
// normalised by the device width.  The launcher notifies the maml icons at half
// of the return, so the live rect read here is the flying icon.
bool ComputeRatio(double* ratio) {
    double screen_width = 0.0;
    double flight_left = 0.0;
    double flight_right = 0.0;
    double target_left = 0.0;
    double target_right = 0.0;
    __atomic_load(&g_screen_width, &screen_width, __ATOMIC_ACQUIRE);
    __atomic_load(&g_flight_left, &flight_left, __ATOMIC_ACQUIRE);
    __atomic_load(&g_flight_right, &flight_right, __ATOMIC_ACQUIRE);
    __atomic_load(&g_target_left, &target_left, __ATOMIC_ACQUIRE);
    __atomic_load(&g_target_right, &target_right, __ATOMIC_ACQUIRE);
    const uint64_t now_ms = MonotonicMillis();
    if (__atomic_load_n(&g_width_state, __ATOMIC_ACQUIRE) != kStateReady ||
            __atomic_load_n(&g_flight_state, __ATOMIC_ACQUIRE) != kStateReady ||
            __atomic_load_n(&g_target_state, __ATOMIC_ACQUIRE) != kStateReady) {
        return false;
    }
    if (now_ms - __atomic_load_n(&g_flight_update_ms, __ATOMIC_ACQUIRE) >
                    kFrameFreshnessMs ||
            now_ms - __atomic_load_n(&g_target_update_ms, __ATOMIC_ACQUIRE) >
                    kFrameFreshnessMs) {
        return false;
    }
    if (!(screen_width > 100.0) || !(flight_right > flight_left) ||
            !(target_right > target_left)) {
        return false;
    }
    const double flight_center = (flight_left + flight_right) * 0.5;
    const double target_center = (target_left + target_right) * 0.5;
    double value = (flight_center - target_center) / screen_width;
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
    if (put == nullptr) return;
    double ratio = 0.0;
    if (!ComputeRatio(&ratio)) {
        double screen_width = 0.0;
        double flight_left = 0.0;
        double flight_right = 0.0;
        double target_left = 0.0;
        double target_right = 0.0;
        __atomic_load(&g_screen_width, &screen_width, __ATOMIC_ACQUIRE);
        __atomic_load(&g_flight_left, &flight_left, __ATOMIC_ACQUIRE);
        __atomic_load(&g_flight_right, &flight_right, __ATOMIC_ACQUIRE);
        __atomic_load(&g_target_left, &target_left, __ATOMIC_ACQUIRE);
        __atomic_load(&g_target_right, &target_right, __ATOMIC_ACQUIRE);
        WOMMO_RATIO_LOGW("back_home_ratio: no seed flight=%.1f..%.1f "
                         "target=%.1f..%.1f screen=%.1f",
                         flight_left, flight_right, target_left, target_right,
                         screen_width);
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
#ifdef WOMMO_RATIO_DEBUG
    const uint32_t count = __atomic_add_fetch(&g_ratio_log_count, 1u,
                                              __ATOMIC_RELAXED);
    if (count <= 8u) {
        double screen_width = 0.0;
        double flight_left = 0.0;
        double flight_right = 0.0;
        double target_left = 0.0;
        double target_right = 0.0;
        __atomic_load(&g_screen_width, &screen_width, __ATOMIC_ACQUIRE);
        __atomic_load(&g_flight_left, &flight_left, __ATOMIC_ACQUIRE);
        __atomic_load(&g_flight_right, &flight_right, __ATOMIC_ACQUIRE);
        __atomic_load(&g_target_left, &target_left, __ATOMIC_ACQUIRE);
        __atomic_load(&g_target_right, &target_right, __ATOMIC_ACQUIRE);
        WOMMO_RATIO_LOGW("back_home_ratio: id=%" PRIu64
                         " ratio=%.4f flight=%.1f..%.1f target=%.1f..%.1f "
                         "screen=%.1f layout=%u result=%#" PRIx64,
                         maml_id, ratio, flight_left, flight_right, target_left,
                         target_right, screen_width, layout, result);
    }
#endif
}

extern "C" uint64_t HookedSendCommand(uint64_t id, uint64_t first,
                                      uint64_t second, uint64_t a3, uint64_t a4,
                                      uint64_t a5, uint64_t a6, uint64_t a7) {
    uint64_t command_pointer = 0u;
    uint64_t command_length = 0u;
    if (SplitPointerAndLength(first, second, &command_pointer,
                              &command_length) &&
            command_length == kBackHomeStartCommandLength &&
            memcmp(reinterpret_cast<const void*>(command_pointer),
                   kBackHomeStartCommand, kBackHomeStartCommandLength) == 0) {
        ApplyBackHomeRatio(id);
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

    if (!nhk::ProtectHookRange(reinterpret_cast<uintptr_t>(send_command),
                          kHookPatchSize) ||
            !nhk::ProtectHookRange(reinterpret_cast<uintptr_t>(put_variable_number),
                              kHookPatchSize)) {
        WOMMO_RATIO_LOGE("back_home_ratio: cannot protect maml sdk hook pages");
        return false;
    }

    void* backup = nullptr;
    if (nhk::InstallInlineHook(send_command,
                          reinterpret_cast<void*>(HookedSendCommand),
                          &backup) != nhk::kHookSuccess ||
            backup == nullptr) {
        WOMMO_RATIO_LOGE("back_home_ratio: failed to hook send_command");
        return false;
    }
    __atomic_store_n(&g_original_send_command,
                     reinterpret_cast<SendCommandFn>(backup),
                     __ATOMIC_RELEASE);

    backup = nullptr;
    if (nhk::InstallInlineHook(put_variable_number,
                          reinterpret_cast<void*>(HookedPutVariableNumber),
                          &backup) != nhk::kHookSuccess ||
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
