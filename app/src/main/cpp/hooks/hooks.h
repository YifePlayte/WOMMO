//
// WOMMO native hook: feature hook declarations.
//
// Every feature owns one source file under hooks/ and exposes a single
// installer that receives the mapped Dart AOT image.  The native entry
// (wommo_native.cpp) calls each installer after libapp.so has been mapped.
//
// Installers must be idempotent and fail closed: when the expected pattern is
// missing or ambiguous the original launcher behavior is preserved.

#pragma once

#include "../dart_image.h"

namespace wommo::hooks {

// Ignore the isMIUIWidget gate inside AssistantDragToPAHandler.canDragToPA so
// non-MIUI widgets can be dragged to the minus screen.  Only the drag gate is
// patched; every other consumer of the flag sees the real metadata value.
bool InstallDragToPaHook(const dart::Image& image);

// Hide the launcher-drawn gesture hint line (fake navigation bar) in
// landscape, matching the legacy HideLandscapeNavBar Java hook.
bool InstallHideLandscapeNavBarHook(const dart::Image& image);

}  // namespace wommo::hooks
