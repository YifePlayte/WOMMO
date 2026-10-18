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

#include "hooks/hooks.h"
#include "config/native_config.h"
#include "log.h"

#include <nhk/log.h>
#include <nhk/module.h>
#include <nhk/process.h>
#include <nhk/text.h>

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
constexpr char kDataAppPrefix[] = "/data/app/";
constexpr char kDataDartLibraryTail[] = "/base.apk!/lib/arm64-v8a/libapp.so";
constexpr char kSystemHomePrefix[] = "/product/priv-app/MiuiHome/";
constexpr char kSystemDartLibraryTail[] =
        "/MiuiHome.apk!/lib/arm64-v8a/libapp.so";

bool IsLauncherProcess() {
    return nhk::process::SelfCommandLineIs(kLauncherProcessName);
}

// LSPosed's HYOS native entry is initialized in the root spawner process
// (cmdline `usap64`) before it forks the launcher child.  The executable
// identity is the hard process-family boundary.
bool IsHyosSpawnerProcessFamily() {
    return nhk::process::SelfExecutableIs(kSpawnerPath);
}

// The joystick guard covers the launcher, weather and gallery: all three
// statically link the same Rust backend. Dart business hooks stay
// launcher-only.
bool IsJoystickGuardProcess() {
    return nhk::process::SelfCommandLineIs(kLauncherProcessName) ||
            nhk::process::SelfCommandLineIs(kWeatherProcessName) ||
            nhk::process::SelfCommandLineIs(kGalleryProcessName);
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

bool AcceptLauncherDartPath(const char* path) {
    if (path == nullptr) return false;
    if (nhk::text::StartsWith(path, kDataAppPrefix) &&
            nhk::text::EndsWith(path, kDataDartLibraryTail)) {
        return true;
    }
    return nhk::text::StartsWith(path, kSystemHomePrefix) &&
            nhk::text::EndsWith(path, kSystemDartLibraryTail);
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
// Per-feature entry logs, matching the Java BaseHook "Inited hook" /
// "Failed init hook" lines.  Success and failure are tracked separately so a
// later retry can still report a success after an initial failure.
uint32_t g_dart_success_logged = 0u;
uint32_t g_dart_failure_logged = 0u;
uint32_t g_joystick_success_logged = 0u;
uint32_t g_joystick_failure_logged = 0u;
uint32_t g_back_home_sdk_success_logged = 0u;
uint32_t g_back_home_sdk_failure_logged = 0u;

void InstallJoystickGuard(void* handle, bool gallery);
void RetryPendingGatedHooks();

// Binds the LSPosed-reported library handle to the exact Dart AOT image:
// the handle must export the snapshot symbol, the symbol's mapping path must
// be one of the launcher APK forms, and the ELF image at that load bias must
// be found again through dl_iterate_phdr.  Any mismatch fails closed.
bool AcquireLauncherImage(void* handle, wommo::dart::Image* image) {
    if (!wommo::dart::AcquireVerifiedDartImage(handle, &AcceptLauncherDartPath,
                                               image)) {
        return false;
    }
    if (__atomic_exchange_n(&g_image_verified_state, 1u, __ATOMIC_ACQ_REL) ==
            0u) {
        WOMMO_LOGW("launcher dart image verified");
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

void LogDartHookResult(size_t index, const char* name, bool ok) {
    if (index >= 32u) return;
    const uint32_t bit = 1u << index;
    uint32_t* logged = ok ? &g_dart_success_logged : &g_dart_failure_logged;
    if ((__atomic_fetch_or(logged, bit, __ATOMIC_ACQ_REL) & bit) != 0u) return;
    if (ok) {
        WOMMO_LOGW("Inited hook: %s", name);
    } else {
        WOMMO_LOGE("Failed init hook: %s", name);
    }
}

const char* JoystickTargetName(bool gallery) {
    if (gallery) return "gallery";
    return IsLauncherProcess() ? "launcher" : "weather";
}

void LogJoystickResult(bool gallery, bool ok) {
    uint32_t* logged = ok ? &g_joystick_success_logged
                          : &g_joystick_failure_logged;
    if (__atomic_exchange_n(logged, 1u, __ATOMIC_ACQ_REL) != 0u) return;
    if (ok) {
        WOMMO_LOGW("Inited hook: joystick_touch_guard (%s)",
                   JoystickTargetName(gallery));
    } else {
        WOMMO_LOGE("Failed init hook: joystick_touch_guard (%s)",
                   JoystickTargetName(gallery));
    }
}

void LogBackHomeSdkResult(bool ok) {
    uint32_t* logged = ok ? &g_back_home_sdk_success_logged
                          : &g_back_home_sdk_failure_logged;
    if (__atomic_exchange_n(logged, 1u, __ATOMIC_ACQ_REL) != 0u) return;
    if (ok) {
        WOMMO_LOGW("Inited hook: back_home_ratio (maml sdk)");
    } else {
        WOMMO_LOGE("Failed init hook: back_home_ratio (maml sdk)");
    }
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
    for (size_t index = 0u; index < sizeof(kDartHooks) / sizeof(kDartHooks[0]);
            ++index) {
        const DartHook& hook = kDartHooks[index];
        if (hook.feature != 0u && !wommo::config::Enabled(hook.feature)) {
            LogSwitchOff(hook.name, hook.feature);
            gated_skipped = true;
            continue;
        }
        const bool installed = hook.install(image);
        LogDartHookResult(index, hook.name, installed);
        if (!installed) all_ok = false;
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
    // The spawner is the parent process, not an app child.  A library callback
    // can race the child cmdline transition; never install app guards from a
    // process that is not one of the three launcher-family targets.
    if (!IsJoystickGuardProcess()) return;
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
    LogJoystickResult(gallery, installed);
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

bool OnNativeInit() {
    wommo::config::Configure();
    const bool launcher = IsLauncherProcess();
    const bool spawner = IsHyosSpawnerProcessFamily();
    WOMMO_LOGW("native_init: launcher=%u spawner=%u", launcher ? 1u : 0u,
               spawner ? 1u : 0u);
    if (!IsJoystickGuardHookProcess()) {
        WOMMO_LOGW("rejected non-hooked HYOS process");
        return false;
    }
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
    return true;
}

// Called by LSPosed whenever a new library is loaded into the process.
void OnLibraryLoaded(const char* name, void* handle) {
    if (name == nullptr || handle == nullptr) return;

    // The spawner is the parent of the app children; business hooks only run
    // in the app processes, once their libraries are mapped there.
    if (!IsJoystickGuardProcess()) return;

    // The maml SDK is loaded lazily after libapp.so; the back-home ratio hook
    // needs its exports. Launcher only, and only while its switch is on.
    if (nhk::text::EndsWith(name, kMamlSdkLibraryName)) {
        if (IsLauncherProcess()) {
            if (wommo::config::EnsureReady() &&
                    wommo::config::Enabled(wommo::config::kBackHomeRatio)) {
                const bool installed =
                        wommo::hooks::InstallBackHomeRatioSdkHooks(handle);
                LogBackHomeSdkResult(installed);
            } else {
                LogSwitchOff("back_home_ratio", wommo::config::kBackHomeRatio);
            }
        }
        return;
    }
    // Joystick guard: backend 1.7 (launcher + weather share fingerprints) and
    // backend 3.9 (gallery, refactored converter).
    if (nhk::text::EndsWith(name, kAppLauncherLibraryName) ||
            nhk::text::EndsWith(name, kWeatherAppLibraryName)) {
        InstallJoystickGuard(handle, false);
        return;
    }
    if (nhk::text::EndsWith(name, kGalleryLibraryName)) {
        InstallJoystickGuard(handle, true);
        return;
    }
    // Dart business hooks are launcher-only.
    if (IsLauncherProcess() && nhk::text::EndsWith(name, kDartLibraryName)) {
        WOMMO_LOGW("libapp.so loaded, installing native hooks");
        RunDartHooks(handle);
        return;
    }
    // Any later library of the same process is a free retry point for hooks
    // held back by an endpoint that had not answered yet.
    RetryPendingGatedHooks();
}

}  // namespace

namespace nhk {

const ModuleDescriptor& GetModuleDescriptor() {
    static const ModuleDescriptor kDescriptor = MakeDescriptor(
            "WommoNative", OnNativeInit, OnLibraryLoaded);
    return kDescriptor;
}

}  // namespace nhk

