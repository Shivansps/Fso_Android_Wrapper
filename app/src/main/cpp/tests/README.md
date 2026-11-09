# Native touch text input regression tests

These tests compile the actual `touch_controls.cpp` and use a real desktop SDL3
event queue with its dummy video driver. No game files, Android emulator or JNI
runtime are needed. The local `jni.h` only supplies the JNI entry-point types;
normal Android builds continue to use the NDK header.

Configure this directory with CMake, supplying `TOUCH_SDL_LIBRARY` as the full
path to a desktop SDL3 shared library (the `.lib` import library on Windows),
then build and run CTest. On Windows, put the matching SDL3 DLL on `PATH`.

```sh
cmake -S path/to/cpp/tests -B build/touch-input-tests -DTOUCH_SDL_LIBRARY=/path/to/libSDL3.so
cmake --build build/touch-input-tests
ctest --test-dir build/touch-input-tests --output-on-failure
```

The tests cover gameplay key events, C/1/2/3 text entry, release handling,
non-printable keys, physical and overlay modifier shortcuts, Shift/Caps Lock,
punctuation, worker-thread and native macro delivery, text-input deactivation,
queued string lifetime, and destroyed windows. TC builds also test generic
developer scancode buttons.
