package com.localaipainter.engine

import android.content.Context
import com.localaipainter.util.Logger

/**
 * NCNN 推理引擎 —— 通过 JNI 调用 ncnn 库
 */
class NcnnEngine(private val context: Context) : InferenceEngine {
    override val backendName: String = "NCNN"

    private var modelLoaded = false

    init {
        try {
            System.loadLibrary("ncnn")
        } catch (e: UnsatisfiedLinkError) {
            Logger.w("NCNN native lib not found, running in stub mode")
        }
    }

    override fun loadModel(modelPath: String): Boolean {
        Logger.i("NCNN: loading model from $modelPath")
        // TODO: 调用 ncnn::Net::loadModel / loadParam
        modelLoaded = java.io.File(modelPath).exists()
        return modelLoaded
    }

    override fun unloadModel() {
        modelLoaded = false
        Logger.i("NCNN: model unloaded")
    }

    override fun runInference(input: FloatArray, width: Int, height: Int): FloatArray {
        if (!modelLoaded) {
            Logger.e("NCNN: model not loaded")
            return FloatArray(width * height * 3)
        }
        Logger.d("NCNN: inference ${width}x${height}")
        // TODO: 真实推理
        return FloatArray(width * height * 3) { ((it * 7) % 256) / 255f }
    }

    override fun isAvailable(): Boolean = modelLoaded

    override fun getMemoryUsageMB(): Int = 0 // TODO

    override fun release() = unloadModel()
}
