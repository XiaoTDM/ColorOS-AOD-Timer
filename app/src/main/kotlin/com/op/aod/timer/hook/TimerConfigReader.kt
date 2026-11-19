package com.op.aod.timer.hook

import android.content.Context
import android.net.Uri
import com.op.aod.timer.data.TimerConfigContract

/**
 * Hook 侧配置读取：1 秒 TTL 缓存，避免高频 IPC。
 */
internal object TimerConfigReader {

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    @Volatile
    private var cached: Int? = null

    @Volatile
    private var lastReadNs = 0L

    private const val CACHE_TTL_NS = 1_000_000_000L

    fun read(context: Context?): Int {
        if (context == null) return TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES

        val now = System.nanoTime()
        cached?.let { value ->
            if (now - lastReadNs < CACHE_TTL_NS) return value
        }

        val fresh = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getInt(0) else null
            }
        }.getOrNull()

        if (fresh != null) {
            cached = fresh
            lastReadNs = now
            return fresh
        }

        return cached ?: TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
    }
}
