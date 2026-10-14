//
// WOMMO native hook: keep gamepad stick motion out of the touch pipeline.
//
// The shared Rust backend (hyper_os_flutter_backend, statically linked into
// each app) splits motion events on a single source bit (`tbnz wN, #1`).
// Touchscreen/mouse/stylus/touchpad (bit set) take the pointer path with
// per-index tool checks; joystick/gamepad/trackball (bit clear) fall through
// to dispatch_generic_motion_event, which reads getSource and discards the
// result, then converts the event to touch pointer packets unconditionally.
// A joystick MOVE (action 2, one pointer, pointerId 0, stick-derived X/Y)
// therefore becomes indistinguishable touch input downstream.
//
// Backend 1.7 (launcher libapp_launcher.so, weather libweather_app.so): both
// routers end in a tail `b dispatch_generic` after the function epilogue, so
// replacing that word with `ret` is stack-balanced and no caller reads the
// return value. Trackball/dpad motion is dropped as well; touch/mouse/stylus
// keep the stock path.
//
// Backend 3.9 (gallery libapp_gallery.so): the routers were refactored to
// `bl` a shared generic converter (8 call sites, all on the bit-clear
// branch, every caller overwrites the return value). The converter itself
// only drops unknown actions ("drop malformed generic motion event" is
// action-based, so joystick MOVE still converts). A single `ret` at the
// converter entry drops all 8 non-pointer paths at once; pointer-path calls
// go to a different function and are unaffected.
//

#include "hooks.h"

#include "../log.h"

#include <dlfcn.h>
#include <inttypes.h>
#include <string.h>

// #define WOMMO_JOYSTICK_TOUCH_GUARD_DEBUG
#ifdef WOMMO_JOYSTICK_TOUCH_GUARD_DEBUG
#define WOMMO_JOYSTICK_TOUCH_GUARD_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_JOYSTICK_TOUCH_GUARD_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_JOYSTICK_TOUCH_GUARD_LOGW(...) ((void)0)
#define WOMMO_JOYSTICK_TOUCH_GUARD_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

constexpr uint32_t kRet = 0xD65F03C0u;
constexpr uint32_t kAll = 0xFFFFFFFFu;

// Backend 1.7 engine router: whole 22-word body of
// Engine::dispatch_motion_event. Words 6, 14 and 21 are the PC-relative
// `bl getSource`, `b dispatch_generic` and `b dispatch_pointer`; the branch
// targets move between app builds. Word 14 is replaced with `ret`.
constexpr size_t kRouterPatternLength = 22u;
constexpr dart::PatternWord kRouterPattern[kRouterPatternLength] = {
    {0xa9be7bfdu, kAll},
    {0xa9014ff4u, kAll},
    {0x910003fdu, kAll},
    {0xaa0003f4u, kAll},
    {0xaa0103e0u, kAll},
    {0xaa0103f3u, kAll},
    {0x00000000u, 0u},
    {0x37080100u, kAll},
    {0xaa1403e0u, kAll},
    {0xaa1303e1u, kAll},
    {0xaa1f03e2u, kAll},
    {0x2a1f03e4u, kAll},
    {0xa9414ff4u, kAll},
    {0xa8c27bfdu, kAll},
    {0x00000000u, 0u},
    {0xaa1403e0u, kAll},
    {0xaa1303e1u, kAll},
    {0xaa1f03e2u, kAll},
    {0x2a1f03e4u, kAll},
    {0xa9414ff4u, kAll},
    {0xa8c27bfdu, kAll},
    {0x00000000u, 0u},
};

// Backend 1.7 window router tail: the same bit test selects the generic path;
// word 9 (`b dispatch_generic`) is replaced with `ret`. The preceding `tbnz`
// keeps this pattern distinct from the pointer-path tail.
constexpr size_t kWindowRouterPatternLength = 11u;
constexpr dart::PatternWord kWindowRouterPattern[kWindowRouterPatternLength] = {
    {0x37080140u, kAll},
    {0xaa1603e0u, kAll},
    {0xaa1503e1u, kAll},
    {0x52800022u, kAll},
    {0xaa1403e3u, kAll},
    {0x2a1303e4u, kAll},
    {0xa9424ff4u, kAll},
    {0xa94157f6u, kAll},
    {0xa8c37bfdu, kAll},
    {0x00000000u, 0u},
    {0xaa1603e0u, kAll},
};

// Backend 3.9 generic converter entry: 8 `bl` call sites (all on the
// bit-clear branch, every caller overwrites x0 afterwards) converge here.
// Word 0 is the frame-allocating `str`; replacing it with `ret` leaves the
// stack untouched and drops the event before any conversion runs.
constexpr size_t kGalleryGenericEntryPatternLength = 10u;
constexpr dart::PatternWord
    kGalleryGenericEntryPattern[kGalleryGenericEntryPatternLength] = {
        {0xf81a0ffdu, kAll},
        {0xa9016ffeu, kAll},
        {0xa90267fau, kAll},
        {0xa9035ff8u, kAll},
        {0xa90457f6u, kAll},
        {0xa9054ff4u, kAll},
        {0xd10b83ffu, kAll},
        {0xaa0003f3u, kAll},
        {0xaa0103e0u, kAll},
        {0x2a0403f7u, kAll},
};

constexpr char kAppEntrySymbol[] = "app_entry_point";
constexpr char kDataAppPrefix[] = "/data/app/";
// Launcher/weather libraries map straight out of the APK on some builds
// (base.apk!/...) and extracted under lib/arm64 on others; both exact forms
// are accepted, never a loose library-name match.
constexpr char kLauncherTail[] = "/base.apk!/lib/arm64-v8a/libapp_launcher.so";
constexpr char kLauncherExtractedTail[] = "/lib/arm64/libapp_launcher.so";
constexpr char kSystemHomePrefix[] = "/product/priv-app/MiuiHome/";
constexpr char kSystemLauncherTail[] =
        "/MiuiHome.apk!/lib/arm64-v8a/libapp_launcher.so";
constexpr char kWeatherTail[] = "/base.apk!/lib/arm64-v8a/libweather_app.so";
constexpr char kWeatherExtractedTail[] = "/lib/arm64/libweather_app.so";
constexpr char kGalleryTail[] =
        "/MIUIGallery.apk!/lib/arm64-v8a/libapp_gallery.so";
constexpr char kGalleryExtractedTail[] = "/lib/arm64/libapp_gallery.so";

bool StringsEqual(const char* left, const char* right) {
    if (left == nullptr || right == nullptr) return false;
    size_t index = 0u;
    while (left[index] != '\0' && right[index] != '\0') {
        if (left[index] != right[index]) return false;
        ++index;
    }
    return left[index] == right[index];
}

bool StartsWith(const char* value, const char* prefix) {
    if (value == nullptr || prefix == nullptr) return false;
    size_t index = 0u;
    while (prefix[index] != '\0') {
        if (value[index] != prefix[index]) return false;
        ++index;
    }
    return true;
}

bool EndsWith(const char* value, const char* suffix) {
    if (value == nullptr || suffix == nullptr) return false;
    const size_t value_length = strlen(value);
    const size_t suffix_length = strlen(suffix);
    if (suffix_length > value_length) return false;
    return StringsEqual(value + value_length - suffix_length, suffix);
}

bool IsBackend17Path(const char* path) {
    if (path == nullptr) return false;
    if (StartsWith(path, kDataAppPrefix) &&
            (EndsWith(path, kLauncherTail) ||
             EndsWith(path, kLauncherExtractedTail) ||
             EndsWith(path, kWeatherTail) ||
             EndsWith(path, kWeatherExtractedTail))) {
        return true;
    }
    return StartsWith(path, kSystemHomePrefix) &&
            EndsWith(path, kSystemLauncherTail);
}

bool IsGalleryPath(const char* path) {
    return path != nullptr && StartsWith(path, kDataAppPrefix) &&
            (EndsWith(path, kGalleryTail) ||
             EndsWith(path, kGalleryExtractedTail));
}

uint32_t g_backend17_state = 0u;
uint32_t g_gallery_state = 0u;

bool PatchOneWord(const dart::Image& image, const dart::PatternWord* pattern,
                  size_t length, size_t word, const char* what) {
    (void)what;
    uintptr_t match = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueMaskedPattern(image, pattern, length, &match,
                                       &matches) ||
            !dart::RangeInExecutableSegment(
                    image, match, length * sizeof(uint32_t))) {
        WOMMO_JOYSTICK_TOUCH_GUARD_LOGW("joystick_touch_guard: %s rejected "
                   "(matches=%u), keeping original behavior",
                   what, matches);
        return false;
    }
    const uintptr_t target = match + word * sizeof(uint32_t);
    const uint32_t patch[1] = {kRet};
    if (!dart::PatchCode(target, patch, 1u)) {
        WOMMO_JOYSTICK_TOUCH_GUARD_LOGE("joystick_touch_guard: patch %s failed",
                   what);
        return false;
    }
    WOMMO_JOYSTICK_TOUCH_GUARD_LOGW("joystick_touch_guard: %s patched at 0x%"
               PRIxPTR, what, target);
    return true;
}

bool AcquireAppEntryImage(void* library_handle, bool (*path_ok)(const char*),
                         wommo::dart::Image* image) {
    if (library_handle == nullptr || image == nullptr) return false;
    void* entry = dlsym(library_handle, kAppEntrySymbol);
    if (entry == nullptr) return false;
    Dl_info info{};
    if (dladdr(entry, &info) == 0 || info.dli_fbase == nullptr ||
            !path_ok(info.dli_fname)) {
        WOMMO_JOYSTICK_TOUCH_GUARD_LOGW("joystick_touch_guard: unexpected path %s",
                   info.dli_fname != nullptr ? info.dli_fname : "<null>");
        return false;
    }
    return wommo::dart::AcquireImageAtBase(
            reinterpret_cast<uintptr_t>(info.dli_fbase), image);
}

}  // namespace

bool InstallJoystickTouchGuardHooks(void* library_handle) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_backend17_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_backend17_state == 2u;
    }
    wommo::dart::Image image{};
    const bool ok =
            AcquireAppEntryImage(library_handle, IsBackend17Path, &image) &&
            PatchOneWord(image, kRouterPattern, kRouterPatternLength, 14u,
                         "engine-router") &&
            PatchOneWord(image, kWindowRouterPattern,
                         kWindowRouterPatternLength, 9u, "window-router");
    __atomic_store_n(&g_backend17_state, ok ? 2u : 0u, __ATOMIC_RELEASE);
    return ok;
}

bool InstallJoystickTouchGuardGalleryHooks(void* library_handle) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_gallery_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_gallery_state == 2u;
    }
    wommo::dart::Image image{};
    const bool ok =
            AcquireAppEntryImage(library_handle, IsGalleryPath, &image) &&
            PatchOneWord(image, kGalleryGenericEntryPattern,
                         kGalleryGenericEntryPatternLength, 0u,
                         "generic-entry");
    __atomic_store_n(&g_gallery_state, ok ? 2u : 0u, __ATOMIC_RELEASE);
    return ok;
}

}  // namespace wommo::hooks
