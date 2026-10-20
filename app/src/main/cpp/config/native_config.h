#pragma once

#include <nhk/config.h>

namespace wommo::config {

// Feature bits, mirrored by NativeConfigEndpoint.kt.
constexpr uint32_t kPerfectIcons = 1u << 0u;
constexpr uint32_t kHideLandscapeNavBar = 1u << 1u;
constexpr uint32_t kDragToPa = 1u << 2u;
constexpr uint32_t kAllowSearchOn4x7 = 1u << 3u;
constexpr uint32_t kBackHomeRatio = 1u << 4u;
constexpr uint32_t kJoystickTouchGuard = 1u << 5u;
constexpr uint32_t kDisableIconHighlight = 1u << 6u;
constexpr uint32_t kKnownFeatures = kPerfectIcons | kHideLandscapeNavBar |
        kDragToPa | kAllowSearchOn4x7 | kBackHomeRatio | kJoystickTouchGuard |
        kDisableIconHighlight;

inline constexpr nhk::config::Descriptor kDescriptor{
    0x00574F4Du, 0x574F4D31, kKnownFeatures};

inline void Configure() { nhk::config::Configure(&kDescriptor); }
inline bool Refresh() { return nhk::config::Refresh(); }
inline bool EnsureReady() { return nhk::config::EnsureReady(); }
inline bool Enabled(uint32_t feature) {
    return nhk::config::Enabled(feature);
}
inline bool Ready() { return nhk::config::Ready(); }
inline uint32_t Flags() { return nhk::config::Flags(); }

}  // namespace wommo::config
