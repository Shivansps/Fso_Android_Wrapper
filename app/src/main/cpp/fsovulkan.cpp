#include <SDL_hints.h>
#include <adrenotools/driver.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <vulkan/vulkan.h>

#include <cstring>
#include <string>
#include <vector>

namespace {

constexpr char LogTag[] = "FSOVulkanDriver";
PFN_vkGetInstanceProcAddr getDriverProc = nullptr;

std::string getString(JNIEnv* env, jstring value)
{
    if (!value) {
        return {};
    }
    const auto* utf = env->GetStringUTFChars(value, nullptr);
    if (!utf) {
        return {};
    }
    std::string result(utf);
    env->ReleaseStringUTFChars(value, utf);
    return result;
}

bool supportsAndroidSurface(PFN_vkGetInstanceProcAddr getProc)
{
    const auto enumerate = reinterpret_cast<PFN_vkEnumerateInstanceExtensionProperties>(
        getProc(VK_NULL_HANDLE, "vkEnumerateInstanceExtensionProperties"));
    if (!enumerate || !getProc(VK_NULL_HANDLE, "vkCreateInstance")) {
        return false;
    }

    uint32_t count = 0;
    if (enumerate(nullptr, &count, nullptr) != VK_SUCCESS || count == 0) {
        return false;
    }
    std::vector<VkExtensionProperties> extensions(count);
    if (enumerate(nullptr, &count, extensions.data()) != VK_SUCCESS) {
        return false;
    }

    bool surface = false;
    bool androidSurface = false;
    for (uint32_t i = 0; i < count; ++i) {
        const auto* name = extensions[i].extensionName;
        surface |= std::strcmp(name, VK_KHR_SURFACE_EXTENSION_NAME) == 0;
        androidSurface |= std::strcmp(name, VK_KHR_ANDROID_SURFACE_EXTENSION_NAME) == 0;
    }
    return surface && androidSurface;
}

} // namespace

// SDL loads this library and resolves this symbol before it creates the Vulkan surface.
extern "C" VKAPI_ATTR __attribute__((visibility("default"))) PFN_vkVoidFunction VKAPI_CALL
vkGetInstanceProcAddr(VkInstance instance, const char* name)
{
    return getDriverProc ? getDriverProc(instance, name) : nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_shivansps_fsowrapper_VulkanDriver_configureLoader(
    JNIEnv* env, jclass, jstring nativeDir, jstring tempDir, jstring driverDir, jstring libraryName)
{
    const auto hooks = getString(env, nativeDir);
    const auto temp = getString(env, tempDir);
    const auto directory = getString(env, driverDir);
    const auto library = getString(env, libraryName);
    if (hooks.empty() || directory.empty() || library.empty()) {
        return JNI_FALSE;
    }

    const bool directPanvk = library == "libvulkan_panfrost.so";
    const auto driverPath = directory + "/";
    const auto tempPath = temp + "/";
    void* loader;
    if (directPanvk) {
        // Match PanProbe's native swapchain test: use the ICD's own WSI.
        loader = dlopen((driverPath + library).c_str(), RTLD_NOW | RTLD_LOCAL);
    } else {
        loader = adrenotools_open_libvulkan(RTLD_NOW | RTLD_LOCAL, ADRENOTOOLS_DRIVER_CUSTOM,
            temp.empty() ? nullptr : tempPath.c_str(), hooks.c_str(),
            driverPath.c_str(), library.c_str(), nullptr, nullptr);
    }

    if (!loader) {
        const char* error = dlerror();
        __android_log_print(ANDROID_LOG_WARN, LogTag, "%s could not open %s: %s",
            directPanvk ? "Direct PanVK ICD" : "AdrenoTools", library.c_str(),
            error ? error : "unknown loader error");
        return JNI_FALSE;
    }

    auto getProc = directPanvk ? reinterpret_cast<PFN_vkGetInstanceProcAddr>(
        dlsym(loader, "vk_icdGetInstanceProcAddr")) : nullptr;
    if (!getProc) {
        getProc = reinterpret_cast<PFN_vkGetInstanceProcAddr>(dlsym(loader, "vkGetInstanceProcAddr"));
    }
    if (!getProc || !supportsAndroidSurface(getProc)) {
        __android_log_print(ANDROID_LOG_WARN, LogTag, "Loader for %s lacks Android Vulkan surface support",
            library.c_str());
        dlclose(loader);
        return JNI_FALSE;
    }

    const auto shimPath = hooks + "/libfsovulkan.so";
    getDriverProc = getProc;
    if (!SDL_SetHint(SDL_HINT_VULKAN_LIBRARY, shimPath.c_str())) {
        __android_log_print(ANDROID_LOG_WARN, LogTag, "Could not set SDL_HINT_VULKAN_LIBRARY");
        getDriverProc = nullptr;
        dlclose(loader);
        return JNI_FALSE;
    }

    // The loader and its dispatch functions are needed until the :game process exits.
    __android_log_print(ANDROID_LOG_INFO, LogTag, "%s: %s",
        directPanvk ? "Using direct PanVK ICD" : "Using AdrenoTools loader", library.c_str());
    return JNI_TRUE;
}
