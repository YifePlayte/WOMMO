//
// WOMMO native hook: launcher switch values.
//
// See native_config.h for the rationale.  Everything here is one-shot
// infrastructure: a bounded number of synchronous Binder queries followed by
// atomic publication.  No allocation, no thread and no Binder call happens on
// any feature hot path.

#include "native_config.h"

#include "../log.h"

#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>

#include <dlfcn.h>
#include <inttypes.h>
#include <sched.h>
#include <stdint.h>
#include <string.h>

// libbinder_ndk exports this, but the NDK header set does not declare it.
extern "C" AIBinder* AServiceManager_checkService(const char* service_name);

namespace wommo::config {
namespace {

// Wire format shared with NativeConfigEndpoint.kt.  The transaction code sits
// in the user range (0x00000001..0x00ffffff) and does not collide with the
// clipboard transaction codes or with the other HYOS native modules.
//
// The endpoint rides the clipboard service: HyperOS hides `window` (and
// `device_config`) from third-party processes, so WMS is not usable as a
// carrier for the weather and gallery processes, while `clipboard` resolves in
// every domain the module hooks and is far colder than a window transaction.
constexpr char kServiceName[] = "clipboard";
constexpr char kInterfaceDescriptor[] = "android.content.IClipboard";
constexpr transaction_code_t kTransactionCode = 0x00574F4Du;
constexpr int32_t kAck = 0x574F4D31;
// ack, available, flags, revision.
constexpr int32_t kReplyBytes = 4 + 4 + 4 + 8;

// Each attempt is one synchronous Binder transaction; the endpoint answers
// immediately, so a small budget is enough for a retry after a slow boot.
constexpr uint32_t kMaxRefreshAttempts = 3u;

// The binder API lives in libbinder_ndk; the probe decides whether the module
// namespace can already see it through the default group.
constexpr char kBinderNdkName[] = "libbinder_ndk.so";
constexpr char kProbeSymbol[] = "AServiceManager_checkService";
using CheckServiceFn = AIBinder* (*)(const char*);
using GetClassFn = const AIBinder_Class* (*)(AIBinder*);
using ClassDefineFn = AIBinder_Class* (*)(const char*, AIBinder_Class_onCreate,
                                          AIBinder_Class_onDestroy,
                                          AIBinder_Class_onTransact);
using ClassGetDescriptorFn = const char* (*)(const AIBinder_Class*);
using AssociateClassFn = bool (*)(AIBinder*, const AIBinder_Class*);
using PrepareTransactionFn = binder_status_t (*)(AIBinder*, AParcel**);
using TransactFn = binder_status_t (*)(AIBinder*, transaction_code_t, AParcel**,
                                       AParcel**, binder_flags_t);
using DecStrongFn = void (*)(AIBinder*);
using ParcelDeleteFn = void (*)(AParcel*);
using ParcelDataSizeFn = int32_t (*)(const AParcel*);
using ParcelReadInt32Fn = binder_status_t (*)(const AParcel*, int32_t*);
using ParcelReadInt64Fn = binder_status_t (*)(const AParcel*, int64_t*);

// 0 = unresolved, 1 = resolving, 2 = resolved, 3 = failed.
uint32_t g_symbol_state = 0u;
bool g_binder_ndk_ready = false;
void* g_binder_ndk_handle = nullptr;
CheckServiceFn g_check_service = nullptr;
GetClassFn g_get_class = nullptr;
ClassDefineFn g_class_define = nullptr;
ClassGetDescriptorFn g_class_get_descriptor = nullptr;
AssociateClassFn g_associate_class = nullptr;
PrepareTransactionFn g_prepare_transaction = nullptr;
TransactFn g_transact = nullptr;
DecStrongFn g_dec_strong = nullptr;
ParcelDeleteFn g_parcel_delete = nullptr;
ParcelDataSizeFn g_parcel_data_size = nullptr;
ParcelReadInt32Fn g_parcel_read_int32 = nullptr;
ParcelReadInt64Fn g_parcel_read_int64 = nullptr;

// 0 = no authoritative answer stored, 1 = authoritative answer stored.
uint32_t g_ready = 0u;
uint32_t g_flags = 0u;
uint64_t g_revision = 0u;
uint32_t g_refresh_attempts = 0u;
// One-shot log bits: successful query, unusable answer, unknown feature bits.
constexpr uint32_t kSuccessLogged = 1u << 0u;
constexpr uint32_t kUnavailableLogged = 1u << 1u;
constexpr uint32_t kUnknownBitsLogged = 1u << 2u;
uint32_t g_log_state = 0u;
// Result of the last query, published for the one-shot log.
const char* g_query_failure_stage = nullptr;
int32_t g_query_failure_status = 0;

bool StringsEqual(const char* left, const char* right) {
    if (left == nullptr || right == nullptr) return false;
    return strcmp(left, right) == 0;
}

bool MarkLogged(uint32_t bit) {
    const uint32_t previous =
            __atomic_fetch_or(&g_log_state, bit, __ATOMIC_ACQ_REL);
    return (previous & bit) == 0u;
}

// libbinder_ndk is mapped by every process that talks to a system service, but
// LSPosed loads the module library into a namespace whose default group does
// not include the process' global group, so `dlsym(RTLD_DEFAULT, ...)` cannot
// see it (observed on myron).  The fallback asks the linker for the soname:
// bionic then attaches the already loaded library to this namespace instead of
// mapping a second copy.  The handle must stay local - RTLD_GLOBAL would change
// symbol resolution for every other library in the process.
void* BinderLibrary() {
    if (g_binder_ndk_ready) return g_binder_ndk_handle;
    g_binder_ndk_ready = true;
    if (dlsym(RTLD_DEFAULT, kProbeSymbol) != nullptr) {
        g_binder_ndk_handle = RTLD_DEFAULT;
        return g_binder_ndk_handle;
    }
    g_binder_ndk_handle = dlopen(kBinderNdkName, RTLD_NOW);
    if (g_binder_ndk_handle == nullptr) {
        const char* reason = dlerror();
        WOMMO_LOGE("native config: cannot open %s: %s", kBinderNdkName,
                   reason != nullptr ? reason : "<unknown>");
        return nullptr;
    }
    WOMMO_LOGW("native config: %s attached to the module namespace",
               kBinderNdkName);
    return g_binder_ndk_handle;
}

void* BinderSymbol(const char* name) {
    void* library = BinderLibrary();
    return library == nullptr ? nullptr : dlsym(library, name);
}

template <typename T>
bool ResolveSymbol(T* slot, const char* name) {
    *slot = reinterpret_cast<T>(BinderSymbol(name));
    if (*slot == nullptr) {
        WOMMO_LOGE("native config: missing symbol %s", name);
        return false;
    }
    return true;
}

// A remote binder is associated with this class so that
// AIBinder_prepareTransaction writes the interface descriptor the Java side
// enforces.  The callbacks are never invoked: the class is used for its
// descriptor only.
void* ConfigBinderCreate(void*) { return nullptr; }

void ConfigBinderDestroy(void*) {}

binder_status_t ConfigBinderTransact(AIBinder*, transaction_code_t,
                                     const AParcel*, AParcel*) {
    return STATUS_UNKNOWN_TRANSACTION;
}

bool ResolveSymbolsLocked() {
    return ResolveSymbol(&g_check_service, "AServiceManager_checkService") &&
            ResolveSymbol(&g_get_class, "AIBinder_getClass") &&
            ResolveSymbol(&g_class_define, "AIBinder_Class_define") &&
            ResolveSymbol(&g_class_get_descriptor,
                          "AIBinder_Class_getDescriptor") &&
            ResolveSymbol(&g_associate_class, "AIBinder_associateClass") &&
            ResolveSymbol(&g_prepare_transaction,
                          "AIBinder_prepareTransaction") &&
            ResolveSymbol(&g_transact, "AIBinder_transact") &&
            ResolveSymbol(&g_dec_strong, "AIBinder_decStrong") &&
            ResolveSymbol(&g_parcel_delete, "AParcel_delete") &&
            ResolveSymbol(&g_parcel_data_size, "AParcel_getDataSize") &&
            ResolveSymbol(&g_parcel_read_int32, "AParcel_readInt32") &&
            ResolveSymbol(&g_parcel_read_int64, "AParcel_readInt64");
}

bool ResolveSymbols() {
    for (;;) {
        const uint32_t state =
                __atomic_load_n(&g_symbol_state, __ATOMIC_ACQUIRE);
        if (state == 2u) return true;
        if (state == 3u) return false;
        if (state == 0u) {
            uint32_t expected = 0u;
            if (__atomic_compare_exchange_n(&g_symbol_state, &expected, 1u,
                                            false, __ATOMIC_ACQ_REL,
                                            __ATOMIC_ACQUIRE)) {
                const bool resolved = ResolveSymbolsLocked();
                __atomic_store_n(&g_symbol_state, resolved ? 2u : 3u,
                                 __ATOMIC_RELEASE);
                if (!resolved) {
                    WOMMO_LOGE("native config: libbinder_ndk symbols unavailable");
                }
                return resolved;
            }
            continue;
        }
        sched_yield();
    }
}

// Associates this module's private class with the carrier binder so that
// AIBinder_prepareTransaction writes the interface descriptor the endpoint
// enforces.  A binder that already carries a class is reused as-is.
bool AssociateDescriptor(AIBinder* carrier, const char** stage) {
    const AIBinder_Class* existing = g_get_class(carrier);
    if (existing != nullptr) {
        const char* descriptor = g_class_get_descriptor(existing);
        if (descriptor == nullptr || !StringsEqual(descriptor, kInterfaceDescriptor)) {
            *stage = "descriptor";
            return false;
        }
        return true;
    }
    AIBinder_Class* defined =
            g_class_define(kInterfaceDescriptor, ConfigBinderCreate,
                           ConfigBinderDestroy, ConfigBinderTransact);
    if (defined == nullptr) {
        *stage = "class";
        return false;
    }
    if (!g_associate_class(carrier, defined)) {
        *stage = "associate";
        return false;
    }
    return true;
}

// The endpoint rides the clipboard service; a code it does not own falls through
// to the real service and returns an error reply immediately, so an unanswered
// query cannot block.  On failure the first failing step and its status are
// published for the one-shot log.
bool Query(uint32_t* out_flags, uint64_t* out_revision) {
    if (out_flags == nullptr || out_revision == nullptr) return false;
    const char* stage = nullptr;
    int32_t status = 0;
    bool accepted = false;

    if (!ResolveSymbols()) {
        stage = "symbols";
    } else {
        AIBinder* carrier = g_check_service(kServiceName);
        if (carrier == nullptr) {
            stage = "service";
        } else {
            AParcel* input = nullptr;
            AParcel* output = nullptr;
            if (!AssociateDescriptor(carrier, &stage)) {
                // stage already names the failing step.
            } else if ((status = g_prepare_transaction(carrier, &input)) != STATUS_OK ||
                       input == nullptr) {
                stage = "prepare";
            } else if ((status = g_transact(carrier, kTransactionCode, &input,
                                            &output, 0u)) != STATUS_OK ||
                       output == nullptr) {
                stage = "transact";
            } else if ((status = g_parcel_data_size(output)) != kReplyBytes) {
                stage = "reply";
            } else {
                int32_t ack = 0;
                int32_t available = 0;
                int32_t flags = 0;
                int64_t revision = 0;
                if (g_parcel_read_int32(output, &ack) != STATUS_OK) {
                    stage = "ack_read";
                } else if (g_parcel_read_int32(output, &available) != STATUS_OK) {
                    stage = "available_read";
                } else if (g_parcel_read_int32(output, &flags) != STATUS_OK) {
                    stage = "flags_read";
                } else if (g_parcel_read_int64(output, &revision) != STATUS_OK) {
                    stage = "revision_read";
                } else if (ack != kAck) {
                    stage = "ack";
                    status = ack;
                } else if (available != 0 && available != 1) {
                    stage = "available";
                    status = available;
                } else if (revision < 0) {
                    stage = "revision";
                    status = static_cast<int32_t>(revision);
                } else {
                    if ((static_cast<uint32_t>(flags) & ~kKnownFeatures) != 0u &&
                            MarkLogged(kUnknownBitsLogged)) {
                        WOMMO_LOGW("native config: unknown feature bits 0x%" PRIx32
                                   " ignored",
                                   static_cast<uint32_t>(flags) & ~kKnownFeatures);
                    }
                    if (available == 1) {
                        *out_flags =
                                static_cast<uint32_t>(flags) & kKnownFeatures;
                        *out_revision = static_cast<uint64_t>(revision);
                        accepted = true;
                    }
                }
            }
            // AIBinder_transact consumes the input parcel; a failed or skipped
            // transaction still owns it.
            if (input != nullptr) g_parcel_delete(input);
            if (output != nullptr) g_parcel_delete(output);
            g_dec_strong(carrier);
        }
    }

    if (stage != nullptr) {
        __atomic_store_n(&g_query_failure_stage, stage, __ATOMIC_RELEASE);
        __atomic_store_n(&g_query_failure_status, status, __ATOMIC_RELEASE);
    }
    return accepted;
}

}  // namespace

bool Refresh() {
    uint32_t flags = 0u;
    uint64_t revision = 0u;
    if (!Query(&flags, &revision)) {
        __atomic_store_n(&g_ready, 0u, __ATOMIC_RELEASE);
        if (MarkLogged(kUnavailableLogged)) {
            const char* stage =
                    __atomic_load_n(&g_query_failure_stage, __ATOMIC_ACQUIRE);
            WOMMO_LOGW("native config: endpoint unavailable "
                       "(stage=%s status=%d), gated hooks stay off",
                       stage != nullptr ? stage : "none",
                       __atomic_load_n(&g_query_failure_status,
                                       __ATOMIC_RELAXED));
        }
        return false;
    }
    __atomic_store_n(&g_flags, flags, __ATOMIC_RELAXED);
    __atomic_store_n(&g_revision, revision, __ATOMIC_RELAXED);
    __atomic_store_n(&g_ready, 1u, __ATOMIC_RELEASE);
    if (MarkLogged(kSuccessLogged)) {
        WOMMO_LOGW("native config: flags=0x%" PRIx32 " revision=%" PRIu64,
                   flags, revision);
    }
    return true;
}

bool EnsureReady() {
    if (__atomic_load_n(&g_ready, __ATOMIC_ACQUIRE) != 0u) return true;
    for (;;) {
        const uint32_t attempts =
                __atomic_load_n(&g_refresh_attempts, __ATOMIC_ACQUIRE);
        if (attempts >= kMaxRefreshAttempts) return false;
        uint32_t expected = attempts;
        if (__atomic_compare_exchange_n(&g_refresh_attempts, &expected,
                                        attempts + 1u, false, __ATOMIC_ACQ_REL,
                                        __ATOMIC_ACQUIRE)) {
            break;
        }
    }
    return Refresh();
}

bool Enabled(uint32_t feature) {
    if (feature == 0u) return true;
    if (__atomic_load_n(&g_ready, __ATOMIC_ACQUIRE) == 0u) return false;
    return (__atomic_load_n(&g_flags, __ATOMIC_ACQUIRE) & feature) == feature;
}

bool Ready() { return __atomic_load_n(&g_ready, __ATOMIC_ACQUIRE) != 0u; }

uint32_t Flags() { return __atomic_load_n(&g_flags, __ATOMIC_ACQUIRE); }

}  // namespace wommo::config
