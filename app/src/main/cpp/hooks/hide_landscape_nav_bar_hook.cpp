// WOMMO native hook: hide the launcher-drawn gesture hint line (the "fake
// navigation bar") in landscape, matching the legacy Java hook
// HideLandscapeNavBar (RecentsContainer.hideFakeNavBarForHidingGestureLine).
//
// The Flutter launcher paints its own pill-shaped gesture line through
// _FakeNavPainter.paint; the painter is used by the FakeNavigationBar widget
// only.  Replacing the paint entry with a plain `ret` removes the visual while
// the gesture touch area (GestureRegionWidget) keeps working unchanged.
//
// The pattern below is the stable function body; the four wildcard words are
// pool-page loads whose offsets move between launcher builds.

#include "hooks.h"

#include "../log.h"

#include <inttypes.h>

// Runtime logs are muted: hooks sit on hot paths and the log traffic is
// not worth the IO.  Uncomment the define below to re-enable them when
// debugging on device.
// #define WOMMO_HIDE_LANDSCAPE_NAV_BAR_DEBUG
#ifdef WOMMO_HIDE_LANDSCAPE_NAV_BAR_DEBUG
#define WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGW(...) ((void)0)
#define WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

constexpr uint32_t kRet = 0xD65F03C0u;
constexpr uint32_t kAll = 0xFFFFFFFFu;

constexpr dart::PatternWord kFakeNavPainterPattern[] = {
    {0xA9BF79FDu, kAll},  // stp  x29, x30, [x15, #-0x10]!
    {0xAA0F03FDu, kAll},  // mov  x29, x15
    {0xD10101EFu, kAll},  // sub  x15, x15, #0x40
    {0x1E601003u, kAll},  // fmov d3, #2.0
    {0x00000000u, 0u},    // add  x17, x27, #page
    {0x00000000u, 0u},    // ldr  d2, [x17, #imm]
    {0x1E631001u, kAll},  // fmov d1, #6.0
    {0x00000000u, 0u},    // add  x17, x27, #page
    {0x00000000u, 0u},    // ldr  d0, [x17, #imm]
    {0xAA0103E0u, kAll},  // mov  x0, x1
    {0xAA0203E1u, kAll},  // mov  x1, x2
    {0xF81F83A2u, kAll},  // stur x2, [x29, #-8]
    {0xFC40F064u, kAll},  // ldur d4, [x3, #0xf]
    {0x1E613885u, kAll},  // fsub d5, d4, d1
    {0x1E6238A1u, kAll},  // fsub d1, d5, d2
    {0xFC1D03A1u, kAll},  // stur d1, [x29, #-0x30]
    {0xFC407064u, kAll},  // ldur d4, [x3, #7]
    {0x1E603885u, kAll},  // fsub d5, d4, d0
    {0x1E6318A4u, kAll},  // fdiv d4, d5, d3
    {0xFC1D83A4u, kAll},  // stur d4, [x29, #-0x28]
};

// 0 = not installed, 1 = installing, 2 = installed.
uint32_t g_state = 0u;

}  // namespace

bool InstallHideLandscapeNavBarHook(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_state == 2u;
    }

    uintptr_t paint = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueMaskedPattern(
                image, kFakeNavPainterPattern,
                sizeof(kFakeNavPainterPattern) / sizeof(kFakeNavPainterPattern[0]),
                &paint, &matches) ||
            !dart::RangeInExecutableSegment(image, paint, sizeof(uint32_t))) {
        WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGW("hide_landscape_nav_bar: fake nav painter pattern rejected "
                   "(matches=%u), keeping original behavior",
                   matches);
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    const uint32_t patch[1] = {kRet};
    if (!dart::PatchCode(paint, patch, 1u)) {
        WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGE("hide_landscape_nav_bar: failed to patch fake nav painter");
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    __atomic_store_n(&g_state, 2u, __ATOMIC_RELEASE);
    WOMMO_HIDE_LANDSCAPE_NAV_BAR_LOGW("hide_landscape_nav_bar: fake nav painter disabled at "
               "0x%" PRIxPTR " (file offset 0x%" PRIxPTR ")",
               paint, paint - image.bias);
    return true;
}

}  // namespace wommo::hooks
