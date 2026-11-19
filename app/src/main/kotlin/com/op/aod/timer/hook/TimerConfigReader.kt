package com.op.aod.timer.hook

import android.content.Context
import android.net.Uri
import com.op.aod.timer.data.TimerConfig
import com.op.aod.timer.data.TimerConfigContract

/**
 * Hook 侧配置读取：1 秒 TTL 缓存，避免高频 IPC。
 */
internal object TimerConfigReader {

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    @Volatile
    private var cached: TimerConfig? = null

    @Volatile
    private var lastReadNs = 0L

    private const val CACHE_TTL_NS = 1_000_000_000L

    fun read(context: Context?): TimerConfig {
        if (context == null) return TimerConfig()

        val now = System.nanoTime()
        cached?.let { value ->
            if (now - lastReadNs < CACHE_TTL_NS) return value
        }

        val fresh = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
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
            lastReadNs = now
            return fresh
        }

        return cached ?: TimerConfig()
    }
}
