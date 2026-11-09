#pragma once

// The host tests exercise SDL input only. Android builds use the NDK's real jni.h.
using jboolean = unsigned char;
using jint = int;
using jfloat = float;
struct JNIEnv;
using jclass = void*;
#define JNIEXPORT
#define JNICALL
