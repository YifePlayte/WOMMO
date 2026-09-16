
#include "dart_image.h"

#include "log.h"
#include "lsposed_hook_backend.h"

#include <dlfcn.h>
#include <elf.h>
#include <errno.h>
#include <inttypes.h>
#include <link.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

namespace wommo::dart {
namespace {

bool StringsEqual(const char* left, const char* right) {
    if (left == nullptr || right == nullptr) return false;
    size_t index = 0u;
    while (left[index] != '\0' && right[index] != '\0') {
        if (left[index] != right[index]) return false;
        ++index;
    }
    return left[index] == right[index];
}

bool EndsWith(const char* value, const char* suffix) {
    if (value == nullptr || suffix == nullptr) return false;
    const size_t value_length = strlen(value);
    const size_t suffix_length = strlen(suffix);
    if (suffix_length > value_length) return false;
    return StringsEqual(value + value_length - suffix_length, suffix);
}

size_t PageSize() {
    static size_t value = 0u;
    if (value == 0u) {
        const long queried = sysconf(_SC_PAGESIZE);
        value = queried > 0 ? static_cast<size_t>(queried) : size_t{4096};
    }
    return value;
}

uintptr_t PageStart(uintptr_t address) {
    return address & ~(static_cast<uintptr_t>(PageSize()) - 1u);
}

struct FindImageRequest {
    const char* library_name;
    Image* image;
};

int FindImageCallback(dl_phdr_info* info, size_t, void* opaque) {
    auto* request = static_cast<FindImageRequest*>(opaque);
    if (info->dlpi_name == nullptr || request->image->found) return 0;
    if (!EndsWith(info->dlpi_name, request->library_name)) return 0;

    request->image->bias = static_cast<uintptr_t>(info->dlpi_addr);
    for (size_t index = 0u;
         index < info->dlpi_phnum && request->image->segment_count < 8u;
         ++index) {
        const ElfW(Phdr)& program = info->dlpi_phdr[index];
        if (program.p_type != PT_LOAD || program.p_memsz == 0u) continue;
        request->image->segments[request->image->segment_count++] = {
            static_cast<uintptr_t>(info->dlpi_addr + program.p_vaddr),
            static_cast<size_t>(program.p_memsz),
            static_cast<uint32_t>(program.p_flags),
        };
    }
    request->image->found = true;
    return 1;
}

struct FindImageAtBaseRequest {
    uintptr_t base;
    Image* image;
};

int FindImageAtBaseCallback(dl_phdr_info* info, size_t, void* opaque) {
    auto* request = static_cast<FindImageAtBaseRequest*>(opaque);
    if (request->image->found) return 0;
    if (static_cast<uintptr_t>(info->dlpi_addr) != request->base) return 0;

    request->image->bias = request->base;
    for (size_t index = 0u;
         index < info->dlpi_phnum && request->image->segment_count < 8u;
         ++index) {
        const ElfW(Phdr)& program = info->dlpi_phdr[index];
        if (program.p_type != PT_LOAD || program.p_memsz == 0u) continue;
        request->image->segments[request->image->segment_count++] = {
            static_cast<uintptr_t>(info->dlpi_addr + program.p_vaddr),
            static_cast<size_t>(program.p_memsz),
            static_cast<uint32_t>(program.p_flags),
        };
    }
    request->image->found = true;
    return 1;
}

}  // namespace

bool AcquireImage(const char* library_name, Image* image) {
    if (library_name == nullptr || image == nullptr) return false;
    FindImageRequest request{library_name, image};
    dl_iterate_phdr(FindImageCallback, &request);
    return image->found && image->segment_count != 0u;
}

bool AcquireImageAtBase(uintptr_t base, Image* image) {
    if (base == 0u || image == nullptr) return false;
    FindImageAtBaseRequest request{base, image};
    dl_iterate_phdr(FindImageAtBaseCallback, &request);
    return image->found && image->segment_count != 0u;
}

const Segment* FindSegment(const Image& image, uintptr_t address,
                           uint32_t required_flags) {
    for (size_t index = 0u; index < image.segment_count; ++index) {
        const Segment& segment = image.segments[index];
        if ((segment.flags & required_flags) == required_flags &&
                address >= segment.start &&
                address < segment.start + segment.size) {
            return &segment;
        }
    }
    return nullptr;
}

bool RangeInExecutableSegment(const Image& image, uintptr_t address,
                              size_t size) {
    const Segment* segment = FindSegment(image, address, PF_R | PF_X);
    if (segment == nullptr) return false;
    return address + size <= segment->start + segment->size;
}

bool FindUniqueWordPattern(const Image& image, const uint32_t* pattern,
                           size_t count, uintptr_t* match,
                           uint32_t* match_count) {
    if (pattern == nullptr || count == 0u || match == nullptr) return false;
    PatternWord masked[64]{};
    if (count > 64u) return false;
    for (size_t index = 0u; index < count; ++index) {
        masked[index] = {pattern[index], 0xFFFFFFFFu};
    }
    return FindUniqueMaskedPattern(image, masked, count, match, match_count);
}

bool FindUniqueMaskedPattern(const Image& image, const PatternWord* pattern,
                             size_t count, uintptr_t* match,
                             uint32_t* match_count) {
    if (pattern == nullptr || count == 0u || match == nullptr) return false;
    *match = 0u;
    uint32_t found = 0u;
    for (size_t index = 0u; index < image.segment_count; ++index) {
        const Segment& segment = image.segments[index];
        if ((segment.flags & (PF_R | PF_X)) != (PF_R | PF_X)) continue;
        const uint32_t* words = reinterpret_cast<const uint32_t*>(segment.start);
        const size_t word_count = segment.size / sizeof(uint32_t);
        if (word_count < count) continue;
        for (size_t word = 0u; word + count <= word_count; ++word) {
            bool full_match = true;
            for (size_t offset = 0u; offset < count; ++offset) {
                const PatternWord& entry = pattern[offset];
                if (entry.mask == 0u) continue;
                if ((words[word + offset] & entry.mask) !=
                        (entry.value & entry.mask)) {
                    full_match = false;
                    break;
                }
            }
            if (!full_match) continue;
            *match = segment.start + word * sizeof(uint32_t);
            ++found;
        }
    }
    if (match_count != nullptr) *match_count = found;
    if (found != 1u) {
        *match = 0u;
        return false;
    }
    return true;
}

bool PatchCode(uintptr_t address, const uint32_t* words, size_t count) {
    if (words == nullptr || count == 0u) return false;
    const size_t length = count * sizeof(uint32_t);
    // Register the range before modifying it: a concurrent
    // madvise(MADV_DONTNEED) from HyperOS cleanup would otherwise drop the
    // copy-on-write pages and revert the patch.  Fail closed when the range
    // cannot be protected.
    if (!ProtectHookRange(address, length)) {
        WOMMO_LOGE("cannot protect hook range at 0x%" PRIxPTR, address);
        return false;
    }
    const uintptr_t page = PageStart(address);
    const uintptr_t end = address + length;
    const size_t pages = (end - page + PageSize() - 1u) / PageSize();
    if (mprotect(reinterpret_cast<void*>(page), pages * PageSize(),
                 PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        WOMMO_LOGE("mprotect RWX failed for 0x%" PRIxPTR ": %d", address,
                   errno);
        return false;
    }
    memcpy(reinterpret_cast<void*>(address), words, length);
    __builtin___clear_cache(reinterpret_cast<char*>(address),
                            reinterpret_cast<char*>(address) + length);
    if (mprotect(reinterpret_cast<void*>(page), pages * PageSize(),
                 PROT_READ | PROT_EXEC) != 0) {
        WOMMO_LOGE("mprotect RX restore failed for 0x%" PRIxPTR ": %d",
                   address, errno);
        return false;
    }
    const uint32_t* applied = reinterpret_cast<const uint32_t*>(address);
    for (size_t index = 0u; index < count; ++index) {
        if (applied[index] != words[index]) {
            WOMMO_LOGE("patch verification failed at 0x%" PRIxPTR,
                       address + index * sizeof(uint32_t));
            return false;
        }
    }
    return true;
}

}  // namespace wommo::dart
