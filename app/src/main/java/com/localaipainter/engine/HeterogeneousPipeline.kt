package com.localaipainter.engine

import android.content.Context
import com.localaipainter.memory.LruTensorCache
import com.localaipainter.scheduler.Scheduler
import com.localaipainter.scheduler.SchedulerFactory
import com.localaipainter.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 异构推理管线 —— 协调引擎 + 调度器 + 张量缓存
 *
 * generate() 是 suspend 函数，在 Dispatchers.Default 上执行推理，
 * 不阻塞 UI 线程。onProgress 回调切回调用协程的上下文。
 */
class HeterogeneousPipeline(
    private val context: Context,
    private val tensorCache: LruTensorCache
) {
    private var engine: InferenceEngine? = null
    private var scheduler: Scheduler = SchedulerFactory.getByName("Euler A")
    private var initialized = false

    fun init() {
        // 使用 engine 包内的 EngineFactory（同包，直接引用）
        val backend = com.localaipainter.engine.EngineFactory.getBestBackend()
        engine = com.localaipainter.engine.EngineFactory.tryCreate(context, backend)
        if (engine == null) {
            Logger.e("Pipeline", "Failed to create any inference engine")
            return
        }
        initialized = true
        Logger.i("Pipeline", "Initialized with backend=$backend, scheduler=${scheduler.name}")
    }

    fun setBackend(backend: String) {
        engine?.release()
        engine = com.localaipainter.engine.EngineFactory.tryCreate(context, backend)
        Logger.i("Pipeline", "Backend switched to $backend")
    }

    fun setScheduler(name: String) {
        scheduler = SchedulerFactory.getByName(name)
        Logger.i("Pipeline", "Scheduler set to ${scheduler.name}")
    }

    fun loadModel(modelPath: String): Boolean {
        if (!initialized) init()
        val ok = engine?.loadModel(modelPath) ?: false
        Logger.i("Pipeline", "Model load ${if (ok) "OK" else "FAILED"}: $modelPath")
        return ok
    }

    /**
     * 异步生成 —— 在 Default 调度器上执行推理循环
     */
    suspend fun generate(
        prompt: String,
        negativePrompt: String = "",
        width: Int = 512,
        height: Int = 512,
        steps: Int = 20,
        cfgScale: Float = 7.5f,
        seed: Long = -1L,
        onProgress: (step: Int, total: Int) -> Unit = { _, _ -> }
    ): FloatArray? = withContext(Dispatchers.Default) {
        if (!initialized || engine == null) {
            Logger.e("Pipeline", "Pipeline not initialized")
            return@withContext null
        }

        Logger.i("Pipeline", "Generate: prompt='$prompt' ${width}x${height} steps=$steps cfg=$cfgScale")
        val timesteps = scheduler.getTimesteps(steps)
        var latents = FloatArray(width * height * 3) { kotlin.random.Random.nextFloat() * 0.1f }

        for (i in 0 until steps) {
            val eng = engine ?: run {
                Logger.e("Pipeline", "Engine became null at step $i")
                return@withContext null
            }
            val noisePred = eng.runInference(latents, width, height)
            latents = scheduler.step(latents, noisePred, timesteps[i], i, steps)
            onProgress(i + 1, steps)
        }

        tensorCache.put("last_output", latents)
        Logger.i("Pipeline", "Generation complete, latents cached (${latents.size} floats)")
        latents
    }

    fun getEngineInfo(): String {
        val eng = engine ?: return "No engine"
        return "${eng.backendName} | mem=${eng.getMemoryUsageMB()}MB | available=${eng.isAvailable()}"
    }

    fun release() {
        engine?.release()
        engine = null
        initialized = false
        Logger.i("Pipeline", "Pipeline released")
    }
}
