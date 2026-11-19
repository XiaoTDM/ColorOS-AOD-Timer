package com.op.aod.timer.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri

/**
 * UI 侧配置读写。
 */
object TimerConfigStore {

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    @Volatile
    private var cached: TimerConfig? = null

    fun read(resolver: ContentResolver): TimerConfig {
        cached?.let { return it }

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
        return TimerConfig()
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
