package com.localaipainter.engine

import android.content.Context
import com.localaipainter.util.Logger

/**
 * MNN 推理引擎 —— 通过 JNI 调用 libMNN.so
 */
class MnnEngine(private val context: Context) : InferenceEngine {
    override val backendName: String = "MNN"

    private var modelLoaded = false
    private var inputSize: Long = 0

    init {
        try {
            System.loadLibrary("MNN")
        } catch (e: UnsatisfiedLinkError) {
            Logger.w("MNN native lib not found, running in stub mode")
        }
    }

    override fun loadModel(modelPath: String): Boolean {
        Logger.i("MNN: loading model from $modelPath")
        // TODO: 调用 MNNInterpreter::createFromFile
        modelLoaded = FileExists(modelPath)
        return modelLoaded
    }

    override fun unloadModel() {
        modelLoaded = false
        Logger.i("MNN: model unloaded")
    }

    override fun runInference(input: FloatArray, width: Int, height: Int): FloatArray {
        if (!modelLoaded) {
            Logger.e("MNN: model not loaded, returning empty")
            return FloatArray(width * height * 3)
        }
        Logger.d("MNN: inference input=${input.size} ${width}x${height}")
        // TODO: 真实推理
        return FloatArray(width * height * 3) { (it % 256) / 255f }
    }

    override fun isAvailable(): Boolean = modelLoaded

    override fun getMemoryUsageMB(): Int {
        // TODO: 从 MNN 内部查询
        return (inputSize / (1024 * 1024)).toInt().coerceAtLeast(0)
    }

    override fun release() {
        unloadModel()
    }

    private fun FileExists(path: String): Boolean {
        val f = java.io.File(path)
        return f.exists() && f.length() > 0
    }
}
