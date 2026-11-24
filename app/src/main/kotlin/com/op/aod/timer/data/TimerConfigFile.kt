package com.op.aod.timer.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 配置的文件存储。
 *
 * 不使用 SharedPreferences：模块 App 的 `getSharedPreferences` 会被 LSPosed
 * 重定向（New XSharedPreferences，计划在 2.3.0 移除），而普通文件读写不受影响。
 *
 * 存放在**设备加密（DE）**目录，保证解锁前 AOD 也能读到配置：
 * `/data/user_de/0/<package>/files/timer_config.json`
 *
 * 写入采用「临时文件 + 重命名」，避免进程中断留下半截 JSON。
 */
internal object TimerConfigFile {

    private const val TAG = "AOD_Timer"
    private const val FILE_NAME = "timer_config.json"
    private const val TEMP_NAME = "timer_config.json.tmp"

    fun read(context: Context): TimerConfig? = runCatching {
        val file = file(context)
        if (!file.exists()) return null

        val json = JSONObject(file.readText())
        TimerConfig(
            keepAodMinutes = json.optInt(
                "keepAodMinutes",
                TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
            ),
            forceDeburnIn = json.optBoolean(
                "forceDeburnIn",
                TimerConfigContract.DEFAULT_FORCE_DEBURN_IN
            ),
            scheduleEnabled = json.optBoolean(
                "scheduleEnabled",
                TimerConfigContract.DEFAULT_SCHEDULE_ENABLED
            ),
            scheduleStartMinute = json.optInt(
                "scheduleStartMinute",
                TimerConfigContract.DEFAULT_SCHEDULE_START
            ),
            scheduleEndMinute = json.optInt(
                "scheduleEndMinute",
                TimerConfigContract.DEFAULT_SCHEDULE_END
            ),
            autoWake = json.optBoolean("autoWake", TimerConfigContract.DEFAULT_AUTO_WAKE),
            wakeRetryMinutes = json.optInt(
                "wakeRetryMinutes",
                TimerConfigContract.DEFAULT_WAKE_RETRY_MINUTES
            ),
            wakeRetryIntervalSeconds = json.optInt(
                "wakeRetryIntervalSeconds",
                TimerConfigContract.DEFAULT_WAKE_RETRY_INTERVAL_SECONDS
            ),
            wakeFallbackProximity = json.optBoolean(
                "wakeFallbackProximity",
                TimerConfigContract.DEFAULT_WAKE_FALLBACK_PROXIMITY
            ),
        )
    }.getOrElse {
        Log.w(TAG, "config read failed: ${it.message}")
        null
    }

    fun write(context: Context, config: TimerConfig) {
        runCatching {
            val json = JSONObject()
                .put("keepAodMinutes", config.keepAodMinutes)
                .put("forceDeburnIn", config.forceDeburnIn)
                .put("scheduleEnabled", config.scheduleEnabled)
                .put("scheduleStartMinute", config.scheduleStartMinute)
                .put("scheduleEndMinute", config.scheduleEndMinute)
                .put("autoWake", config.autoWake)
                .put("wakeRetryMinutes", config.wakeRetryMinutes)
                .put("wakeRetryIntervalSeconds", config.wakeRetryIntervalSeconds)
                .put("wakeFallbackProximity", config.wakeFallbackProximity)

            val target = file(context)
            target.parentFile?.mkdirs()

            val temp = File(target.parentFile, TEMP_NAME)
            temp.writeText(json.toString())
            if (!temp.renameTo(target)) {
                // 某些文件系统上 rename 会失败，退化到直接覆盖
                target.writeText(json.toString())
                temp.delete()
            }
            com.op.aod.timer.util.Logs.d("config written: ${target.absolutePath}")
        }.onFailure {
            Log.w(TAG, "config write failed: ${it.message}")
        }
    }

    private fun file(context: Context): File {
        val storage = runCatching { context.createDeviceProtectedStorageContext() }
            .getOrDefault(context)
        return File(storage.filesDir, FILE_NAME)
    }
}
