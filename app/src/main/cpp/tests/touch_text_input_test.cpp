#include "touch_controls.cpp"

#include <cstdio>
#include <string>
#include <vector>

struct Events {
    std::string text;
    std::vector<SDL_Scancode> downs;
    std::vector<SDL_Scancode> ups;
    std::vector<Uint32> types;
    bool correct_window = true;
};

static int failures = 0;
static SDL_WindowID test_window = 0;

static void check(bool passed, const char* name) {
    std::printf("%s: %s\n", passed ? "PASS" : "FAIL", name);
    if (!passed) ++failures;
}

static Events drain() {
    Events result;
    SDL_Event event{};
    while (SDL_PollEvent(&event)) {
        if (event.type == SDL_EVENT_TEXT_INPUT) {
            result.text += event.text.text;
            result.correct_window &= event.text.windowID == test_window;
        } else if (event.type == SDL_EVENT_KEY_DOWN) {
            result.downs.push_back(event.key.scancode);
        } else if (event.type == SDL_EVENT_KEY_UP) {
            result.ups.push_back(event.key.scancode);
        } else continue;
        result.types.push_back(event.type);
    }
    return result;
}

static void tap(int code) {
    NB_onButton(code, true);
    NB_onButton(code, false);
}

int main() {
    SDL_SetHint(SDL_HINT_VIDEO_DRIVER, "dummy");
    if (!SDL_Init(SDL_INIT_VIDEO)) {
        std::printf("SDL_Init failed: %s\n", SDL_GetError());
        return 1;
    }
    SDL_Window* window = SDL_CreateWindow("touch input tests", 640, 480, SDL_WINDOW_HIDDEN);
    if (!window) {
        std::printf("SDL_CreateWindow failed: %s\n", SDL_GetError());
        return 1;
    }
    test_window = SDL_GetWindowID(window);
    sdl_window_id.store(test_window);
    drain();

    SDL_StopTextInput(window);
    tap(50); tap(51);
    auto events = drain();
    check(events.text.empty() && events.downs == std::vector<SDL_Scancode>{SDL_SCANCODE_C, SDL_SCANCODE_1}
          && events.ups == events.downs, "gameplay keeps key presses and releases without text");

    check(SDL_StartTextInput(window), "text input starts");
    drain();
    tap(50); tap(51); tap(52); tap(53);
    events = drain();
    check(events.text == "c123" && events.correct_window, "overlay C/1/2/3 type into the active window");
    check(events.downs.size() == 4 && events.ups.size() == 4, "text entry preserves key events");
    check(events.types.size() == 12 && events.types[0] == SDL_EVENT_KEY_DOWN
          && events.types[1] == SDL_EVENT_TEXT_INPUT && events.types[2] == SDL_EVENT_KEY_UP,
          "main-thread key and text event order");

    NB_onButton(50, false);
    check(drain().text.empty(), "key release never inserts text");
    tap(49); tap(40); tap(1); tap(7);
    check(drain().text.empty(), "Backspace/Tab/Escape/F1 do not insert characters");

    NB_onButton(21, true); tap(50); NB_onButton(21, false); tap(50);
    check(drain().text == "c", "overlay Ctrl suppresses shortcut text and releases cleanly");
    tap(3); tap(50);
    check(drain().text == "c", "Alt+M macro does not type an m");
    NB_onButton(11, true); tap(50); NB_onButton(11, false); tap(50);
    check(drain().text == "c", "held overlay Alt suppresses text");

    for (const auto modifier : {SDL_KMOD_CTRL, SDL_KMOD_ALT, SDL_KMOD_GUI}) {
        SDL_SetModState(static_cast<SDL_Keymod>(modifier)); tap(50); SDL_SetModState(SDL_KMOD_NONE);
        check(drain().text.empty(), "physical modifier shortcuts do not type");
    }

    push_key(true, SDL_SCANCODE_LSHIFT, SDLK_LSHIFT); tap(50); tap(51);
    push_key(false, SDL_SCANCODE_LSHIFT, SDLK_LSHIFT); tap(50);
    check(drain().text == "C!c", "synthetic Shift selects characters without sticking");
    SDL_SetModState(SDL_KMOD_CAPS); tap(50); SDL_SetModState(SDL_KMOD_NONE);
    check(drain().text == "C", "Caps Lock follows the SDL keymap");

    tap(20); tap(41); tap(42); tap(48);
    check(drain().text == " =-\\", "space and punctuation are printable");

    std::thread worker([] { tap(50); tap(51); });
    worker.join();
    events = drain();
    check(events.text == "c1" && events.downs.size() == 2 && events.ups.size() == 2,
          "off-thread touch and macro delivery is non-blocking");

    std::thread macro_worker([] { macro::exec(macro::make(4)); });
    macro_worker.join();
    check(drain().text == "c5", "native communication macro emits text on the main thread");

#ifdef TOUCH_TEST_TC
    tap(1000 + SDL_SCANCODE_B);
    check(drain().text == "b", "TC developer scancode buttons also emit text");
#endif

    SDL_StopTextInput(window);
    tap(50); tap(51);
    check(drain().text.empty(), "text stops when the field deactivates");

    SDL_StartTextInput(window);
    for (int index = 0; index < 200; ++index) { tap(50); tap(51); }
    std::string expected;
    for (int index = 0; index < 200; ++index) expected += "c1";
    check(drain().text == expected, "queued text retains valid storage without lost characters");

    SDL_DestroyWindow(window);
    touch_text_input::push(test_window, SDL_SCANCODE_C, SDL_KMOD_NONE);
    check(drain().text.empty(), "destroyed windows safely discard pending text");
    SDL_Quit();
    std::printf("Failures: %d\n", failures);
    return failures ? 1 : 0;
}
