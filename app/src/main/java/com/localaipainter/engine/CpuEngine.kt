package com.localaipainter.engine

import android.content.Context
import com.localaipainter.util.Logger

/**
 * CPU 纯软件推理引擎 —— 无 GPU/NPU 时的兜底方案
 */
class CpuEngine(private val context: Context) : InferenceEngine {
    override val backendName: String = "CPU"

    private var modelLoaded = false

    override fun loadModel(modelPath: String): Boolean {
        Logger.i("CPU: loading $modelPath (fallback mode)")
        // TODO: 纯 CPU 推理初始化
        modelLoaded = java.io.File(modelPath).exists()
        return modelLoaded
    }

    override fun unloadModel() {
        modelLoaded = false
    }

    override fun runInference(input: FloatArray, width: Int, height: Int): FloatArray {
        if (!modelLoaded) return FloatArray(width * height * 3)
        Logger.d("CPU: running on all cores, ${width}x${height}")
        // TODO: 多线程 CPU 推理
        val out = FloatArray(width * height * 3)
        for (i in out.indices) {
            out[i] = ((input[i % input.size] * 255).toInt() % 256) / 255f
        }
        return out
    }

    override fun isAvailable(): Boolean = true // CPU 永远可用

    override fun getMemoryUsageMB(): Int = 0

    override fun release() = unloadModel()
}
