# AGENTS.md

## Project Context

WOMMO is a personal LSPosed module (API 102) that patches MIUI/HyperOS system
components. One APK ships two independent hook families:

- Java/Kotlin hooks under `app/src/main/java/com/yifeplayte/wommo/hook/` for
  regular Android processes that have dex code, built on ezhooktool.
- Native hooks under `app/src/main/cpp/` for the HyperOS 4 Flutter launcher
  (`com.miui.home`), whose business logic is a Dart AOT snapshot (`libapp.so`).
  That APK contains no dex, so Java hooks cannot reach it.

When a feature exists on both launcher generations, both implementations share
one preference key: the Java hook covers the legacy launcher, the native hook
covers the Flutter one, and the settings UI switch is the same.

## Repository Layout

```text
app/src/main/java/com/yifeplayte/wommo/
  App.kt                                  XposedService binding, UI process
  hook/MainHook.kt                        the only XposedModule; lifecycle
  hook/hooks/Base*.kt                     hook base classes
  hook/hooks/singlepackage/              one group object per host package
  hook/hooks/singlepackage/<group>/      BaseHook implementations (lowercase)
  hook/hooks/multipackage/*.kt            BaseMultiHook implementations
  hook/hooks/subpackage/                  BaseSubPackage + BaseSubHook groups
  hook/utils/                             Log, DexKit, preferences, fields...
  activity/sections/*.kt                  settings UI; keys match hook keys
  utils/                                  Build, Clazz, Object, Terminal...
app/src/main/cpp/
  classhelper/                            pre-existing JNI helper library
  wommo_native.cpp                        native_init entry; hook dispatch
  dart_image.{h,cpp}                      image lookup, pattern scan, patch
  lsposed_hook_backend.*                  LSPosed native API wrapper
  log.h                                   WOMMO_LOGW / WOMMO_LOGE only
  hooks/hooks.h                           native feature hook declarations
  hooks/*_hook.cpp                        one file per native feature
app/src/main/resources/META-INF/xposed/   module.prop, java_init.list,
                                          native_init.list, scope.list
```

`classhelper` is a separate pre-existing JNI helper. Do not rename it, merge it
into `wommo_native`, or change its exported symbols; Kotlin code loads it by
name (`utils/Object.kt`).

## Java/Dex Hook System

### Entry and lifecycle

- `META-INF/xposed/java_init.list` declares `com.yifeplayte.wommo.hook.MainHook`
  as the module entry.
- `MainHook` lifecycle:
  - `onModuleLoaded`: set `EzReflect.logger = Log`, call
    `EzXposed.initOnModuleLoaded(this, param)`, then install hooks on
    `EzXposed.onTargetReady`.
  - `onPackageLoaded` / `onPackageReady`: initialize `EzXposed` for the host
    package; `onPackageReady` also initializes DexKit with the host APK path
    for every package except `system`.
  - `onSystemServerStarting`: system_server support.
  - `onHotReloading` / `onHotReloaded`: delegate to
    `EzXposed.handleHotReloading` / `handleHotReloadedWithTargetReady`; after a
    reload the hooks are re-installed so fresh preference values are read.
- `installHooks()` iterates the three reflection registries
  (`singlePackagesHooked`, `multiPackagesHooked`, `subPackagesHooked`), calls
  `init()` on each, then closes DexKit.
- `PACKAGE_NAME_HOOKED` is the union of all registry package names. It decides
  which packages receive `onPackageLoaded`/`onPackageReady` and powers the
  "restart all scope" dialog (`killall <pkg>` via libsu for every hooked
  package except `android`).
- The settings UI calls `reloadAllTargets()` after a switch change, which asks
  `XposedService.hotReloadModule` to reload every running target process.

### Hook kinds

| Base | Scope | Pick when |
| --- | --- | --- |
| `BaseHook` | one package | normal single-process feature |
| `BasePackage` | one package | group object; owns the `BaseHook` list |
| `BaseMultiHook` | several packages | the same logic is installed per package through `hooks: Map<String, () -> Unit>` |
| `BaseSubHook` | one sub-package | feature lives in a plugin with its own ClassLoader |
| `BaseSubPackage` | one sub-package | group object; resolves the plugin ClassLoader |

- Every hook has a `key` (snake_case feature identifier). `isEnabled` reads the
  remote preference `getBoolean(key, false)`, so a hook installs only when the
  user enabled it.
- `init()` is single-shot (`isInit` guard) and wraps `hook()` in `runCatching`,
  logging failures instead of crashing the host.

### Registry scanning and directory conventions

- `ClassScanner.scanObjectOf<T>(packageName)` reflects the module's own
  dex elements and returns every singleton (`INSTANCE` field) assignable to `T`
  whose class lives directly in `packageName` (inner classes are skipped).
- `BasePackage` groups hooks from the package
  `javaClass.packageName + "." + javaClass.simpleName.lowercase()`.
  Example: `singlepackage/Home.kt` (`object Home : BasePackage("com.miui.home")`)
  owns the hooks in `singlepackage/home/`.
- The same convention applies to `BaseSubPackage`. Group object names are
  CamelCase, their hook packages are all-lowercase.
- A new group object only needs to exist; `MainHook` discovers it.

### Adding a Java hook

1. Choose the group: `singlepackage` (one package), `multipackage` (several),
   or a `subpackage` plugin. Reuse an existing group object when one covers the
   host package; otherwise add a `BasePackage`/`BaseMultiHook`/`BaseSubPackage`.
2. Add `object <Feature> : BaseHook()` (or `BaseSubHook`) in the group's
   lowercase package, override `key` and `hook()`.
3. Implement with the ezhooktool DSL: `loadClass(...)`,
   `findMethod { name(...); paramCount(...); ... }`,
   `.createHook { before { it.args[...] = ... } / after { ... } }`,
   `returnConstant(...)`.
4. Add a UI switch/slider in the matching `activity/sections/*.kt` with the
   same key (`SPSwitch`/`SPSlider`) and a string resource in `res/values`.
5. When reimplementing an existing feature (for example on a new launcher
   generation), reuse the existing key so the same switch keeps working.

### Tooling

- ezhooktool core helpers used everywhere: `loadClass`, `findMethod`,
  `findAllMethods`, `createHook`/`createHooks`, `callMethod`, `getFieldOrNull`,
  `putField`, `putStaticField`, `paramsAssignableFrom`, `notAbstract`.
- DexKit (`hook/utils/DexKit.kt`) locates obfuscated members when names are
  unavailable: `dexKitBridge.findClass { matcher { usingStrings = listOf(...) } }`
  / `findMethod { ... }`, then `.getInstance()` / `.getMethodInstance()` which
  apply `EzXposed.safeClassLoader`. `.single()` throws on ambiguity, so prefer
  stable markers (unique strings) and log clearly on failure.
- `hook/utils/AdditionalFields.kt` replaces XposedHelpers additional instance
  fields with a weak identity map. Use it instead of holding strong references.
- `utils/Object.kt` loads `libclasshelper` and exposes `invokeSuper*Method` for
  calling super implementations from hooks.
- `utils/Clazz.kt` can force static final fields through `Unsafe` when
  reflection refuses; only use it when a normal hook cannot work.
- `hook/utils/Log.kt` writes to logcat (tag `WOMMO`) and to the libxposed log;
  use `Log.i/w/e` from hooks. `utils/Build.kt` exposes `IS_HYPER_OS`,
  `HYPER_OS_VERSION`, `IS_TABLET`, `IS_INTERNATIONAL_BUILD` for version-gated
  paths (see `SystemUIPlugin.initClassLoader()` for the branching pattern).
- Settings live in the remote preferences file `config`
  (`XSharedPreferences` on the hook side, `SharedPreferences.mSP` on the UI
  side). Hook code must read them at `init()` time (or after hot reload), not
  cache them forever.

## Native Hook System (Flutter MiuiHome)

### Entry and lifecycle

- `META-INF/xposed/native_init.list` declares `libwommo_native.so`, the single
  native entry. Do not add one `.so` per feature.
- LSPosed's HYOS entry initializes in the root spawner (cmdline `usap64`, exe
  `/system_ext/bin/hyos_spawner`) before the launcher child is forked.
  `native_init` accepts the spawner family and the launcher process, but
  business hooks only install from the `libapp.so` load callback inside
  `com.miui.home`.
- Image verification is handle-bound and fail closed (see
  `AcquireLauncherImage` in `wommo_native.cpp`):
  1. the process must be exactly `com.miui.home` (spawner family only passes
     `native_init`, never the library callback);
  2. the LSPosed-reported library handle must export
     `_kDartIsolateSnapshotInstructions`;
  3. `dladdr` on that symbol must report a mapping path that is either
     `/data/app/.../base.apk!/lib/arm64-v8a/libapp.so` or
     `/product/priv-app/MiuiHome/.../MiuiHome.apk!/lib/arm64-v8a/libapp.so`;
  4. `AcquireImageAtBase` must find the same ELF image again by load bias so
     the segments are pinned to the verified handle.
  Never fall back to a loose `libapp.so` name match for patching.

### Patch persistence: madvise guard

- HyperOS memory cleanup can issue `madvise(MADV_DONTNEED)` over the mapped
  launcher image. That drops the modified copy-on-write code pages and the
  kernel re-reads the original bytes, silently reverting every patch.
- `lsposed_hook_backend.cpp` hooks libc's `madvise` through LSPosed's
  `hookFunc` and removes registered hook pages from `MADV_DONTNEED` ranges
  (`GuardedMadvise`). Page registration happens before the patch is written to
  close the concurrent-cleanup race.
- `dart::PatchCode` calls `ProtectHookRange` before touching the code. A range
  that cannot be protected is not patched: the guard is a hard prerequisite,
  not a best effort.
- Verify the hook is live by reading the `madvise` entry in `/proc/<pid>/mem`:
  the original bionic prologue (`bti c; mov x8, #__NR_madvise; svc #0`) is
  replaced by a `ldr x17 / br x17` trampoline.

### Adding a feature hook

1. Create `app/src/main/cpp/hooks/<feature>_hook.cpp` with an idempotent
   installer taking `const wommo::dart::Image&`.
2. Declare it in `hooks/hooks.h`.
3. Register `{ "<feature>", &wommo::hooks::Install<Feature>Hook }` in
   `kDartHooks[]` in `wommo_native.cpp`.
4. Add the source to `WOMMO_NATIVE_SOURCES` in `cpp/CMakeLists.txt`.

### Patching rules

- Locate code by instruction fingerprint, never by launcher version offsets.
  A fingerprint must match exactly once in every supported libapp.so build.
  Missing or ambiguous matches fail closed: log a warning and keep the original
  launcher behavior.
- Pool-page operands (`add x17, x27, #page`, `ldr ..., [x17, #imm]`) differ
  between builds. Use wildcard `PatternWord` entries for them and keep the rest
  exact.
- Patch the smallest possible unit. Prefer bypassing the one failing branch
  (`nop` / `ret` at a verified site) over rewriting shared behavior. Do not
  force data fields that other code paths depend on (for example, do not set
  `isMIUIWidget` globally just to pass the drag-to-PA gate).
- Write through `dart::PatchCode`: it manages mprotect, restores RX, clears the
  instruction cache, and reads the words back. Never patch without read-back
  verification.
- Version-specific offsets discovered during reverse engineering are evidence,
  not constants. Keep them in local notes; only fingerprints belong in code.
- Known gap: the native hooks install unconditionally. Wiring them to the
  shared module preference keys (the same keys the Java hooks and UI use) is
  pending.

### Logging

HyperOS logd filters INFO/DEBUG from the launcher process. Use `WOMMO_LOGW` /
`WOMMO_LOGE` (tag `WommoNative`) for everything. Never use `android.util.Log`
from native code; keep diagnostics in logcat under the module tag or in the
LSPosed logs.

## Device and Testing Workflow

Target device: Redmi 25102RKBEC (`myron`), HyperOS 4 / Android 17, 4K pages,
LSPosed 2.2.0 (API 102).

Build (debug is the iteration target, release for delivery):

```text
./gradlew :app:assembleDebug
```

Deploy Java hooks: `adb install -r <apk>`, then restart the affected scope
processes (the in-app "restart all scope" dialog runs `killall` per hooked
package, or do it manually with adb/root). Verify with `adb logcat | grep
WOMMO` and the module log.

Deploy native hooks:

1. `adb install -r app/build/outputs/apk/debug/WOMMO-*.apk`
2. Kill the desktop's HYOS spawner so a fresh one loads the new APK. Find it by
   checking that `readlink /proc/<pid>/exe` is `/system_ext/bin/hyos_spawner`;
   `kill -TERM <pid>` and the launcher restarts automatically.
3. If the hook logs are missing, the launcher was forked from a spawner that
   did not load the module. Kill the launcher process (`pidof com.miui.home`)
   so it refork from a module-loaded spawner.
4. Verify: `adb logcat -d | grep WommoNative` and check the reported file
   offset. Then verify the patched words in `/proc/<pid>/mem` as root.
5. Confirm the launcher is stable (no new tombstone) before visible tests.
   User-visible behavior must be confirmed on the device by the user.

Cross-version testing (native): keep both launcher APKs (system
`/product/priv-app/MiuiHome/MiuiHome.apk` and the current `/data/app` update).
`adb install -r <apk>` switches versions. Every new fingerprint must resolve
uniquely and patch correctly on both before the change is considered done.
Meta field offsets must not be hardcoded (they differ per launcher build).

LSPosed scope is controlled only through:

```text
su -c '/data/adb/modules/zygisk_lsposed/lspctl scope add|remove|list <module> <pkg> --user=0'
```

## Recovery: Launcher Safe Mode

A crash loop makes the launcher start in safe mode (`com.miui.home:safe_mode`
and `libapp.so` is not loaded). Fix it by reinstalling the launcher APK with
`adb install -r <MiuiHome.apk>`; do not delete data, files, or properties.
Keep the previous known-good MiuiHome APK on hand before every native test.

## Reverse Engineering Toolkit

- `libapp.so` has a `.gnu_debugdata` section: XZ-compressed ELF containing the
  full Dart symbol table. Decompress and use `readelf -sW` for function names
  and offsets.
- Dart AOT facts: `x27` is the object pool pointer, `x28 << 32` supplies the
  high bits when decompressing 32-bit pointers, `x22` holds the null object,
  and the bool singletons are `x22+0x20` (true) and `x22+0x30` (false), so a
  bool condition often appears as `tbnz/tbz wN, #4`.
- To observe the pool or objects at runtime, read the target process memory as
  root (`/proc/<pid>/mem`); a small static NDK scanner is enough.
- Strings in the APK are authoritative for user-visible behavior (toasts, log
  messages); Dart symbol names are authoritative for logic.
- jadx is useless for the Flutter launcher (no dex) but remains the right tool
  for any legacy launcher APK and for the module's own Java side.

## Code and Commit Conventions

- Language: code comments and commit messages in English.
- Commits: a single short English line describing the change, matching the
  existing `git log` style. Commit only when asked; never push unless asked.
- This project is GPL-3.0 with no per-file license headers. Do not import
  SPDX/license headers from reference projects; if code is adapted, keep a
  short attribution comment instead of a foreign license identifier.
- Do not add comments to code unless they explain a non-obvious contract
  (for example why a fingerprint is shaped the way it is).

## Operating Boundaries

- Never write Android system properties, even with already-present values.
- Never modify files under `/data/adb/modules` or install helpers under
  `/system/bin`.
- Keep the static scope minimal; do not add new packages to `scope.list` for a
  feature unless its process genuinely must be hooked.
- A native patch must never be enabled unless its fingerprint and target range
  were both verified; fail closed and look like stock behavior otherwise.
- Keep the Java and native hook families independent: a failure in one must not
  affect the other, and new features should be added to whichever family owns
  the target process.
