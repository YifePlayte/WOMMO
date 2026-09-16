cmake_minimum_required(VERSION 3.22)

project(wommo_native LANGUAGES CXX)

if(NOT ANDROID)
    message(FATAL_ERROR "Must use the Android NDK toolchain")
endif()

if(NOT "${ANDROID_ABI}" STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Only arm64-v8a is supported; got '${ANDROID_ABI}'")
endif()

# Classhelper library (existing JNI helper)
add_subdirectory(classhelper)

# LSPosed native entry: one shared library carries the hook infrastructure and
# every feature hook.  Features live under hooks/ and are dispatched by
# wommo_native.cpp.
set(WOMMO_NATIVE_SOURCES
    wommo_native.cpp
    dart_image.cpp
    lsposed_hook_backend.cpp
    hooks/drag_to_pa_hook.cpp
    hooks/hide_landscape_nav_bar_hook.cpp
)

set(WOMMO_NATIVE_TARGET wommo_native)
set(WOMMO_NATIVE_EXPORTS exports-lsposed.map)

add_library(${WOMMO_NATIVE_TARGET} SHARED ${WOMMO_NATIVE_SOURCES})

target_include_directories(${WOMMO_NATIVE_TARGET} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_features(${WOMMO_NATIVE_TARGET} PRIVATE cxx_std_17)
target_compile_options(${WOMMO_NATIVE_TARGET} PRIVATE
    -fno-exceptions
    -fno-rtti
    -fno-threadsafe-statics
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -mbranch-protection=standard
    -Wall
    -Wextra
    -Werror
)

target_link_options(${WOMMO_NATIVE_TARGET} PRIVATE
    "-Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/${WOMMO_NATIVE_EXPORTS}"
    -Wl,--gc-sections
    -Wl,--no-undefined
    -Wl,-z,relro
    -Wl,-z,now
)

target_link_libraries(${WOMMO_NATIVE_TARGET} PRIVATE log dl)

set_target_properties(${WOMMO_NATIVE_TARGET} PROPERTIES
    CXX_EXTENSIONS OFF
    OUTPUT_NAME "${WOMMO_NATIVE_TARGET}"
)
