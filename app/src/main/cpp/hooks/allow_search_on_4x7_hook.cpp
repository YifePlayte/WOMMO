// WOMMO native hook: allow 4x7 desktop with capsule search (minimal / ask XiaoAI).
//
// OS3 reference: LauncherLayoutType.isLayoutAndSearchTypeSupported returned
// remainSpace >= 0; on 4 columns the capsule indicator turns remainSpace
// slightly negative at max rows, so enabling search from 4x7 prompted 4x6 and
// switching layout to 4x7 turned search off. Forcing the tail select true
// keeps 4x7 rows for types 2/3. Classic (1) is untouched: its large bar costs
// a full row, so 7 rows + classic would overflow; it keeps stock 4x6 (option B).
//
// Pattern is the stable tail: eor / fcmp d0,d1 / add true / add false /
// csel x0,x16,x17,ge (patched to mov x0,x16). Unique in verified builds.

#include "hooks.h"

#include "../log.h"

#include <inttypes.h>

// Runtime logs are muted: hooks sit on hot paths and the log traffic is
// not worth the IO.  Uncomment the define below to re-enable them when
// debugging on device.
// #define WOMMO_ALLOW_SEARCH_ON_4X7_DEBUG
#ifdef WOMMO_ALLOW_SEARCH_ON_4X7_DEBUG
#define WOMMO_ALLOW_SEARCH_ON_4X7_LOGW(...) WOMMO_LOGW(__VA_ARGS__)
#define WOMMO_ALLOW_SEARCH_ON_4X7_LOGE(...) WOMMO_LOGE(__VA_ARGS__)
#else
#define WOMMO_ALLOW_SEARCH_ON_4X7_LOGW(...) ((void)0)
#define WOMMO_ALLOW_SEARCH_ON_4X7_LOGE(...) ((void)0)
#endif

namespace wommo::hooks {
namespace {

constexpr size_t kTailPatternLength = 5u;
constexpr uint32_t kTailPattern[kTailPatternLength] = {
    0x6e211c21u,  // eor  v1.16b, v1.16b, v1.16b
    0x1e612000u,  // fcmp d0, d1
    0x910082d0u,  // add  x16, x22, #0x20 (true)
    0x9100c2d1u,  // add  x17, x22, #0x30 (false)
    0x9a91a200u,  // csel x0, x16, x17, ge
};
constexpr uint32_t kMovX0X16 = 0xaa1003e0u;  // mov x0, x16 (force true)

// 0 = not installed, 1 = installing, 2 = installed.
uint32_t g_state = 0u;

}  // namespace

bool InstallAllowSearchOn4x7Hook(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        // Already installed or another thread is installing it.
        return g_state == 2u;
    }

    uintptr_t tail = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueWordPattern(image, kTailPattern, kTailPatternLength,
                                     &tail, &matches) ||
            !dart::RangeInExecutableSegment(
                    image, tail, kTailPatternLength * sizeof(uint32_t))) {
        WOMMO_ALLOW_SEARCH_ON_4X7_LOGW("allow_search_on_4x7: pattern rejected "
                   "(matches=%u), keeping original behavior",
                   matches);
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    const uintptr_t csel = tail + 4u * sizeof(uint32_t);
    const uint32_t patch[1] = {kMovX0X16};
    if (!dart::PatchCode(csel, patch, 1u)) {
        WOMMO_ALLOW_SEARCH_ON_4X7_LOGE("allow_search_on_4x7: failed to patch support check");
        __atomic_store_n(&g_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    __atomic_store_n(&g_state, 2u, __ATOMIC_RELEASE);
    WOMMO_ALLOW_SEARCH_ON_4X7_LOGW("allow_search_on_4x7: support check forced true at "
               "0x%" PRIxPTR " (file offset 0x%" PRIxPTR ")",
               csel, csel - image.bias);
    return true;
}

}  // namespace wommo::hooks
