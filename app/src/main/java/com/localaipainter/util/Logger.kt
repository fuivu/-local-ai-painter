package com.localaipainter.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志工具 —— 同时输出到 Logcat 和文件，方便排查
 */
object Logger {
    private const val TAG = "LocalAIPainter"
    private var logFile: File? = null
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()
        logFile = File(dir, "app_${System.currentTimeMillis()}.log")
    }

    fun d(msg: String) {
        android.util.Log.d(TAG, msg)
        writeToFile("DEBUG", msg)
    }

    fun i(msg: String) {
        android.util.Log.i(TAG, msg)
        writeToFile("INFO", msg)
    }

    fun w(msg: String) {
        android.util.Log.w(TAG, msg)
        writeToFile("WARN", msg)
    }

    fun e(msg: String, t: Throwable? = null) {
        android.util.Log.e(TAG, msg, t)
        writeToFile("ERROR", "$msg ${t?.stackTraceToString() ?: ""}")
    }

    private fun writeToFile(level: String, msg: String) {
        try {
            logFile?.appendText("[${sdf.format(Date())}] [$level] $msg\n")
        } catch (_: Exception) {
            // 文件写入失败不影响主流程
        }
    }

    fun exportLogs(): File? = logFile
}
