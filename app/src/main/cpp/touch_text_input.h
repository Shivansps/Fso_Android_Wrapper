#pragma once

#include "third_party/SDL3/SDL.h"

#include <array>
#include <atomic>
#include <map>
#include <memory>
#include <new>

namespace touch_text_input {

inline std::atomic<SDL_Keymod> held_modifiers{SDL_KMOD_NONE};

inline SDL_Keymod modifiers_for_key(SDL_Scancode scancode, bool down) {
    SDL_Keymod modifier = SDL_KMOD_NONE;
    switch (scancode) {
        case SDL_SCANCODE_LSHIFT: modifier = SDL_KMOD_LSHIFT; break;
        case SDL_SCANCODE_RSHIFT: modifier = SDL_KMOD_RSHIFT; break;
        case SDL_SCANCODE_LCTRL: modifier = SDL_KMOD_LCTRL; break;
        case SDL_SCANCODE_RCTRL: modifier = SDL_KMOD_RCTRL; break;
        case SDL_SCANCODE_LALT: modifier = SDL_KMOD_LALT; break;
        case SDL_SCANCODE_RALT: modifier = SDL_KMOD_RALT; break;
        case SDL_SCANCODE_LGUI: modifier = SDL_KMOD_LGUI; break;
        case SDL_SCANCODE_RGUI: modifier = SDL_KMOD_RGUI; break;
        default: break;
    }
    // SDL_PushEvent does not update SDL's keyboard state for overlay modifiers.
    if (down) held_modifiers.fetch_or(modifier, std::memory_order_relaxed);
    else held_modifiers.fetch_and(static_cast<SDL_Keymod>(~modifier), std::memory_order_relaxed);
    return static_cast<SDL_Keymod>(SDL_GetModState() | held_modifiers.load(std::memory_order_relaxed));
}

struct PendingKey {
    SDL_WindowID window_id;
    SDL_Scancode scancode;
    SDL_Keymod modifiers;
};

inline void SDLCALL dispatch_text(void* userdata) {
    const std::unique_ptr<PendingKey> key(static_cast<PendingKey*>(userdata));
    SDL_Window* window = SDL_GetWindowFromID(key->window_id);
    if (!window || !SDL_TextInputActive(window)) return;

    const SDL_Keycode character = SDL_GetKeyFromScancode(key->scancode, key->modifiers, false);
    // SDL's non-printable keycodes are outside the Unicode range.
    if (character < 0x20 || character == 0x7f || character > 0x10ffff) return;

    // SDL_PushEvent copies the text pointer. Keep each encoded character stable
    // for the lifetime of the library, including while other events are queued.
    // This cache is only accessed on SDL's main thread; map entries never move.
    static std::map<SDL_Keycode, std::array<char, 5>> text_cache;
    auto entry = text_cache.try_emplace(character);
    if (entry.second) *SDL_UCS4ToUTF8(character, entry.first->second.data()) = '\0';

    SDL_Event text{};
    text.type = SDL_EVENT_TEXT_INPUT;
    text.text.windowID = key->window_id;
    text.text.text = entry.first->second.data();
    SDL_PushEvent(&text);
}

inline void push(SDL_WindowID window_id, SDL_Scancode scancode, SDL_Keymod modifiers) {
    if (window_id == 0 || (modifiers & (SDL_KMOD_CTRL | SDL_KMOD_ALT | SDL_KMOD_GUI))) return;
    auto* key = new (std::nothrow) PendingKey{window_id, scancode, modifiers};
    // Touch buttons and macros can run off-thread; SDL's text-input/window and
    // keymap APIs must run on its main thread. Keep the caller non-blocking.
    if (key && !SDL_RunOnMainThread(dispatch_text, key, false)) delete key;
}

} // namespace touch_text_input
