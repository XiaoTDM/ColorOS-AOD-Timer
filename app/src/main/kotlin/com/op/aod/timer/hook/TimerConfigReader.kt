package com.op.aod.timer.hook

import android.content.Context
import android.net.Uri
import android.util.Log
import com.op.aod.timer.data.TimerConfig
import com.op.aod.timer.data.TimerConfigContract
import org.json.JSONObject
import java.io.File

/**
 * Hook 侧配置读取。
 *
 * 读取优先级：
 *   1. 1 秒内存缓存
 *   2. 模块 App 的只读 ContentProvider（配置变更后 1 秒内生效）
 *   3. SystemUI 自己保存的磁盘缓存（模块 App 进程不在时兜底）
 *   4. 默认值
 *
 * 之所以要第 3 层：Provider 跑在模块 App 进程里，进程被系统冻结或杀掉后
 * 查询会失败；把最后一次成功读取的配置缓存到 SystemUI 自己的设备加密目录，
 * 就能保证 App 不在时模块照常工作。
 */
internal object TimerConfigReader {

    private const val TAG = "AOD_Timer"

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    private const val CACHE_FILE = "aod_timer_config_cache.json"
    private const val CACHE_TTL_NS = 1_000_000_000L
    private const val WARN_INTERVAL_NS = 10_000_000_000L

    @Volatile
    private var cached: TimerConfig? = null

    @Volatile
    private var lastReadNs = 0L

    @Volatile
    private var loggedFirstRead = false

    @Volatile
    private var lastWarnNs = 0L

    fun read(context: Context?): TimerConfig {
        if (context == null) return TimerConfig()

        val now = System.nanoTime()
        cached?.let { value ->
            if (now - lastReadNs < CACHE_TTL_NS) return value
        }

        val fresh = queryProvider(context)
        if (fresh != null) {
            if (cached != fresh) cacheToDisk(context, fresh)
            cached = fresh
            lastReadNs = now
            if (!loggedFirstRead) {
                loggedFirstRead = true
                Log.i(
                    TAG,
                    "config loaded: keep=${fresh.keepAodMinutes}, " +
                        "schedule=${fresh.scheduleEnabled} " +
                        "${fresh.scheduleStartMinute}-${fresh.scheduleEndMinute}, " +
                        "forceShift=${fresh.forceDeburnIn}"
                )
            }
            return fresh
        }

        // Provider 不可用（模块 App 进程不在等），退回磁盘缓存
        cached?.let { return it }
        val disk = readCache(context)
        if (disk != null) {
            cached = disk
            lastReadNs = now
            Log.i(TAG, "config from disk cache: keep=${disk.keepAodMinutes}")
            return disk
        }

        if (now - lastWarnNs > WARN_INTERVAL_NS) {
            lastWarnNs = now
            Log.w(TAG, "config unavailable (provider and cache both failed); using defaults")
        }
        return TimerConfig()
    }

    private fun queryProvider(context: Context): TimerConfig? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                TimerConfig(
                    keepAodMinutes = cursor.getInt(0),
                    forceDeburnIn = cursor.getInt(1) == 1,
                    scheduleEnabled = cursor.getInt(2) == 1,
                    scheduleStartMinute = cursor.getInt(3),
                    scheduleEndMinute = cursor.getInt(4),
                    autoWake = cursor.getInt(5) == 1,
                    wakeRetryMinutes = cursor.getInt(6),
                    wakeRetryIntervalSeconds = cursor.getInt(7),
                    wakeFallbackProximity = cursor.getInt(8) == 1,
                )
            } else {
                null
            }
        }
    }.getOrNull()

    private fun cacheFile(context: Context): File = runCatching {
        val storage = context.createDeviceProtectedStorageContext()
        File(storage.filesDir, CACHE_FILE)
    }.getOrElse {
        File(context.filesDir, CACHE_FILE)
    }

    private fun cacheToDisk(context: Context, config: TimerConfig) {
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

            val file = cacheFile(context)
            file.parentFile?.mkdirs()
            file.writeText(json.toString())
        }.onFailure {
            Log.w(TAG, "config cache write failed: ${it.message}")
        }
    }

    private fun readCache(context: Context): TimerConfig? = runCatching {
        val file = cacheFile(context)
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
    }.getOrNull()
}
