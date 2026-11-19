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
    private var cached: Int? = null

    fun read(resolver: ContentResolver): Int {
        cached?.let { return it }

        val fresh = runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getInt(0) else null
            }
        }.getOrNull()

        if (fresh != null) {
            cached = fresh
            return fresh
        }
        return TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
    }

    fun write(resolver: ContentResolver, minutes: Int) {
        val values = ContentValues().apply {
            put(TimerConfigContract.KEY_KEEP_AOD_MINUTES, minutes)
        }
        runCatching { resolver.update(uri, values, null, null) }
        cached = minutes
    }
}
