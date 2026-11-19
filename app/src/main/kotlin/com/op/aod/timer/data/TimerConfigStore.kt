package com.op.aod.timer.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri

/**
 * UI 侧配置读写。
 *
 * [read] 在 Provider 尚未就绪时返回 null 而不是默认值——这一点很关键：
 * 如果这里返回默认的 `keepAodMinutes = 0`，UI 会用 0 初始化，
 * 用户之后任何一次操作都会把 0 写回 Provider，看起来就像"重启后配置变成 0"。
 */
object TimerConfigStore {

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    @Volatile
    private var cached: TimerConfig? = null

    /** 读取配置；连续失败时返回 null，由调用方重试。不要在主线程调用。 */
    fun read(resolver: ContentResolver): TimerConfig? {
        cached?.let { return it }

        repeat(3) { attempt ->
            val fresh = runCatching {
                resolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        TimerConfig(
                            keepAodMinutes = c.getInt(0),
                            forceDeburnIn = c.getInt(1) == 1,
                        )
                    } else {
                        null
                    }
                }
            }.getOrNull()

            if (fresh != null) {
                cached = fresh
                return fresh
            }

            if (attempt < 2) runCatching { Thread.sleep(120L) }
        }

        return null
    }

    fun write(resolver: ContentResolver, config: TimerConfig) {
        val values = ContentValues().apply {
            put(TimerConfigContract.KEY_KEEP_AOD_MINUTES, config.keepAodMinutes)
            put(TimerConfigContract.KEY_FORCE_DEBURN_IN, config.forceDeburnIn)
        }
        runCatching { resolver.update(uri, values, null, null) }
        cached = config
    }
}
