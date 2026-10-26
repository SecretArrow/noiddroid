// Auto-generated SPIR-V bytecode for the Poseidon2b compute shader.
//
// In v1 this is a placeholder shader that simply returns the input nonce
// counter unchanged — it proves the Vulkan dispatch pipeline is alive.
// Replaced at release build time by `scripts/compile_shaders.py` with the
// real compiled `poseidon2b.comp` from `vulkan/shaders/`.

#pragma once
#include <cstdint>

// Tiny SPIR-V that does: u32 counter[0] = 0; (a no-op main body)
// Assembled from the corresponding GLSL:
//   #version 450
//   layout(local_size_x=64) in;
//   layout(binding=0) buffer Buf { uint counter[]; };
//   void main() { if (gl_GlobalInvocationID.x == 0u) counter[0] = 0u; }
static const unsigned char kPoseidon2bSpirv[] = {
    0x03, 0x02, 0x23, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x0b, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00,
    // (truncated — full SPIR-V embedded by scripts/compile_shaders.py at release)
};
static constexpr unsigned kPoseidon2bSpirvLen = sizeof(kPoseidon2bSpirv);
