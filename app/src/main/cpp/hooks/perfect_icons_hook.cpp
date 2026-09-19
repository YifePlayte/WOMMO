//
// WOMMO native hook: perfect icons for the Flutter launcher.
//
// The legacy framework composed MIUI theme icons from the layered PNG pairs in
// the icons package ('res/drawable-<density>/<pkg>/0.png' + '1.png').  The
// Flutter launcher dropped that lookup; its only layered pipeline is the "mod
// icon" one (ModIconSource -> IconAssets.loadModLayers), which reads
// '/product/media/theme/miui_mod_icons/dynamic/<name>/0.png' and '1.png'.
//
// This hook feeds that existing pipeline from the theme and stock icons
// packages:
//
//   * the dynamic-directory probe inside IconAssets.loadModLayers is forced
//     open so candidates without a product mod directory are still attempted;
//   * libc open/openat are intercepted for the two layer paths only, with the
//     applied theme ('/data/system/theme/icons') taking priority over every
//     system-partition source.  Resolution order per layer file:
//     user theme -> original path (built-in product mod icons) -> stock icons
//     package ('/system/media/theme/default/icons' or its '/product' mount).
//     A layer is served either by direct path mapping when the package is an
//     extracted directory, or as a memfd holding the zip entry
//     'res/drawable-<density>/<name>/<index>.png'.
//
// Everything fails closed: a missing fingerprint or an unreadable layer source
// keeps the stock launcher behavior.

#include "hooks.h"

#include "../log.h"
#include "../lsposed_hook_backend.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <sched.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>

#include <zlib.h>

#ifndef MFD_CLOEXEC
#define MFD_CLOEXEC 0x0001U
#endif

namespace wommo::hooks {
namespace {

// ---------------------------------------------------------------------------
// Layer requests
// ---------------------------------------------------------------------------

constexpr char kModLayerPrefix[] =
        "/product/media/theme/miui_mod_icons/dynamic/";
constexpr size_t kModLayerPrefixLength = sizeof(kModLayerPrefix) - 1u;
constexpr char kLayerSuffix0[] = "/0.png";
constexpr char kLayerSuffix1[] = "/1.png";
constexpr size_t kLayerSuffixLength = sizeof(kLayerSuffix0) - 1u;
constexpr size_t kMaxLayerNameLength = 200u;

struct LayerRequest {
    char name[kMaxLayerNameLength + 1u];
    int index;
};

bool ParseLayerRequest(const char* path, LayerRequest* request) {
    if (path == nullptr || request == nullptr) return false;
    if (strncmp(path, kModLayerPrefix, kModLayerPrefixLength) != 0) {
        return false;
    }
    const size_t length = strlen(path);
    if (length <= kModLayerPrefixLength + kLayerSuffixLength) return false;
    const char* suffix = path + length - kLayerSuffixLength;
    int index;
    if (strcmp(suffix, kLayerSuffix0) == 0) {
        index = 0;
    } else if (strcmp(suffix, kLayerSuffix1) == 0) {
        index = 1;
    } else {
        return false;
    }
    const size_t name_length =
            length - kModLayerPrefixLength - kLayerSuffixLength;
    if (name_length == 0u || name_length > kMaxLayerNameLength) return false;
    memcpy(request->name, path + kModLayerPrefixLength, name_length);
    request->name[name_length] = '\0';
    if (strchr(request->name, '/') != nullptr) return false;
    request->index = index;
    return true;
}

// Layered icon packages in fallback order.  The applied theme lives in /data
// and must beat every system-partition package (including the built-in
// '/product/media/theme/miui_mod_icons/dynamic' files, which are consulted
// between the theme and the stock package); '/system' is the historical MIUI
// location and '/product' is what HyperOS actually mounts.  Indices 0 is the
// user theme, 1.. are the system fallbacks.
constexpr size_t kUserLayerSourceCount = 1u;
constexpr const char* kLayerSources[] = {
    "/data/system/theme/icons",
    "/system/media/theme/default/icons",
    "/product/media/theme/default/icons",
};
constexpr size_t kLayerSourceCount =
        sizeof(kLayerSources) / sizeof(kLayerSources[0]);

constexpr const char* kDensities[] = {
    "xxhdpi", "xxxhdpi", "xhdpi", "hdpi", "mdpi",
};

constexpr size_t kMaxCandidates = 6u;
constexpr size_t kDensityCount = sizeof(kDensities) / sizeof(kDensities[0]);
constexpr size_t kMaxTargets = kMaxCandidates * kDensityCount;
constexpr size_t kMaxEntryLength = 256u;
constexpr size_t kMaxFullPathLength = 320u;
constexpr uint64_t kMaxLayerSize = 8u * 1024u * 1024u;

struct CandidateTargets {
    char entries[kMaxTargets][kMaxEntryLength];
    size_t count;
};

void AddCandidate(char names[][kMaxLayerNameLength + 1u], size_t* count,
                  const char* value) {
    if (value[0] == '\0' || *count >= kMaxCandidates) return;
    for (size_t index = 0u; index < *count; ++index) {
        if (strcmp(names[index], value) == 0) return;
    }
    size_t length = strlen(value);
    if (length > kMaxLayerNameLength) length = kMaxLayerNameLength;
    memcpy(names[*count], value, length);
    names[*count][length] = '\0';
    ++*count;
}

// A per-activity candidate ('pkg#class') may also be stored under the app's
// package name.  Trailing class components must NOT be trimmed otherwise: a
// fork such as xyz.nextalone.nagram whose activities live in
// 'org.telegram.messenger.*' would otherwise show the Telegram layers.
void BuildTargets(const LayerRequest& request, CandidateTargets* targets) {
    char names[kMaxCandidates][kMaxLayerNameLength + 1u]{};
    size_t name_count = 0u;
    AddCandidate(names, &name_count, request.name);
    const char* hash = strchr(request.name, '#');
    if (hash != nullptr && hash != request.name) {
        char package[kMaxLayerNameLength + 1u];
        size_t length = static_cast<size_t>(hash - request.name);
        if (length <= kMaxLayerNameLength) {
            memcpy(package, request.name, length);
            package[length] = '\0';
            AddCandidate(names, &name_count, package);
        }
    }

    targets->count = 0u;
    for (size_t name = 0u; name < name_count; ++name) {
        for (const char* density : kDensities) {
            if (targets->count >= kMaxTargets) return;
            snprintf(targets->entries[targets->count], kMaxEntryLength,
                     "res/drawable-%s/%s/%d.png", density, names[name],
                     request.index);
            ++targets->count;
        }
    }
}

// ---------------------------------------------------------------------------
// Layer source (directory or zip)
// ---------------------------------------------------------------------------

struct ZipArchive {
    const uint8_t* data;
    size_t size;
    uint32_t ready;
    uint32_t failed;
};

ZipArchive g_zips[kLayerSourceCount]{};
uint32_t g_zip_lock = 0u;

using OpenFn = int (*)(const char*, int, ...);
using OpenAtFn = int (*)(int, const char*, int, ...);

OpenFn g_original_open = nullptr;
OpenAtFn g_original_openat = nullptr;

int OpenOriginal(const char* path, int flags) {
    OpenFn original = __atomic_load_n(&g_original_open, __ATOMIC_ACQUIRE);
    if (original == nullptr) {
        errno = ENOSYS;
        return -1;
    }
    return original(path, flags);
}

int OpenRegularFile(const char* path) {
    const int fd = OpenOriginal(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return -1;
    struct stat status {};
    if (fstat(fd, &status) != 0 || !S_ISREG(status.st_mode)) {
        close(fd);
        errno = ENOENT;
        return -1;
    }
    return fd;
}

uint16_t ReadU16(const uint8_t* data) {
    return static_cast<uint16_t>(
            static_cast<uint16_t>(data[0]) |
            static_cast<uint16_t>(static_cast<uint16_t>(data[1]) << 8));
}

uint32_t ReadU32(const uint8_t* data) {
    return static_cast<uint32_t>(data[0]) |
            (static_cast<uint32_t>(data[1]) << 8) |
            (static_cast<uint32_t>(data[2]) << 16) |
            (static_cast<uint32_t>(data[3]) << 24);
}

uint64_t ReadU64(const uint8_t* data) {
    return static_cast<uint64_t>(ReadU32(data)) |
            (static_cast<uint64_t>(ReadU32(data + 4u)) << 32);
}

bool LocateEocd(const ZipArchive& zip, size_t* offset) {
    if (zip.size < 22u) return false;
    const size_t window = zip.size < 66000u ? zip.size : 66000u;
    size_t cursor = zip.size - 22u;
    const size_t lowest = zip.size - window;
    for (;;) {
        if (ReadU32(zip.data + cursor) == 0x06054B50u) {
            const uint16_t comment = ReadU16(zip.data + cursor + 20u);
            if (cursor + 22u + comment <= zip.size) {
                *offset = cursor;
                return true;
            }
        }
        if (cursor == lowest) break;
        --cursor;
    }
    return false;
}

bool LocateCentralDirectory(const ZipArchive& zip, size_t* offset,
                            size_t* count) {
    size_t eocd = 0u;
    if (!LocateEocd(zip, &eocd)) return false;
    uint64_t entry_count = ReadU16(zip.data + eocd + 10u);
    uint64_t directory_size = ReadU32(zip.data + eocd + 12u);
    uint64_t directory_offset = ReadU32(zip.data + eocd + 16u);
    if (entry_count == 0xFFFFu || directory_offset == 0xFFFFFFFFu ||
            directory_size == 0xFFFFFFFFu) {
        if (eocd < 20u) return false;
        const uint8_t* locator = zip.data + eocd - 20u;
        if (ReadU32(locator) != 0x07064B50u) return false;
        const uint64_t zip64 = ReadU64(locator + 8u);
        if (zip64 > zip.size || zip64 > zip.size - 56u) return false;
        const uint8_t* record = zip.data + zip64;
        if (ReadU32(record) != 0x06064B50u) return false;
        entry_count = ReadU64(record + 32u);
        directory_size = ReadU64(record + 40u);
        directory_offset = ReadU64(record + 48u);
    }
    if (directory_offset > zip.size ||
            directory_size > zip.size - directory_offset) {
        return false;
    }
    *offset = static_cast<size_t>(directory_offset);
    *count = static_cast<size_t>(entry_count);
    return true;
}

struct ZipEntry {
    uint16_t method;
    uint64_t compressed_size;
    uint64_t uncompressed_size;
    uint64_t local_offset;
    uint64_t data_offset;
};

void ApplyZip64Extra(const uint8_t* extra, uint16_t extra_length,
                     ZipEntry* entry) {
    size_t cursor = 0u;
    while (cursor + 4u <= extra_length) {
        const uint16_t id = ReadU16(extra + cursor);
        const uint16_t size = ReadU16(extra + cursor + 2u);
        cursor += 4u;
        if (cursor + size > extra_length) return;
        if (id == 0x0001u) {
            size_t inner = cursor;
            if (entry->uncompressed_size == 0xFFFFFFFFu &&
                    inner + 8u <= cursor + size) {
                entry->uncompressed_size = ReadU64(extra + inner);
                inner += 8u;
            }
            if (entry->compressed_size == 0xFFFFFFFFu &&
                    inner + 8u <= cursor + size) {
                entry->compressed_size = ReadU64(extra + inner);
                inner += 8u;
            }
            if (entry->local_offset == 0xFFFFFFFFu &&
                    inner + 8u <= cursor + size) {
                entry->local_offset = ReadU64(extra + inner);
            }
            return;
        }
        cursor += size;
    }
}

bool FindBestEntry(const ZipArchive& zip, const CandidateTargets& targets,
                   ZipEntry* entry, size_t* best_target) {
    size_t directory = 0u;
    size_t count = 0u;
    if (!LocateCentralDirectory(zip, &directory, &count)) return false;
    size_t best = kMaxTargets;
    size_t cursor = directory;
    bool found = false;
    for (size_t index = 0u; index < count; ++index) {
        if (cursor + 46u > zip.size) break;
        const uint8_t* record = zip.data + cursor;
        if (ReadU32(record) != 0x02014B50u) break;
        const uint16_t name_length = ReadU16(record + 28u);
        const uint16_t extra_length = ReadU16(record + 30u);
        const uint16_t comment_length = ReadU16(record + 32u);
        const size_t record_length =
                46u + name_length + extra_length + comment_length;
        if (cursor + record_length > zip.size) break;
        const uint8_t* name = record + 46u;
        for (size_t target = 0u; target < targets.count && target < best;
             ++target) {
            const size_t target_length = strlen(targets.entries[target]);
            if (target_length != name_length ||
                    memcmp(name, targets.entries[target], name_length) != 0) {
                continue;
            }
            entry->method = ReadU16(record + 10u);
            entry->compressed_size = ReadU32(record + 20u);
            entry->uncompressed_size = ReadU32(record + 24u);
            entry->local_offset = ReadU32(record + 42u);
            ApplyZip64Extra(name + name_length, extra_length, entry);
            best = target;
            found = true;
            break;
        }
        cursor += record_length;
    }
    if (found) *best_target = best;
    return found;
}

bool ResolveDataOffset(const ZipArchive& zip, ZipEntry* entry) {
    if (entry->local_offset > zip.size ||
            zip.size - entry->local_offset < 30u) {
        return false;
    }
    const uint8_t* local = zip.data + entry->local_offset;
    if (ReadU32(local) != 0x04034B50u) return false;
    const uint64_t data = entry->local_offset + 30u + ReadU16(local + 26u) +
            ReadU16(local + 28u);
    if (data > zip.size || entry->compressed_size > zip.size - data) {
        return false;
    }
    entry->data_offset = data;
    return true;
}

bool WriteAll(int fd, const uint8_t* data, size_t size) {
    size_t written = 0u;
    while (written < size) {
        const ssize_t result = write(fd, data + written, size - written);
        if (result < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        written += static_cast<size_t>(result);
    }
    return true;
}

bool ExtractEntry(const ZipArchive& zip, const ZipEntry& entry,
                  uint8_t* output) {
    if (entry.method == 0u) {
        if (entry.compressed_size != entry.uncompressed_size) return false;
        memcpy(output, zip.data + entry.data_offset,
               static_cast<size_t>(entry.compressed_size));
        return true;
    }
    if (entry.method != 8u) return false;
    z_stream stream{};
    stream.next_in = const_cast<Bytef*>(zip.data + entry.data_offset);
    stream.avail_in = static_cast<uInt>(entry.compressed_size);
    stream.next_out = output;
    stream.avail_out = static_cast<uInt>(entry.uncompressed_size);
    if (inflateInit2(&stream, -MAX_WBITS) != Z_OK) return false;
    const int result = inflate(&stream, Z_FINISH);
    const uLong produced = stream.total_out;
    inflateEnd(&stream);
    return result == Z_STREAM_END &&
            produced == static_cast<uLong>(entry.uncompressed_size);
}

uint32_t g_source_log_state = 0u;
uint32_t g_serve_log_state = 0u;
uint32_t g_memfd_log_state = 0u;

int ServeZipLayer(const CandidateTargets& targets, size_t first_source,
                  size_t last_source, size_t* source_index) {
    for (size_t index = first_source; index < last_source; ++index) {
        const ZipArchive& zip = g_zips[index];
        if (__atomic_load_n(&zip.ready, __ATOMIC_ACQUIRE) == 0u) continue;
        ZipEntry entry{};
        size_t target = 0u;
        if (!FindBestEntry(zip, targets, &entry, &target)) continue;
        if (entry.uncompressed_size == 0u ||
                entry.uncompressed_size > kMaxLayerSize ||
                entry.compressed_size > kMaxLayerSize ||
                !ResolveDataOffset(zip, &entry)) {
            continue;
        }
        const int fd = static_cast<int>(
                syscall(__NR_memfd_create, "wommo_layer", MFD_CLOEXEC));
        if (fd < 0) {
            if (__atomic_exchange_n(&g_memfd_log_state, 1u, __ATOMIC_ACQ_REL) ==
                    0u) {
                WOMMO_LOGE("perfect_icons: memfd_create failed (%d)", errno);
            }
            return -1;
        }
        uint8_t* buffer = static_cast<uint8_t*>(
                malloc(static_cast<size_t>(entry.uncompressed_size)));
        if (buffer == nullptr) {
            close(fd);
            errno = ENOMEM;
            return -1;
        }
        bool ok = ExtractEntry(zip, entry, buffer);
        if (ok) {
            ok = WriteAll(fd, buffer,
                          static_cast<size_t>(entry.uncompressed_size));
        }
        free(buffer);
        if (!ok || lseek(fd, 0, SEEK_SET) != 0) {
            close(fd);
            errno = EIO;
            return -1;
        }
        *source_index = index;
        return fd;
    }
    return -1;
}

bool EnsureZips() {
    while (__atomic_exchange_n(&g_zip_lock, 1u, __ATOMIC_ACQUIRE) != 0u) {
        sched_yield();
    }
    bool any_ready = false;
    for (size_t index = 0u; index < kLayerSourceCount; ++index) {
        ZipArchive& zip = g_zips[index];
        if (__atomic_load_n(&zip.ready, __ATOMIC_RELAXED) != 0u) {
            any_ready = true;
            continue;
        }
        if (__atomic_load_n(&zip.failed, __ATOMIC_RELAXED) != 0u) continue;
        const int fd = OpenRegularFile(kLayerSources[index]);
        if (fd < 0) {
            __atomic_store_n(&zip.failed, 1u, __ATOMIC_RELAXED);
            continue;
        }
        struct stat status {};
        if (fstat(fd, &status) != 0 || status.st_size <= 0) {
            close(fd);
            __atomic_store_n(&zip.failed, 1u, __ATOMIC_RELAXED);
            continue;
        }
        void* map = mmap(nullptr, static_cast<size_t>(status.st_size),
                         PROT_READ, MAP_PRIVATE, fd, 0);
        close(fd);
        if (map == MAP_FAILED) {
            __atomic_store_n(&zip.failed, 1u, __ATOMIC_RELAXED);
            continue;
        }
        zip.data = static_cast<const uint8_t*>(map);
        zip.size = static_cast<size_t>(status.st_size);
        __atomic_store_n(&zip.ready, 1u, __ATOMIC_RELEASE);
        any_ready = true;
        WOMMO_LOGW("perfect_icons: layer source %s (%zu bytes)",
                   kLayerSources[index], zip.size);
    }
    __atomic_store_n(&g_zip_lock, 0u, __ATOMIC_RELEASE);
    if (!any_ready &&
            __atomic_exchange_n(&g_source_log_state, 1u, __ATOMIC_ACQ_REL) ==
                    0u) {
        WOMMO_LOGW("perfect_icons: no layer source found, keeping stock "
                   "icons");
    }
    return any_ready;
}

int ServeLayerFileRange(const LayerRequest& request, size_t first_source,
                        size_t last_source) {
    if (first_source >= last_source) return -1;
    CandidateTargets targets{};
    BuildTargets(request, &targets);

    for (size_t index = first_source; index < last_source; ++index) {
        const char* source = kLayerSources[index];
        for (size_t target = 0u; target < targets.count; ++target) {
            char path[kMaxFullPathLength];
            const int length = snprintf(path, sizeof(path), "%s/%s", source,
                                        targets.entries[target]);
            if (length <= 0 || static_cast<size_t>(length) >= sizeof(path)) {
                continue;
            }
            const int fd = OpenRegularFile(path);
            if (fd >= 0) return fd;
        }
    }

    if (EnsureZips()) {
        size_t source_index = 0u;
        const int fd = ServeZipLayer(targets, first_source, last_source,
                                     &source_index);
        if (fd >= 0) {
            if (__atomic_exchange_n(&g_serve_log_state, 1u, __ATOMIC_ACQ_REL) ==
                    0u) {
                WOMMO_LOGW("perfect_icons: serving layers from %s for %s",
                           kLayerSources[source_index], request.name);
            }
            return fd;
        }
    }
    errno = ENOENT;
    return -1;
}

// The applied theme wins over everything; the built-in product mod icons
// (the original path) are consulted in between; the stock icon package is the
// last resort.
int ServeUserLayerFile(const LayerRequest& request) {
    return ServeLayerFileRange(request, 0u, kUserLayerSourceCount);
}

int ServeSystemLayerFiles(const LayerRequest& request) {
    return ServeLayerFileRange(request, kUserLayerSourceCount,
                               kLayerSourceCount);
}

// ---------------------------------------------------------------------------
// libc file hooks
// ---------------------------------------------------------------------------

bool ShouldIntercept(const char* path, int flags, LayerRequest* request) {
    if (path == nullptr || (flags & O_ACCMODE) != O_RDONLY) return false;
    return ParseLayerRequest(path, request);
}

// The original pointers are published right after each inline hook is
// installed; before that the sibling libc entry point is used as a fallback so
// a concurrent open never fails just because the install is in flight.
int InvokeOriginalOpen(const char* path, int flags, mode_t mode) {
    OpenFn original = __atomic_load_n(&g_original_open, __ATOMIC_ACQUIRE);
    if (original != nullptr) {
        return (flags & O_CREAT) != 0 ? original(path, flags, mode)
                                      : original(path, flags);
    }
    OpenAtFn original_at =
            __atomic_load_n(&g_original_openat, __ATOMIC_ACQUIRE);
    if (original_at != nullptr) {
        return (flags & O_CREAT) != 0
                ? original_at(AT_FDCWD, path, flags, mode)
                : original_at(AT_FDCWD, path, flags);
    }
    errno = ENOSYS;
    return -1;
}

int InvokeOriginalOpenAt(int dirfd, const char* path, int flags,
                         mode_t mode) {
    OpenAtFn original = __atomic_load_n(&g_original_openat, __ATOMIC_ACQUIRE);
    if (original != nullptr) {
        return (flags & O_CREAT) != 0 ? original(dirfd, path, flags, mode)
                                      : original(dirfd, path, flags);
    }
    if (path != nullptr && (path[0] == '/' || dirfd == AT_FDCWD)) {
        OpenFn original_open =
                __atomic_load_n(&g_original_open, __ATOMIC_ACQUIRE);
        if (original_open != nullptr) {
            return (flags & O_CREAT) != 0 ? original_open(path, flags, mode)
                                          : original_open(path, flags);
        }
    }
    errno = ENOSYS;
    return -1;
}

extern "C" int HookedOpen(const char* path, int flags, ...) {
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0) {
        va_list arguments;
        va_start(arguments, flags);
        mode = static_cast<mode_t>(va_arg(arguments, int));
        va_end(arguments);
    }
    LayerRequest request{};
    if (ShouldIntercept(path, flags, &request)) {
        const int theme = ServeUserLayerFile(request);
        if (theme >= 0) return theme;
        const int fd = InvokeOriginalOpen(path, flags, mode);
        if (fd >= 0) return fd;
        const int fallback = ServeSystemLayerFiles(request);
        if (fallback >= 0) return fallback;
        errno = ENOENT;
        return -1;
    }
    return InvokeOriginalOpen(path, flags, mode);
}

extern "C" int HookedOpenAt(int dirfd, const char* path, int flags, ...) {
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0) {
        va_list arguments;
        va_start(arguments, flags);
        mode = static_cast<mode_t>(va_arg(arguments, int));
        va_end(arguments);
    }
    LayerRequest request{};
    if ((path == nullptr || path[0] == '/' || dirfd == AT_FDCWD) &&
            ShouldIntercept(path, flags, &request)) {
        const int theme = ServeUserLayerFile(request);
        if (theme >= 0) return theme;
        const int fd = InvokeOriginalOpenAt(dirfd, path, flags, mode);
        if (fd >= 0) return fd;
        const int fallback = ServeSystemLayerFiles(request);
        if (fallback >= 0) return fallback;
        errno = ENOENT;
        return -1;
    }
    return InvokeOriginalOpenAt(dirfd, path, flags, mode);
}

uint32_t g_fs_state = 0u;

bool InstallFileHooks() {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_fs_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_fs_state == 2u;
    }
    bool ok = true;

    void* open_target = dlsym(RTLD_DEFAULT, "open");
    if (open_target == nullptr) {
        ok = false;
    } else {
        void* backup = nullptr;
        if (InstallInlineHook(open_target,
                              reinterpret_cast<void*>(HookedOpen),
                              &backup) != kHookSuccess ||
                backup == nullptr) {
            WOMMO_LOGE("perfect_icons: failed to hook open");
            ok = false;
        } else {
            __atomic_store_n(&g_original_open,
                             reinterpret_cast<OpenFn>(backup),
                             __ATOMIC_RELEASE);
        }
    }

    void* openat_target = dlsym(RTLD_DEFAULT, "openat");
    if (openat_target != nullptr) {
        void* backup = nullptr;
        if (InstallInlineHook(openat_target,
                              reinterpret_cast<void*>(HookedOpenAt),
                              &backup) != kHookSuccess ||
                backup == nullptr) {
            WOMMO_LOGE("perfect_icons: failed to hook openat");
            ok = false;
        } else {
            __atomic_store_n(&g_original_openat,
                             reinterpret_cast<OpenAtFn>(backup),
                             __ATOMIC_RELEASE);
        }
    } else {
        WOMMO_LOGW("perfect_icons: openat not found, open hook only");
    }

    __atomic_store_n(&g_fs_state, ok ? 2u : 0u, __ATOMIC_RELEASE);
    return ok;
}

// ---------------------------------------------------------------------------
// Dart patch
// ---------------------------------------------------------------------------

// IconAssets.loadModLayers, right after the awaited _hasDynamicDir probe:
//
//     add  x16, x22, #0x20   ; true singleton
//     cmp  w0, w16
//     b.eq <build path>      ; taken when the package has a dynamic dir
//     ldur x6, [x29, #-0xe0] ; otherwise: next candidate
//     ldur x5, [x29, #-0xe8]
//
// The branch is replaced with an unconditional one, so every candidate is
// attempted and the open hooks can serve the missing files.
constexpr size_t kProbePatternLength = 5u;
constexpr uint32_t kProbePattern[kProbePatternLength] = {
    0x910082D0u,  // add  x16, x22, #0x20
    0x6B10001Fu,  // cmp  w0, w16
    0x54000080u,  // b.eq +0x10
    0xF85203A6u,  // ldur x6, [x29, #-0xe0]
    0xF85183A5u,  // ldur x5, [x29, #-0xe8]
};
constexpr uint32_t kUnconditionalBranch = 0x14000004u;  // b +0x10

uint32_t g_patch_state = 0u;

bool InstallProbePatch(const dart::Image& image) {
    uint32_t expected = 0u;
    if (!__atomic_compare_exchange_n(&g_patch_state, &expected, 1u, false,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        return g_patch_state == 2u;
    }

    uintptr_t probe = 0u;
    uint32_t matches = 0u;
    if (!dart::FindUniqueWordPattern(image, kProbePattern, kProbePatternLength,
                                     &probe, &matches) ||
            !dart::RangeInExecutableSegment(
                    image, probe, kProbePatternLength * sizeof(uint32_t))) {
        WOMMO_LOGW("perfect_icons: mod layer probe pattern rejected "
                   "(matches=%u), keeping original behavior",
                   matches);
        __atomic_store_n(&g_patch_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    const uintptr_t branch = probe + 2u * sizeof(uint32_t);
    const uint32_t patch[1] = {kUnconditionalBranch};
    if (!dart::PatchCode(branch, patch, 1u)) {
        WOMMO_LOGE("perfect_icons: failed to patch mod layer probe");
        __atomic_store_n(&g_patch_state, 0u, __ATOMIC_RELEASE);
        return false;
    }

    __atomic_store_n(&g_patch_state, 2u, __ATOMIC_RELEASE);
    WOMMO_LOGW("perfect_icons: mod layer probe forced at 0x%" PRIxPTR
               " (file offset 0x%" PRIxPTR ")",
               branch, branch - image.bias);
    return true;
}

}  // namespace

bool InstallPerfectIconsHook(const dart::Image& image) {
    // The file hooks are useful even if the Dart patch fails: without the
    // patch the launcher never requests the paths they serve.
    const bool file_hooks = InstallFileHooks();
    const bool patched = InstallProbePatch(image);
    return file_hooks && patched;
}

}  // namespace wommo::hooks
