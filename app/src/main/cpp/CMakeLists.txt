cmake_minimum_required(VERSION 3.22.1)
project("touchcontrols")

add_library(${CMAKE_PROJECT_NAME} SHARED touch_controls.cpp)

add_library(SDL3 SHARED IMPORTED)
set_target_properties(SDL3 PROPERTIES IMPORTED_LOCATION
        "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSDL3.so")

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL3)

target_link_libraries(${CMAKE_PROJECT_NAME} SDL3 android log)

if(ANDROID_ABI STREQUAL "arm64-v8a" AND FSO_ADRENOTOOLS_DIR)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
    add_subdirectory("${FSO_ADRENOTOOLS_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/adrenotools")

    add_library(fsovulkan SHARED fsovulkan.cpp)
    target_include_directories(fsovulkan PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL3)
    target_compile_definitions(fsovulkan PRIVATE VK_USE_PLATFORM_ANDROID_KHR)
    target_link_libraries(fsovulkan PRIVATE adrenotools SDL3 dl log)
    add_dependencies(fsovulkan main_hook hook_impl)
endif()