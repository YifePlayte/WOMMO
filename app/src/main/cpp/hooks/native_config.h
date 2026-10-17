//
// WOMMO native hook: launcher switch values.
//
// The HyperOS 4 launcher family (MiuiHome, weather, gallery) is spawned by the
// HYOS runtime: those processes own no ART runtime, so neither the module's
// Java hooks nor the framework's remote preference store are reachable from
// inside them.  The module's Java hook in system_server publishes the switch
// snapshot on a private transaction over the clipboard service
// (hook/hooks/singlepackage/system/NativeConfigEndpoint.kt); this module queries
// that endpoint once and keeps the answer in atomics for the feature hooks.
//
// Fail closed: a gated feature is installed only after an authoritative answer
// that contains its bit.  An unreachable endpoint, an unusable frame or an
// unreadable preference store all leave every gated feature off, exactly like a
// switched-off feature.

#pragma once

#include <stdint.h>

namespace wommo::config {

// Feature bits, mirrored by NativeConfigEndpoint.kt.  The preference key of
// each bit is listed there; a hook without a bit installs unconditionally.
constexpr uint32_t kPerfectIcons = 1u << 0u;
constexpr uint32_t kHideLandscapeNavBar = 1u << 1u;
constexpr uint32_t kKnownFeatures = kPerfectIcons | kHideLandscapeNavBar;

// Performs one synchronous query and publishes an authoritative answer.
// Returns false when the endpoint refused, answered with an unusable frame or
// could not be reached.
bool Refresh();

// Refresh() with a bounded retry budget, for library-load callbacks.  Never
// call it from a hot path.
bool EnsureReady();

// True only for a feature set in an authoritative answer.
bool Enabled(uint32_t feature);

// Diagnostics for the entry point.
bool Ready();
uint32_t Flags();

}  // namespace wommo::config
