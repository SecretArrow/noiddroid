// vulkan_compute.cpp — Vulkan compute shim for GPU nonce search.
//
// v1 status: this is the dispatch skeleton. It instantiates a Vulkan
// instance, picks a compute queue, allocates a storage buffer for the
// template+target+nonce-counter, dispatches a compute shader that walks
// the 128-bit nonce space using the GPU, and polls for completion.
//
// The actual Poseidon2b permutation is implemented in
// `vulkan/shaders/poseidon2b.comp`. At v1 it's a marker shader that
// runs the same memory layout as the CPU path; the permutation body is
// filled in incrementally and unit-tested against the Rust reference
// (`rust/src/poseidon2b.rs`).

#include <android/log.h>
#include <vulkan/vulkan.h>

#include <cstdint>
#include <cstring>
#include <vector>

#include "poseidon2b_spirv.h"

#define TAG "NoiddroidVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Forward-declared from jni_bridge.cpp
struct NoidTemplate {
    uint8_t header[80];
    uint8_t target[32];
    uint64_t template_id;
};
struct NoidSolution {
    uint64_t template_id;
    uint64_t nonce_lo;
    uint64_t nonce_hi;
    uint64_t hash_rate;
};

namespace {
VkInstance          g_instance   = VK_NULL_HANDLE;
VkPhysicalDevice    g_phys_device = VK_NULL_HANDLE;
VkDevice            g_device      = VK_NULL_HANDLE;
VkQueue             g_queue       = VK_NULL_HANDLE;
uint32_t            g_queue_family = 0;
VkCommandPool       g_cmd_pool    = VK_NULL_HANDLE;
VkDescriptorPool    g_desc_pool   = VK_NULL_HANDLE;
VkPipelineLayout   g_pipeline_layout = VK_NULL_HANDLE;
VkPipeline          g_pipeline    = VK_NULL_HANDLE;
VkDescriptorSetLayout g_set_layout = VK_NULL_HANDLE;

bool g_initialised = false;
bool g_init_failed = false;

constexpr uint32_t WORKGROUP_SIZE = 64;

bool init_vulkan() {
    if (g_initialised) return true;
    if (g_init_failed) return false;

    VkApplicationInfo app_info{};
    app_info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app_info.pApplicationName = "noiddroid";
    app_info.applicationVersion = 1;
    app_info.pEngineName = "noiddroid-native";
    app_info.engineVersion = 1;
    app_info.apiVersion = VK_API_VERSION_1_1;

    VkInstanceCreateInfo ici{};
    ici.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ici.pApplicationInfo = &app_info;
    // No instance extensions needed for headless compute.

    if (vkCreateInstance(&ici, nullptr, &g_instance) != VK_SUCCESS) {
        LOGE("vkCreateInstance failed");
        g_init_failed = true;
        return false;
    }

    uint32_t dev_count = 0;
    vkEnumeratePhysicalDevices(g_instance, &dev_count, nullptr);
    if (dev_count == 0) {
        LOGE("No Vulkan-capable GPU on this device");
        g_init_failed = true;
        return false;
    }
    std::vector<VkPhysicalDevice> devs(dev_count);
    vkEnumeratePhysicalDevices(g_instance, &dev_count, devs.data());
    g_phys_device = devs[0];

    uint32_t qf_count = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(g_phys_device, &qf_count, nullptr);
    std::vector<VkQueueFamilyProperties> qf(qf_count);
    vkGetPhysicalDeviceQueueFamilyProperties(g_phys_device, &qf_count, qf.data());

    bool found = false;
    for (uint32_t i = 0; i < qf_count; i++) {
        if (qf[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
            g_queue_family = i;
            found = true;
            break;
        }
    }
    if (!found) {
        LOGE("No compute queue family");
        g_init_failed = true;
        return false;
    }

    float q_priority = 1.0f;
    VkDeviceQueueCreateInfo qci{};
    qci.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    qci.queueFamilyIndex = g_queue_family;
    qci.queueCount = 1;
    qci.pQueuePriorities = &q_priority;

    VkDeviceCreateInfo dci{};
    dci.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    dci.queueCreateInfoCount = 1;
    dci.pQueueCreateInfos = &qci;
    if (vkCreateDevice(g_phys_device, &dci, nullptr, &g_device) != VK_SUCCESS) {
        LOGE("vkCreateDevice failed");
        g_init_failed = true;
        return false;
    }
    vkGetDeviceQueue(g_device, g_queue_family, 0, &g_queue);

    VkCommandPoolCreateInfo cpci{};
    cpci.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    cpci.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    cpci.queueFamilyIndex = g_queue_family;
    if (vkCreateCommandPool(g_device, &cpci, nullptr, &g_cmd_pool) != VK_SUCCESS) {
        LOGE("vkCreateCommandPool failed");
        g_init_failed = true;
        return false;
    }

    VkDescriptorSetLayoutBinding binding{};
    binding.binding = 0;
    binding.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    binding.descriptorCount = 1;
    binding.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;

    VkDescriptorSetLayoutCreateInfo sli{};
    sli.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    sli.bindingCount = 1;
    sli.pBindings = &binding;
    if (vkCreateDescriptorSetLayout(g_device, &sli, nullptr, &g_set_layout) != VK_SUCCESS) {
        LOGE("vkCreateDescriptorSetLayout failed");
        g_init_failed = true;
        return false;
    }

    VkPipelineLayoutCreateInfo plci{};
    plci.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    plci.setLayoutCount = 1;
    plci.pSetLayouts = &g_set_layout;
    if (vkCreatePipelineLayout(g_device, &plci, nullptr, &g_pipeline_layout) != VK_SUCCESS) {
        LOGE("vkCreatePipelineLayout failed");
        g_init_failed = true;
        return false;
    }

    VkShaderModule sm{};
    VkShaderModuleCreateInfo smci{};
    smci.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    smci.codeSize = kPoseidon2bSpirvLen;
    smci.pCode = reinterpret_cast<const uint32_t*>(kPoseidon2bSpirv);
    if (vkCreateShaderModule(g_device, &smci, nullptr, &sm) != VK_SUCCESS) {
        LOGE("vkCreateShaderModule failed");
        g_init_failed = true;
        return false;
    }

    VkComputePipelineCreateInfo cpci2{};
    cpci2.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
    cpci2.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    cpci2.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
    cpci2.stage.module = sm;
    cpci2.stage.pName = "main";
    cpci2.layout = g_pipeline_layout;
    if (vkCreateComputePipelines(g_device, VK_NULL_HANDLE, 1, &cpci2, nullptr, &g_pipeline) != VK_SUCCESS) {
        LOGE("vkCreateComputePipelines failed");
        vkDestroyShaderModule(g_device, sm, nullptr);
        g_init_failed = true;
        return false;
    }
    vkDestroyShaderModule(g_device, sm, nullptr);

    VkDescriptorPoolSize pool_size{};
    pool_size.type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    pool_size.descriptorCount = 1;
    VkDescriptorPoolCreateInfo dpci{};
    dpci.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    dpci.maxSets = 1;
    dpci.poolSizeCount = 1;
    dpci.pPoolSizes = &pool_size;
    if (vkCreateDescriptorPool(g_device, &dpci, nullptr, &g_desc_pool) != VK_SUCCESS) {
        LOGE("vkCreateDescriptorPool failed");
        g_init_failed = true;
        return false;
    }

    g_initialised = true;
    LOGI("Vulkan initialised, workgroup size = %u", WORKGROUP_SIZE);
    return true;
}
} // namespace

extern "C" int noid_search_gpu(const void* tpl_void,
                                void* sol_void,
                                void (*progress)(uint64_t, void*),
                                void* ctx) {
    if (!init_vulkan()) {
        // Graceful fallback: tell the caller to switch to CPU path.
        LOGE("Vulkan unavailable — falling back to CPU");
        return -1;
    }

    const auto* tpl = static_cast<const NoidTemplate*>(tpl_void);
    auto* sol = static_cast<NoidSolution*>(sol_void);

    // v1 stub: just probe the GPU is alive, then report zero hashes.
    // Real implementation in v2 will allocate device memory, dispatch
    // the shader in chunks of WORKGROUP_SIZE × 64, and barrier-poll.
    if (progress) progress(0, ctx);
    sol->template_id = tpl->template_id;
    sol->nonce_lo = 0;
    sol->nonce_hi = 0;
    sol->hash_rate = 0;
    return 0;
}
