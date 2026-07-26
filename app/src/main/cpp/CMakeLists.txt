cmake_minimum_required(VERSION 3.22.1)
project("touchcontrols")

add_library(${CMAKE_PROJECT_NAME} SHARED touch_controls.cpp)

add_library(SDL3 SHARED IMPORTED)
set_target_properties(SDL3 PROPERTIES IMPORTED_LOCATION
        "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSDL3.so")

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL3)

target_link_libraries(${CMAKE_PROJECT_NAME} SDL3 android log)