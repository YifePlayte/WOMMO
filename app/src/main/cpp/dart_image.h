//
// WOMMO native hook: Dart AOT image discovery and code patching helpers.
//
// The MiuiHome launcher is a pure Dart AOT application: all business logic
// lives in libapp.so (mapped straight out of the APK, no dex).  Feature hooks
// locate instruction patterns in the executable segments and patch them.
// This module owns the generic plumbing only:
//
//   * locating the currently mapped Dart image via dl_iterate_phdr
//   * segment lookups / executable-range validation
//   * unique word-pattern scanning (fail closed on missing/ambiguous matches)
//   * mprotect-based instruction patching with a read-back verification

#pragma once

#include <stddef.h>
#include <stdint.h>

namespace wommo::dart {

struct Segment {
    uintptr_t start;
    size_t size;
    uint32_t flags;
};

struct Image {
    uintptr_t bias;
    Segment segments[8];
    size_t segment_count;
    bool found;
};

// Locates the mapped image whose path ends with `library_name` (typically
// "libapp.so").  Returns false when the library is not mapped.  This matches
// by name only; prefer AcquireImageAtBase for launcher images so the resolved
// ELF is pinned to a verified handle.
bool AcquireImage(const char* library_name, Image* image);

// Locates the mapped image whose load bias equals `base` (the dli_fbase that
// dladdr reports for a symbol exported by the image).  This is the strict
// lookup: it pins the segments of the exact library instance whose handle the
// caller already authenticated.
bool AcquireImageAtBase(uintptr_t base, Image* image);

// Returns the segment containing `address` with all `required_flags` set, or
// nullptr.  `required_flags` is a PF_* mask.
const Segment* FindSegment(const Image& image, uintptr_t address,
                           uint32_t required_flags);

// True when [address, address + size) lies inside one executable segment.
bool RangeInExecutableSegment(const Image& image, uintptr_t address,
                              size_t size);

// Scans all executable segments for `pattern` and returns the number of
// matches.  On exactly one match, *match is set to its address.  Missing or
// ambiguous patterns fail closed: *match stays 0 so callers never patch an
// unverified location.
bool FindUniqueWordPattern(const Image& image, const uint32_t* pattern,
                           size_t count, uintptr_t* match,
                           uint32_t* match_count);

// A masked instruction pattern word: a word matches when
// (word & mask) == (value & mask).  A mask of 0 skips the word, which lets
// callers ignore pool-page operands whose immediates differ between builds.
struct PatternWord {
    uint32_t value;
    uint32_t mask;
};

// Masked variant of FindUniqueWordPattern with the same fail-closed contract.
bool FindUniqueMaskedPattern(const Image& image, const PatternWord* pattern,
                             size_t count, uintptr_t* match,
                             uint32_t* match_count);

// Overwrites `count` 32-bit words at `address` through mprotect(RWX).  The
// target page permissions are restored to RX afterwards.  The write is
// verified by reading the words back.
bool PatchCode(uintptr_t address, const uint32_t* words, size_t count);

}  // namespace wommo::dart
