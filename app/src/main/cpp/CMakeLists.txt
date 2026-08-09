cmake_minimum_required(VERSION 3.22.1)
project(LocalAIPainter CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 编译选项：ARM NEON + LTO + O3
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -flto -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+simd -mfpu=neon-fp-armv8")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math -funroll-loops")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -flto")

find_library(log-lib log)
find_library(android-lib android)

# ===== 查找 Vulkan SDK =====
find_library(vulkan-lib vulkan)
find_path(vulkan-include vulkan/vulkan.h)

# ===== 通用源文件列表 =====
set(COMMON_SOURCES
    engine_factory.cpp
    jni_bridge.cpp
    image_utils.cpp
    math_utils.cpp
    memory_pool.cpp
    tensor_utils.cpp
    lora_loader.cpp
    text_encoder.cpp
    unet.cpp
    vae_decoder.cpp
    sd_pipeline.cpp
    scheduler_ddim.cpp
    scheduler_euler_a.cpp
    scheduler_lcm.cpp
    quantize_groupwise.cpp
    quantize_minmax.cpp
)

# ===== 调度器模块 =====
add_library(sd_scheduler SHARED
    engine/scheduler/SchedulerBase.cpp
    engine/scheduler/EulerAScheduler.cpp
    engine/scheduler/SchedulerFactory.cpp
)
target_link_libraries(sd_scheduler ${log-lib})

# ===== VAE 模块 =====
add_library(sd_vae SHARED
    engine/vae/VAEBase.cpp
)
target_link_libraries(sd_vae ${log-lib})

# ===== 文本编码器模块 =====
add_library(sd_text_encoder SHARED
    engine/text_encoder/TextEncoderBase.cpp
    engine/text_encoder/T5Encoder.cpp
)
target_link_libraries(sd_text_encoder ${log-lib})

# ===== 模型加载器模块 =====
add_library(sd_model_loader SHARED
    engine/model/ModelLoader.cpp
)
target_link_libraries(sd_model_loader ${log-lib})

# ===== 后处理器模块 =====
add_library(sd_post_processor SHARED
    engine/postprocess/PostProcessorBase.cpp
)
target_link_libraries(sd_post_processor ${log-lib})

# ===== LoRA 管理器模块 =====
add_library(sd_lora SHARED
    engine/lora/LoRAManager.cpp
)
target_link_libraries(sd_lora ${log-lib})

# ===== ControlNet 模块 =====
add_library(sd_controlnet SHARED
    engine/controlnet/ControlNetProcessor.cpp
)
target_link_libraries(sd_controlnet ${log-lib})

# ===== 人脸修复模块 =====
add_library(sd_facerestore SHARED
    engine/facerestore/FaceRestorer.cpp
)
target_link_libraries(sd_facerestore ${log-lib})

# ===== 超分模块 =====
add_library(sd_upscale SHARED
    engine/upscale/Upscaler.cpp
)
target_link_libraries(sd_upscale ${log-lib})

# ===== 天玑 8400 适配层（Dimensity8400Adapter）=====
#  dlopen 方式加载 NeuroPilot .so，缺库时自动 STUB 降级
add_library(dimensity8400_adapter SHARED
    engine/mediatek/Dimensity8400Adapter.cpp
    engine/mediatek/JniDimensity8400.cpp
)
target_link_libraries(dimensity8400_adapter
    ${log-lib}
    ${android-lib}
    dl  # dlopen / dlsym
)
set_target_properties(dimensity8400_adapter PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ Dimensity8400 adapter registered (STUB mode if .so missing)")

# ===== Vulkan Compute 模块（核心创新） =====
if(vulkan-lib AND vulkan-include)
    message(STATUS "✅ Vulkan found: ${vulkan-lib}")
    add_library(vulkan_compute SHARED
        engine/vulkan/VulkanCompute.cpp
        engine/vulkan/JniVulkanBridge.cpp
    )
    target_include_directories(vulkan_compute PRIVATE ${vulkan-include})
    target_link_libraries(vulkan_compute
        ${vulkan-lib}
        ${log-lib}
        ${android-lib}
    )
    set_target_properties(vulkan_compute PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
    )
else()
    message(WARNING "⚠️ Vulkan not found, building stub only")
    # 创建空 stub 库，让 JNI 层能加载但不崩溃
    add_library(vulkan_compute SHARED
        engine/vulkan/JniVulkanBridge.cpp
    )
    target_link_libraries(vulkan_compute ${log-lib})
endif()

# ===== OpenGL ES 辅助库（Kotlin 侧用 EGL/GLES 直接调用，这里提供辅助） =====
add_library(opengl_helper SHARED
    engine/opengl/GLHelper.cpp
)
target_link_libraries(opengl_helper
    ${log-lib}
    ${android-lib}
    GLESv3
    EGL
)
set_target_properties(opengl_helper PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)

# ===== 引擎工厂（聚合层） =====
add_library(engine_factory SHARED
    ${COMMON_SOURCES}
)
target_link_libraries(engine_factory
    sd_scheduler
    sd_vae
    sd_text_encoder
    sd_model_loader
    sd_post_processor
    sd_lora
    sd_controlnet
    sd_facerestore
    sd_upscale
    vulkan_compute
    dimensity8400_adapter
    ${log-lib}
    ${android-lib}
)

# ===== 安装规则 =====
set_target_properties(engine_factory PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)

# 仅保留 arm64-v8a（砍掉 armeabi-v7a 节省 ~15MB）
# 在 app/build.gradle.kts 中设置 ndk.abiFilters = ["arm64-v8a"]

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — 量化引擎 (INT2 / FP8 / WeightStreamer)
# ═════════════════════════════════════════════════════════
add_library(v35_quant SHARED
    v35/quantization/int2_fp8_quantizer.cpp
)
target_link_libraries(v35_quant ${log-lib})
set_target_properties(v35_quant PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ v3.5 Quantization (INT2/FP8) registered")

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — 图融合优化器 (Conv+BN+SiLU / QKV / FlashAttn / LUT)
# ═════════════════════════════════════════════════════════
add_library(v35_graph_fusion SHARED
    v35/optimization/graph_fusion.cpp
)
target_link_libraries(v35_graph_fusion ${log-lib})
set_target_properties(v35_graph_fusion PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ v3.5 Graph Fusion (Conv+BN+SiLU / QKV / FlashAttn / LUT) registered")

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — 双缓冲 + DMA 重叠管线 + CIM 模拟
# ═════════════════════════════════════════════════════════
add_library(v35_pipeline SHARED
    v35/optimization/double_buffered_pipeline.cpp
)
target_link_libraries(v35_pipeline ${log-lib})
set_target_properties(v35_pipeline PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ v3.5 Double-Buffered Pipeline + CIM registered")

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — 天玑 NPU 深度适配 (NPU 990 / 890+ / 890)
# ═════════════════════════════════════════════════════════
add_library(v35_mediatek SHARED
    v35/engine_mediatek/mediatek_npu_adapter.cpp
)
target_link_libraries(v35_mediatek
    ${log-lib}
    ${android-lib}
    dl
)
set_target_properties(v35_mediatek PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ v3.5 MediaTek NPU Adapter (NPU 990/890+/890) registered")

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — Vulkan 零拷贝渲染管线
# ═════════════════════════════════════════════════════════
if(vulkan-lib AND vulkan-include)
    add_library(v35_vulkan_render SHARED
        v35/rendering/vulkan_zero_copy.cpp
    )
    target_include_directories(v35_vulkan_render PRIVATE ${vulkan-include})
    target_link_libraries(v35_vulkan_render
        ${vulkan-lib}
        ${log-lib}
        ${android-lib}
    )
    set_target_properties(v35_vulkan_render PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
    )
    message(STATUS "✅ v3.5 Vulkan Zero-Copy Pipeline registered")
else()
    message(WARNING "⚠️ Vulkan not found, v35 Vulkan render stub only")
    add_library(v35_vulkan_render SHARED
        v35/rendering/vulkan_zero_copy.cpp
    )
    target_link_libraries(v35_vulkan_render ${log-lib})
endif()

# ═════════════════════════════════════════════════════════
#  v3.5 Aurora — 引擎总入口 (聚合所有 v3.5 模块)
# ═════════════════════════════════════════════════════════
add_library(v35_engine SHARED
    v35/engine_v35.cpp
    v35/locai_v35.cpp
)
target_link_libraries(v35_engine
    v35_quant
    v35_graph_fusion
    v35_pipeline
    v35_mediatek
    v35_vulkan_render
    engine_factory
    ${log-lib}
    ${android-lib}
)
set_target_properties(v35_engine PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/../jniLibs/${ANDROID_ABI}"
)
message(STATUS "✅ v3.5 Aurora Engine (master) registered — all modules linked")
