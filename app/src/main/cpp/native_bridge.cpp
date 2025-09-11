//#define ENABLE_LOG
#include <jni.h>
#include <SDL.h>
#include <atomic>
#ifdef ENABLE_LOG
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NB", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "NB", __VA_ARGS__)
#endif

static std::atomic<Uint32> sdl_window_id{0};

static Uint32 detect_window_id_once() {
    SDL_Window* w = SDL_GetKeyboardFocus();
    if (!w) w = SDL_GetMouseFocus();
    if (!w) w = SDL_GetGrabbedWindow();
    return w ? SDL_GetWindowID(w) : 0;
}

static Uint32 ensure_window_id() {
    Uint32 wid = sdl_window_id.load(std::memory_order_relaxed);
    if (wid && SDL_GetWindowFromID(wid)) return wid;
    wid = detect_window_id_once();
    sdl_window_id.store(wid, std::memory_order_relaxed);
    return wid;
}

static void push_key(jboolean down, SDL_Scancode sc, SDL_Keycode kc) {
    SDL_Event e{};
    e.type = down ? SDL_KEYDOWN : SDL_KEYUP;
    e.key.state = down ? SDL_PRESSED : SDL_RELEASED;
    e.key.repeat = 0;
    e.key.keysym.scancode = sc;
    e.key.keysym.sym = kc;
    e.key.keysym.mod = SDL_GetModState();
    e.key.windowID = ensure_window_id();

    int ret = SDL_PushEvent(&e);
    #ifdef ENABLE_LOG
    if (ret != 1) {
        LOGW("SDL_PushEvent %s %s ret=%d windowID=%u err=%s",
             down ? "KEYDOWN" : "KEYUP",
             SDL_GetKeyName(kc),
             ret, e.key.windowID, SDL_GetError());
    } else {
        LOGI("SDL_PushEvent %s %s ret=%d windowID=%u",
             down ? "KEYDOWN" : "KEYUP",
             SDL_GetKeyName(kc), ret, e.key.windowID);
    }
    #endif
}

static void push_alt_combo(jboolean down, SDL_Scancode sc, SDL_Keycode kc) {
    if (down) {
        push_key(true,  SDL_SCANCODE_LALT, SDLK_LALT);
        push_key(true,  sc, kc);
    } else {
        push_key(false, sc, kc);
        push_key(false, SDL_SCANCODE_LALT, SDLK_LALT);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_shivansps_fsowrapper_overlay_NativeBridge_onButton(JNIEnv*, jclass, jint code, jboolean down) {
    auto send = [&](SDL_Scancode sc, SDL_Keycode kc){ push_key(down, sc, kc); };
    #ifdef ENABLE_LOG
    LOGI("native onButton code=%d down=%d", (int)code, (int)down);
    #endif
    switch (code) {
        //COMMON AREA
        case 1: // ESC
            push_key(down, SDL_SCANCODE_ESCAPE, SDLK_ESCAPE);
            break;
        case 2: // F3
            push_key(down, SDL_SCANCODE_F3, SDLK_F3);
            break;
        case 3: // ALT+M
            push_alt_combo(down, SDL_SCANCODE_M, SDLK_m);
            break;
        case 4: // ALT+H
            push_alt_combo(down, SDL_SCANCODE_H, SDLK_h);
            break;
        case 5: // ALT+J
            push_alt_combo(down, SDL_SCANCODE_J, SDLK_j);
            break;
        //DPAD AREA
        case 10: send(SDL_SCANCODE_KP_2,     SDLK_KP_2); break; // inverted up/down
        case 11: send(SDL_SCANCODE_KP_4,     SDLK_KP_4); break;
        case 12: send(SDL_SCANCODE_KP_8,     SDLK_KP_8); break;
        case 13: send(SDL_SCANCODE_KP_6,     SDLK_KP_6); break;
        //Weapons Area
        case 20: // Space
            push_key(down, SDL_SCANCODE_SPACE, SDLK_SPACE);
            break;
        case 21: // Ctrl
            push_key(down, SDL_SCANCODE_LCTRL, SDLK_LCTRL);
            break;
        case 22: // Cycle P
            push_key(down, SDL_SCANCODE_PERIOD, SDLK_PERIOD);
            break;
        case 23: // Cycle S
            push_key(down, SDL_SCANCODE_SLASH, SDLK_SLASH);
            break;
        case 24: // Tab
            push_key(down, SDL_SCANCODE_TAB, SDLK_TAB);
            break;
        case 25: // +
            push_key(down, SDL_SCANCODE_EQUALS, SDLK_EQUALS);
            break;
        case 26: // -
            push_key(down, SDL_SCANCODE_MINUS, SDLK_MINUS);
            break;
        case 27: // Q
            push_key(down, SDL_SCANCODE_Q, SDLK_q);
            break;
        case 28: // X
            push_key(down, SDL_SCANCODE_X, SDLK_x);
            break;
        //Targeting Area
        case 30: // Y
            push_key(down, SDL_SCANCODE_Y, SDLK_y);
            break;
        case 31: // H
            push_key(down, SDL_SCANCODE_H, SDLK_h);
            break;
        case 32: // B
            push_key(down, SDL_SCANCODE_B, SDLK_b);
            break;
        case 33: // E
            push_key(down, SDL_SCANCODE_E, SDLK_e);
            break;
        case 34: // F
            push_key(down, SDL_SCANCODE_F, SDLK_f);
            break;
        case 35: // T
            push_key(down, SDL_SCANCODE_T, SDLK_t);
            break;
        default:
            break;
    }
}