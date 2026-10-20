// WOMMO native hook: always disable the app icon highlight (bloom stroke).
//
// The launcher paints a directional rim light along the icon's rounded-rect
// outline through the engine bloom-stroke paint path (IconPainter.paint ->
// IconPainter.convertGlassStrokeTokenToStrokeConfig -> BloomStrokeConfig.applyTo
// -> PaintCompat|applyBloomStroke).  Its master switch is
// IconLightConfig.isHighLightEnabled, which is only true for default-theme
// icons while the system advanced-textures material switch is on, and it also
// gates the icon light wrappers' bloom fade-in.
//
// The patch makes isHighLightEnabled return false unconditionally by jumping
// to the function's own return-false block, so the stroke config is emitted
// with enabled=false and the engine draws no rim light on icons.  Icon-scoped
// by construction: the function's only callers are the icon stroke config and
// the icon light wrappers; other blooms (search bar, folder glass, maml)
// follow different gates and stay stock.

#include "hooks.h"

#include "../log.h"

#include <inttypes.h>

// Runtime logs are muted: hooks sit on hot paths and the log traffic is
// not worth the IO.  Uncomment the define below to re-enable them when
// debugging on device.
// #define WOMMO_DISABLE_ICON_HIGHLIGHT_DEBUG
#ifdef WOMMO_DISABLE_ICON_HIGHLIGHT_DEBUG
#define WOMMO_DISABLE_ICON_HIGHLIGHT_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_DISABLE_ICON_HIGHLIGHT_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_DISABLE_ICON_HIGHLIGHT_LOGW(...) ((void)0)
#define WOMMO_DISABLE_ICON_HIGHLIGHT_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

constexpr uint32_t kAll = 0xFFFFFFFFu;

// IconLightConfig.isHighLightEnabled.  The function first asks
// BlurUtilities.isMaterialDisabled and otherwise returns a config flag; the
// tail is the shared Dart epilogue behind the return-false block
// (add x0, x22, #0x30 ; mov x15, x29 ; ldp ; ret).  Only the object-pool
// loads, the static-field offsets and the helper call offsets vary between
// builds, so they are wildcarded and the surrounding instructions stay exact:
// that also proves the return-false block still sits 24 words below the
// patched call.
constexpr dart::PatternWord kIsHighLightEnabledPattern[] = {
    {0xA9BF79FDu, kAll},  // stp  x29, x30, [x15, #-0x10]!
    {0xAA0F03FDu, kAll},  // mov  x29, x15
    {0xD10021EFu, kAll},  // sub  x15, x15, #8
    {0x94000000u, 0xFC000000u},  // bl   BlurUtilities.isMaterialDisabled
    {0x362002E0u, kAll},  // tbz  w0, #4, <return-false block>
    {0xF9403F40u, kAll},  // ldr  x0, [x26, #0x78]
    {0x00000000u, 0u},    // ldr  x0, [x0, #static]
    {0x6B16001Fu, kAll},  // cmp  w0, w22
    {0x540001C1u, kAll},  // b.ne <cache hit>
    {0xF9403F40u, kAll},  // ldr  x0, [x26, #0x78]
    {0x00000000u, 0u},    // ldr  x0, [x0, #static]
    {0xF9402370u, kAll},  // ldr  x16, [x27, #0x40]
    {0x6B10001Fu, kAll},  // cmp  w0, w16
    {0x54000061u, kAll},  // b.ne <ready>
    {0x00000000u, 0u},    // ldur x2, [x27, #pool]
    {0x00000000u, 0u},    // bl   <allocate config key>
    {0x00000000u, 0u},    // ldur x16, [x27, #pool]
    {0xF90001F0u, kAll},  // str  x16, [x15]
    {0xF9402B64u, kAll},  // ldr  x4, [x27, #0x50]
    {0x00000000u, 0u},    // bl   <find config instance>
    {0xAA0003E1u, kAll},  // mov  x1, x0
    {0x14000002u, kAll},  // b    <read flag>
    {0xAA0003E1u, kAll},  // mov  x1, x0
    {0x00000000u, 0u},    // ldur w2, [x1, #field]
    {0x8B1C8042u, kAll},  // add  x2, x2, x28, lsl #32
    {0xAA0203E0u, kAll},  // mov  x0, x2
    {0x14000002u, kAll},  // b    <return>
    {0x9100C2C0u, kAll},  // add  x0, x22, #0x30 (false)
    {0xAA1D03EFu, kAll},  // mov  x15, x29
    {0xA8C179FDu, kAll},  // ldp  x29, x30, [x15], #0x10
    {0xD65F03C0u, kAll},  // ret
};

// Branch from the isMaterialDisabled call to the return-false block, skipping
// the material check and the config lookup on every icon paint.
constexpr uint32_t kBranchToFalse = 0x14000018u;  // b +0x60
constexpr size_t kCallWord = 3u;

// 0 = not installed, 1 = installing, 2 = installed.
uint32_t g_state = 0u;

}  // namespace

bool InstallDisableIconHighlightHook(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_state == 2u;
    }

    uintptr_t entry = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueMaskedPattern(
                image, kIsHighLightEnabledPattern,
                sizeof(kIsHighLightEnabledPattern) /
                        sizeof(kIsHighLightEnabledPattern[0]),
                &entry, &matches) ||
            !dart::RangeInExecutableSegment(
                    image, entry,
                    sizeof(kIsHighLightEnabledPattern) /
                            sizeof(kIsHighLightEnabledPattern[0]) *
                            sizeof(uint32_t))) {
        WOMMO_DISABLE_ICON_HIGHLIGHT_LOGW("disable_icon_highlight: pattern rejected "
                   "(matches=%u), keeping original behavior",
                   matches);
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    const uintptr_t call = entry + kCallWord * sizeof(uint32_t);
    const uint32_t patch[1] = {kBranchToFalse};
    if (!dart::PatchCode(call, patch, 1u)) {
        WOMMO_DISABLE_ICON_HIGHLIGHT_LOGE("disable_icon_highlight: failed to patch "
                   "IconLightConfig.isHighLightEnabled");
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    __atomic_store_n(&g_state, 2u, __ATOMIC_RELEASE);
    WOMMO_DISABLE_ICON_HIGHLIGHT_LOGW("disable_icon_highlight: icon highlight forced off at "
               "0x%" PRIxPTR " (file offset 0x%" PRIxPTR ")",
               call, call - image.bias);
    return true;
}

}  // namespace wommo::hooks
