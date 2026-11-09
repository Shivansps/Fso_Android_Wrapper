cmake_minimum_required(VERSION 3.22)
project(touch_text_input_tests LANGUAGES CXX)

set(TOUCH_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/.." CACHE PATH "Directory containing touch_controls.cpp")
set(TOUCH_SDL_CPP_DIR "${TOUCH_CPP_DIR}" CACHE PATH "Directory containing third_party/SDL3")
set(TOUCH_SDL_LIBRARY "" CACHE FILEPATH "Host SDL3 library (import library on Windows)")
option(TOUCH_TEST_TC "Test TC developer scancode buttons" OFF)
if(NOT EXISTS "${TOUCH_SDL_LIBRARY}")
    message(FATAL_ERROR "Set TOUCH_SDL_LIBRARY to a desktop SDL3 library; see README.md")
endif()

find_package(Threads REQUIRED)
add_executable(touch_text_input_test touch_text_input_test.cpp)
target_compile_features(touch_text_input_test PRIVATE cxx_std_17)
target_include_directories(touch_text_input_test PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}" "${TOUCH_CPP_DIR}" "${TOUCH_SDL_CPP_DIR}"
    "${TOUCH_SDL_CPP_DIR}/third_party" "${TOUCH_SDL_CPP_DIR}/third_party/SDL3")
target_link_libraries(touch_text_input_test PRIVATE "${TOUCH_SDL_LIBRARY}" Threads::Threads)
if(TOUCH_TEST_TC)
    target_compile_definitions(touch_text_input_test PRIVATE TOUCH_TEST_TC)
endif()

enable_testing()
add_test(NAME touch_text_input COMMAND touch_text_input_test)
