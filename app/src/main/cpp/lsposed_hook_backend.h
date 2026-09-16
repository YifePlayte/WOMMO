// LSPosed native hook backend for WOMMO.
//
// Keeps the LSPosed native API entries in one place so native features can
// install inline hooks without an external hooking library, and owns the
// madvise(MADV_DONTNEED) guard that keeps modified copy-on-write code pages
// alive under HyperOS memory cleanup.

#pragma once

#include <stddef.h>
#include <stdint.h>

struct NativeAPIEntries {
    uint32_t version;
    int (*hookFunc)(void* target, void* replacement, void** backup);
    int (*unhookFunc)(void* target);
};

using NativeOnModuleLoaded = void (*)(const char* name, void* handle);

constexpr int kHookSuccess = 0;
constexpr int kHookFailed = 1;

bool InitializeLsposedHookBackend(const NativeAPIEntries* entries);
int InstallInlineHook(void* target, void* replacement, void** original);
void* FindLibraryBase(const char* library_name);

// Installs the madvise guard on first use.  The guard intercepts
// madvise(MADV_DONTNEED) and preserves every registered hook page.  Returns
// false when the guard cannot be established; callers must fail closed.
bool EnsureMadviseGuard();

// Registers [address, address + size) as hook code that must survive
// MADV_DONTNEED.  Installs the guard on first use; a false result means the
// range is unprotected and the caller must not modify it.
bool ProtectHookRange(uintptr_t address, size_t size);
