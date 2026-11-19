//
// WOMMO native hook: allow dragging non-MIUI widgets to the minus screen (PA).
//
// The gate lives in AssistantDragToPAHandler.canDragToPA.  For standard widget
// classes the function runs:
//
//     ldur w0, [x2, #0xfb]       ; itemInfo.isMIUIWidget
//     add  x0, x0, x28, lsl #32  ; decompress the 32-bit pointer
//     tbnz w0, #4, error_path    ; if false -> "unsupported itemInfo"
//     mov  x1, x4
//     ...                        ; span check, paging/timing guards follow
//
// Only this sequence is patched: the conditional branch is replaced with a
// NOP, so the drag check ignores the flag while every other consumer of
// isMIUIWidget (refresh scheduling, rendering, widget manager) keeps seeing
// the real metadata value.  The branch displacement is an operand, not
// structure: it moved from +0xa0 to +0xa8 between launcher builds, so its imm14
// is wildcarded and the trailing `mov x1, x4` keeps the site unique.

#include "hooks.h"

#include "../log.h"

#include <inttypes.h>

// Runtime logs are muted: hooks sit on hot paths and the log traffic is
// not worth the IO.  Uncomment the define below to re-enable them when
// debugging on device.
// #define WOMMO_DRAG_TO_PA_DEBUG
#ifdef WOMMO_DRAG_TO_PA_DEBUG
#define WOMMO_DRAG_TO_PA_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_DRAG_TO_PA_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_DRAG_TO_PA_LOGW(...) ((void)0)
#define WOMMO_DRAG_TO_PA_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

constexpr uint32_t kNop = 0xD503201Fu;

// The isMIUIWidget gate inside AssistantDragToPAHandler.canDragToPA.  The
// third word is the conditional branch that is replaced with a NOP; its imm14
// is dropped so a changed error-path distance still matches.
constexpr uint32_t kAll = 0xFFFFFFFFu;
constexpr dart::PatternWord kGatePattern[] = {
    {0xB84FB040u, kAll},          // ldur w0, [x2, #0xfb]
    {0x8B1C8000u, kAll},          // add  x0, x0, x28, lsl #32
    {0x37200500u, 0xFFFFE01Fu},   // tbnz w0, #4, <error path>
    {0xAA0403E1u, kAll},          // mov  x1, x4
};
constexpr size_t kGatePatternLength =
        sizeof(kGatePattern) / sizeof(kGatePattern[0]);

// 0 = not installed, 1 = installing, 2 = installed.
uint32_t g_state = 0u;

}  // namespace

bool InstallDragToPaHook(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        // Already installed or another thread is installing it.
        return g_state == 2u;
    }

    uintptr_t gate = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueMaskedPattern(image, kGatePattern, kGatePatternLength,
                                       &gate, &matches) ||
            !dart::RangeInExecutableSegment(
                    image, gate, kGatePatternLength * sizeof(uint32_t))) {
        WOMMO_DRAG_TO_PA_LOGW("drag_to_pa: isMIUIWidget gate pattern rejected "
                   "(matches=%u), keeping original behavior",
                   matches);
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    const uintptr_t branch = gate + 2u * sizeof(uint32_t);
    const uint32_t patch[1] = {kNop};
    if (!dart::PatchCode(branch, patch, 1u)) {
        WOMMO_DRAG_TO_PA_LOGE("drag_to_pa: failed to patch isMIUIWidget gate");
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    __atomic_store_n(&g_state, 2u, __ATOMIC_RELEASE);
    WOMMO_DRAG_TO_PA_LOGW("drag_to_pa: canDragToPA isMIUIWidget gate ignored at "
               "0x%" PRIxPTR " (file offset 0x%" PRIxPTR ")",
               branch, branch - image.bias);
    return true;
}

}  // namespace wommo::hooks
