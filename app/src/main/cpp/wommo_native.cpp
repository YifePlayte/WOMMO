// WOMMO native hook: LSPosed native entry for the MiuiHome launcher family.
//
// This file is the single native_init entry point declared in
// META-INF/xposed/native_init.list.  It owns only the generic lifecycle:
//
//   * accepting the HYOS process family (spawner + launcher child)
//   * resolving the mapped Dart AOT image (libapp.so)
//   * dispatching every registered feature hook once per process
//
// Feature hooks live under hooks/ and only receive the mapped Dart image.
//
// The HYOS entry is initialized in the root spawner (cmdline `usap64`) before
// the package process is forked, so both the spawner and the actual launcher
// process are accepted here.  Business hooks run later, once libapp.so is
// mapped in the launcher child.  The process-family identity checks follow the
// approach established by the MiuiBackGestureHook research project.

#include "dart_image.h"
#include "hooks/hooks.h"
#include "log.h"
#include "lsposed_hook_backend.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

namespace {

constexpr char kLauncherProcessName[] = "com.miui.home";
constexpr char kSpawnerPath[] = "/system_ext/bin/hyos_spawner";
constexpr char kDartLibraryName[] = "libapp.so";

// Only these mapped paths are accepted as the launcher's Dart AOT image.
// Launcher libraries are mapped straight out of the APK, so dladdr reports the
// in-archive path.  Everything else fails closed before any patch is written.
constexpr char kDartSnapshotSymbol[] = "_kDartIsolateSnapshotInstructions";
constexpr char kDataAppPrefix[] = "/data/app/";
constexpr char kDataDartLibraryTail[] = "/base.apk!/lib/arm64-v8a/libapp.so";
constexpr char kSystemHomePrefix[] = "/product/priv-app/MiuiHome/";
constexpr char kSystemDartLibraryTail[] =
        "/MiuiHome.apk!/lib/arm64-v8a/libapp.so";

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

bool IsLauncherProcess() {
    const int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    char command_line[64]{};
    ssize_t result;
    do {
        result = read(fd, command_line, sizeof(command_line));
    } while (result < 0 && errno == EINTR);
    close(fd);
    if (result <= 0) return false;
    return StringsEqual(command_line, kLauncherProcessName);
}

// LSPosed's HYOS native entry is initialized in the root spawner process
// (cmdline `usap64`) before it forks the launcher child.  The executable
// identity is the hard process-family boundary.
bool IsHyosSpawnerProcessFamily() {
    char executable[128]{};
    const ssize_t length = readlink("/proc/self/exe", executable,
                                    sizeof(executable) - 1u);
    if (length <= 0 || static_cast<size_t>(length) >= sizeof(executable)) {
        return false;
    }
    executable[length] = '\0';
    return StringsEqual(executable, kSpawnerPath);
}

bool IsLauncherHookProcess() {
    return IsLauncherProcess() || IsHyosSpawnerProcessFamily();
}

// The feature hooks installed once per launcher process.
using DartHookInstaller = bool (*)(const wommo::dart::Image& image);

struct DartHook {
    const char* name;
    DartHookInstaller install;
};

constexpr DartHook kDartHooks[] = {
    {"drag_to_pa", &wommo::hooks::InstallDragToPaHook},
    {"hide_landscape_nav_bar", &wommo::hooks::InstallHideLandscapeNavBarHook},
    {"perfect_icons", &wommo::hooks::InstallPerfectIconsHook},
};

bool IsLauncherDartLibraryPath(const char* path) {
    if (path == nullptr) return false;
    if (StartsWith(path, kDataAppPrefix) &&
            EndsWith(path, kDataDartLibraryTail)) {
        return true;
    }
    return StartsWith(path, kSystemHomePrefix) &&
            EndsWith(path, kSystemDartLibraryTail);
}

uint32_t g_image_verified_state = 0u;

// Binds the LSPosed-reported library handle to the exact Dart AOT image:
// the handle must export the snapshot symbol, the symbol's mapping path must
// be one of the launcher APK forms, and the ELF image at that load bias must
// be found again through dl_iterate_phdr.  Any mismatch fails closed.
bool AcquireLauncherImage(void* handle, wommo::dart::Image* image) {
    if (handle == nullptr || image == nullptr) return false;
    void* snapshot = dlsym(handle, kDartSnapshotSymbol);
    if (snapshot == nullptr) {
        WOMMO_LOGW("launcher image rejected: missing symbol %s",
                   kDartSnapshotSymbol);
        return false;
    }
    Dl_info info{};
    if (dladdr(snapshot, &info) == 0 || info.dli_fbase == nullptr) {
        WOMMO_LOGW("launcher image rejected: dladdr failed");
        return false;
    }
    if (!IsLauncherDartLibraryPath(info.dli_fname)) {
        WOMMO_LOGW("launcher image rejected: unexpected path %s",
                   info.dli_fname != nullptr ? info.dli_fname : "<null>");
        return false;
    }
    if (!wommo::dart::AcquireImageAtBase(
                reinterpret_cast<uintptr_t>(info.dli_fbase), image)) {
        WOMMO_LOGW("launcher image rejected: no ELF image at base %p",
                   info.dli_fbase);
        return false;
    }
    if (__atomic_exchange_n(&g_image_verified_state, 1u, __ATOMIC_ACQ_REL) ==
            0u) {
        WOMMO_LOGW("launcher dart image verified: %s", info.dli_fname);
    }
    return true;
}

bool RunDartHooks(void* handle) {
    wommo::dart::Image image{};
    if (!AcquireLauncherImage(handle, &image)) return false;

    bool all_ok = true;
    for (const DartHook& hook : kDartHooks) {
        if (!hook.install(image)) {
            WOMMO_LOGE("hook '%s' failed to install", hook.name);
            all_ok = false;
        }
    }
    return all_ok;
}

// Called by LSPosed whenever a new library is loaded into the process.
void OnLibraryLoaded(const char* name, void* handle) {
    if (name == nullptr || handle == nullptr) return;

    // The spawner is the parent of the launcher child; only the launcher runs
    // the business hooks, and only once libapp.so is mapped there.
    if (!IsLauncherProcess()) return;
    if (!EndsWith(name, kDartLibraryName)) return;

    WOMMO_LOGW("libapp.so loaded, installing native hooks");
    RunDartHooks(handle);
}

}  // namespace

extern "C" {

// LSPosed native_init entry point.
__attribute__((visibility("default"), used))
NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    const bool backend_ready = InitializeLsposedHookBackend(entries);
    const bool launcher = IsLauncherProcess();
    const bool spawner = IsHyosSpawnerProcessFamily();

    WOMMO_LOGW("native_init: entries=%u backend=%u launcher=%u spawner=%u",
               entries != nullptr ? 1u : 0u, backend_ready ? 1u : 0u,
               launcher ? 1u : 0u, spawner ? 1u : 0u);

    if (!backend_ready) return nullptr;
    if (!IsLauncherHookProcess()) {
        WOMMO_LOGW("rejected non-launcher HYOS process");
        return nullptr;
    }

    // In the launcher child libapp.so may already be mapped when the entry is
    // initialized (e.g. after a LSPosed hot reload), so backfill once here.
    if (launcher) {
        void* dart_handle = dlopen(kDartLibraryName, RTLD_NOLOAD);
        if (dart_handle != nullptr) {
            RunDartHooks(dart_handle);
        }
    }

    return OnLibraryLoaded;
}

}  // extern "C"
