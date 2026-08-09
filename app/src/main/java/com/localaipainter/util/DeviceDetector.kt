package com.localaipainter.util

import android.content.Context
import android.os.Build
import java.io.File

/**
 * DeviceDetector (util 层) —— 轻量版硬件检测
 *
 * 与 engine.DeviceDetector 的关系：
 *   - engine.DeviceDetector 是完整版（含 GPU/OpenGL/Vulkan 深度检测）
 *   - 本类是兼容层，供不需要完整检测的代码使用
 *
 * v3.2 更新：
 *   - 新增 Vulkan / OpenGL ES 检测
 *   - 新增混动模式支持判断
 *   - 委托给 engine.DeviceDetector 获取完整信息
 */
class DeviceDetector(private val context: Context) {

    companion object {
        private const val TAG = "DeviceDetector"
    }

    data class ChipInfo(
        val vendor: String,
        val model: String,
        val socName: String,
        val totalRamMB: Long,
        val supportedBackends: List<String>,
        val preferredBackend: String,
        val npuName: String = "",
        // v3.2 新增
        val hasVulkan: Boolean = false,
        val hasOpenGLES: Boolean = false,
        val supportsHybrid: Boolean = false,
    )

    fun detectChip(): ChipInfo {
        // 委托给完整版 engine.DeviceDetector
        val fullInfo = com.localaipainter.engine.DeviceDetector(context).detect()

        return ChipInfo(
            vendor = fullInfo.gpuVendor.ifBlank { "Unknown" },
            model = fullInfo.model,
            socName = fullInfo.chipset,
            totalRamMB = (fullInfo.totalRAMGB * 1024L),
            supportedBackends = fullInfo.supportedBackends,
            preferredBackend = fullInfo.preferredBackend,
            npuName = fullInfo.npuType,
            hasVulkan = fullInfo.hasVulkan,
            hasOpenGLES = fullInfo.hasOpenGLES,
            supportsHybrid = fullInfo.supportsHybrid,
        )
    }

    // ===== 保留旧 API 兼容 =====

    fun isVulkanAvailable(): Boolean {
        return com.localaipainter.engine.DeviceDetector(context).detect().hasVulkan
    }

    fun isOpenGLAvailable(): Boolean {
        return com.localaipainter.engine.DeviceDetector(context).detect().hasOpenGLES
    }

    fun supportsHybridMode(): Boolean {
        return com.localaipainter.engine.DeviceDetector(context).detect().supportsHybrid
    }
}
