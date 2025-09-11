cmake_minimum_required(VERSION 3.22.1)
project("fsowrapper")

add_library(${CMAKE_PROJECT_NAME} SHARED native_bridge.cpp)

add_library(SDL2 SHARED IMPORTED)
set_target_properties(SDL2 PROPERTIES IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSDL2.so")

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${CMAKE_SOURCE_DIR}/third_party/SDL2/)

target_link_libraries(${CMAKE_PROJECT_NAME} SDL2 android log)