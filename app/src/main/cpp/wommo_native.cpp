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
#include "hooks/native_config.h"
#include "log.h"
#include "lsposed_hook_backend.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <pthread.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

namespace {

constexpr char kLauncherProcessName[] = "com.miui.home";
constexpr char kWeatherProcessName[] = "com.miui.weather2";
constexpr char kGalleryProcessName[] = "com.miui.gallery";
constexpr char kSpawnerPath[] = "/system_ext/bin/hyos_spawner";
constexpr char kDartLibraryName[] = "libapp.so";
constexpr char kMamlSdkLibraryName[] = "librust_maml_sdk.so";
constexpr char kAppLauncherLibraryName[] = "libapp_launcher.so";
constexpr char kWeatherAppLibraryName[] = "libweather_app.so";
constexpr char kGalleryLibraryName[] = "libapp_gallery.so";

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

// The joystick guard covers the launcher, weather and gallery: all three
// statically link the same Rust backend. Dart business hooks stay
// launcher-only.
bool IsJoystickGuardProcess() {
    const int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    char command_line[64]{};
    ssize_t result;
    do {
        result = read(fd, command_line, sizeof(command_line));
    } while (result < 0 && errno == EINTR);
    close(fd);
    if (result <= 0) return false;
    return StringsEqual(command_line, kLauncherProcessName) ||
            StringsEqual(command_line, kWeatherProcessName) ||
            StringsEqual(command_line, kGalleryProcessName);
}

bool IsJoystickGuardHookProcess() {
    return IsJoystickGuardProcess() || IsHyosSpawnerProcessFamily();
}

// The feature hooks installed once per launcher process.  A hook with a
// non-zero feature bit is installed only while the module's switch for that
// feature is on; the switch values come from system_server (native_config.h).
using DartHookInstaller = bool (*)(const wommo::dart::Image& image);

struct DartHook {
    const char* name;
    uint32_t feature;
    DartHookInstaller install;
};

constexpr DartHook kDartHooks[] = {
    {"drag_to_pa", wommo::config::kDragToPa,
     &wommo::hooks::InstallDragToPaHook},
    {"allow_search_on_4x7", wommo::config::kAllowSearchOn4x7,
     &wommo::hooks::InstallAllowSearchOn4x7Hook},
    {"hide_landscape_nav_bar", wommo::config::kHideLandscapeNavBar,
     &wommo::hooks::InstallHideLandscapeNavBarHook},
    {"perfect_icons", wommo::config::kPerfectIcons,
     &wommo::hooks::InstallPerfectIconsHook},
    {"back_home_ratio", wommo::config::kBackHomeRatio,
     &wommo::hooks::InstallBackHomeRatioHook},
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

// The verified launcher image handle and the pending-gate state used to retry
// the gated hooks once the configuration endpoint answers.  The loader callback
// writes them, the retry worker reads them, so every access is atomic.
void* g_dart_handle = nullptr;
bool g_dart_retry_pending = false;
bool g_dart_retry_done = false;
// The joystick guard installs from a library callback, so its handle is kept
// for the same retry; loader callback and worker share it, hence the atomics.
void* g_joystick_handle = nullptr;
bool g_joystick_gallery = false;
bool g_joystick_pending = false;
// A gated hook invoked repeatedly (every libapp.so load callback) reports its
// switch state once per process.
uint32_t g_skip_logged = 0u;

void InstallJoystickGuard(void* handle, bool gallery);
void RetryPendingGatedHooks();

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

// A gated hook reports its switch state once per process; the Dart hook table
// and the library-scoped installers share the same log bit.
void LogSwitchOff(const char* name, uint32_t feature) {
    if (feature == 0u) return;
    if ((__atomic_fetch_or(&g_skip_logged, feature, __ATOMIC_ACQ_REL) &
                feature) != 0u) {
        return;
    }
    WOMMO_LOGW("hook '%s' skipped: switch off (flags=0x%" PRIx32 " ready=%u)",
               name, wommo::config::Flags(),
               wommo::config::Ready() ? 1u : 0u);
}

// The service manager caches a failed lookup for a few seconds, so a retry from
// the next library callback can still hit the cached miss (observed in the
// untrusted_app weather process).  This bounded worker is the only thread the
// module creates: it spaces the retries out, then exits.
constexpr uint32_t kConfigRetryAttempts = 5u;
constexpr long kConfigRetryDelaySeconds = 2;
uint32_t g_config_retry_started = 0u;

void* ConfigRetryWorker(void*) {
    pthread_setname_np(pthread_self(), "wommo-config");
    for (uint32_t attempt = 0u; attempt < kConfigRetryAttempts; ++attempt) {
        timespec delay{kConfigRetryDelaySeconds, 0};
        while (nanosleep(&delay, &delay) != 0 && errno == EINTR) {
        }
        if (!wommo::config::Refresh()) continue;
        WOMMO_LOGW("native config: endpoint answered on retry %u",
                   attempt + 1u);
        RetryPendingGatedHooks();
        return nullptr;
    }
    WOMMO_LOGW("native config: endpoint silent after %u retries",
               kConfigRetryAttempts);
    return nullptr;
}

void StartConfigRetryWorker() {
    if (__atomic_exchange_n(&g_config_retry_started, 1u, __ATOMIC_ACQ_REL) !=
            0u) {
        return;
    }
    pthread_t thread{};
    if (pthread_create(&thread, nullptr, ConfigRetryWorker, nullptr) == 0) {
        pthread_detach(thread);
        return;
    }
    __atomic_store_n(&g_config_retry_started, 0u, __ATOMIC_RELEASE);
    WOMMO_LOGE("native config: retry worker unavailable");
}

bool RunDartHooks(void* handle) {
    if (handle != nullptr) {
        __atomic_store_n(&g_dart_handle, handle, __ATOMIC_RELEASE);
    }
    wommo::dart::Image image{};
    if (!AcquireLauncherImage(handle, &image)) return false;

    // One bounded query per process: the launcher has no ART runtime, so the
    // switch values can only come from the module's system_server endpoint.
    const bool config_ready = wommo::config::EnsureReady();

    bool all_ok = true;
    bool gated_skipped = false;
    for (const DartHook& hook : kDartHooks) {
        if (hook.feature != 0u && !wommo::config::Enabled(hook.feature)) {
            LogSwitchOff(hook.name, hook.feature);
            gated_skipped = true;
            continue;
        }
        if (!hook.install(image)) {
            WOMMO_LOGE("hook '%s' failed to install", hook.name);
            all_ok = false;
        }
    }
    // A skipped hook only means "off" when the endpoint answered.  Otherwise the
    // next mapped library retries with the stored image.
    if (gated_skipped && !config_ready) {
        __atomic_store_n(&g_dart_retry_done, false, __ATOMIC_RELAXED);
        __atomic_store_n(&g_dart_retry_pending, true, __ATOMIC_RELEASE);
        StartConfigRetryWorker();
    }
    return all_ok;
}

// The endpoint lives in system_server and is installed by the module's system
// hook; a launcher that starts before it answers must not lose its gated
// features for the whole process lifetime.  The bounded worker retries on a
// timer, and every later library load is a free extra chance.
void RetryPendingDartHooks() {
    if (!__atomic_load_n(&g_dart_retry_pending, __ATOMIC_ACQUIRE) ||
            __atomic_load_n(&g_dart_retry_done, __ATOMIC_ACQUIRE)) {
        return;
    }
    if (!wommo::config::EnsureReady()) return;
    __atomic_store_n(&g_dart_retry_done, true, __ATOMIC_RELEASE);
    WOMMO_LOGW("native config: endpoint answered late, retrying gated hooks");
    RunDartHooks(__atomic_load_n(&g_dart_handle, __ATOMIC_ACQUIRE));
}

// Installs the joystick guard for one library when its switch is on.  The
// switch gates all three backends, so it cannot sit in the Dart hook table.
void InstallJoystickGuard(void* handle, bool gallery) {
    if (handle != nullptr) {
        __atomic_store_n(&g_joystick_handle, handle, __ATOMIC_RELEASE);
        __atomic_store_n(&g_joystick_gallery, gallery, __ATOMIC_RELEASE);
    }
    if (!wommo::config::EnsureReady() ||
            !wommo::config::Enabled(wommo::config::kJoystickTouchGuard)) {
        LogSwitchOff("joystick_touch_guard", wommo::config::kJoystickTouchGuard);
        // Only an unanswered endpoint is retried; an authoritative "off" is final.
        const bool pending = !wommo::config::Ready();
        __atomic_store_n(&g_joystick_pending, pending, __ATOMIC_RELEASE);
        if (pending) StartConfigRetryWorker();
        return;
    }
    __atomic_store_n(&g_joystick_pending, false, __ATOMIC_RELEASE);
    const bool installed = gallery
            ? wommo::hooks::InstallJoystickTouchGuardGalleryHooks(handle)
            : wommo::hooks::InstallJoystickTouchGuardHooks(handle);
    if (!installed) {
        WOMMO_LOGE("hook 'joystick_touch_guard' failed to install");
    }
}

// Re-runs every gated install that was held back by an unanswered endpoint.
void RetryPendingGatedHooks() {
    if (__atomic_load_n(&g_joystick_pending, __ATOMIC_ACQUIRE)) {
        InstallJoystickGuard(
                nullptr,
                __atomic_load_n(&g_joystick_gallery, __ATOMIC_ACQUIRE));
    }
    RetryPendingDartHooks();
}

// Called by LSPosed whenever a new library is loaded into the process.
void OnLibraryLoaded(const char* name, void* handle) {
    if (name == nullptr || handle == nullptr) return;

    // The spawner is the parent of the app children; business hooks only run
    // in the app processes, once their libraries are mapped there.
    if (!IsJoystickGuardProcess()) return;

    // The maml SDK is loaded lazily after libapp.so; the back-home ratio hook
    // needs its exports. Launcher only, and only while its switch is on.
    if (EndsWith(name, kMamlSdkLibraryName)) {
        if (IsLauncherProcess()) {
            if (wommo::config::EnsureReady() &&
                    wommo::config::Enabled(wommo::config::kBackHomeRatio)) {
                if (!wommo::hooks::InstallBackHomeRatioSdkHooks(handle)) {
                    WOMMO_LOGE("hook 'back_home_ratio' sdk part failed to install");
                }
            } else {
                LogSwitchOff("back_home_ratio", wommo::config::kBackHomeRatio);
            }
        }
        return;
    }
    // Joystick guard: backend 1.7 (launcher + weather share fingerprints) and
    // backend 3.9 (gallery, refactored converter).
    if (EndsWith(name, kAppLauncherLibraryName) ||
            EndsWith(name, kWeatherAppLibraryName)) {
        InstallJoystickGuard(handle, false);
        return;
    }
    if (EndsWith(name, kGalleryLibraryName)) {
        InstallJoystickGuard(handle, true);
        return;
    }
    // Dart business hooks are launcher-only.
    if (IsLauncherProcess() && EndsWith(name, kDartLibraryName)) {
        WOMMO_LOGW("libapp.so loaded, installing native hooks");
        RunDartHooks(handle);
        return;
    }
    // Any later library of the same process is a free retry point for hooks
    // held back by an endpoint that had not answered yet.
    RetryPendingGatedHooks();
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
    if (!IsJoystickGuardHookProcess()) {
        WOMMO_LOGW("rejected non-hooked HYOS process");
        return nullptr;
    }

    // In the app children the libraries may already be mapped when the entry
    // is initialized (e.g. after a LSPosed hot reload), so backfill once here.
    if (IsJoystickGuardProcess()) {
        if (IsLauncherProcess()) {
            void* dart_handle = dlopen(kDartLibraryName, RTLD_NOLOAD);
            if (dart_handle != nullptr) {
                RunDartHooks(dart_handle);
            }
        }
        void* app_launcher_handle = dlopen(kAppLauncherLibraryName, RTLD_NOLOAD);
        if (app_launcher_handle != nullptr) {
            InstallJoystickGuard(app_launcher_handle, false);
        }
        void* weather_handle = dlopen(kWeatherAppLibraryName, RTLD_NOLOAD);
        if (weather_handle != nullptr) {
            InstallJoystickGuard(weather_handle, false);
        }
        void* gallery_handle = dlopen(kGalleryLibraryName, RTLD_NOLOAD);
        if (gallery_handle != nullptr) {
            InstallJoystickGuard(gallery_handle, true);
        }
    }

    return OnLibraryLoaded;
}

}  // extern "C"
