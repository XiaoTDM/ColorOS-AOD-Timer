package com.op.aod.timer.hook

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs
import com.op.aod.timer.data.TimerConfig
import com.op.aod.timer.data.TimerConfigContract

/**
 * Hook 侧配置读取：1 秒 TTL 缓存，避免高频读取。
 *
 * 通过 YukiHookPrefsBridge 在宿主进程内直接读模块 prefs（XSharedPreferences），
 * 不依赖模块 App 进程是否启动，重启后立即生效。
 */
internal object TimerConfigReader {

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

        val prefs = context.prefs(TimerConfigContract.PREFS_NAME)
        if (!prefs.isPreferencesAvailable) {
            return cached ?: TimerConfig()
        }

        val fresh = TimerConfig(
            keepAodMinutes = prefs.getInt(
                TimerConfigContract.KEY_KEEP_AOD_MINUTES,
                TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
            ),
            forceDeburnIn = prefs.getBoolean(
                TimerConfigContract.KEY_FORCE_DEBURN_IN,
                TimerConfigContract.DEFAULT_FORCE_DEBURN_IN
            ),
            scheduleEnabled = prefs.getBoolean(
                TimerConfigContract.KEY_SCHEDULE_ENABLED,
                TimerConfigContract.DEFAULT_SCHEDULE_ENABLED
            ),
            scheduleStartMinute = prefs.getInt(
                TimerConfigContract.KEY_SCHEDULE_START,
                TimerConfigContract.DEFAULT_SCHEDULE_START
            ),
            scheduleEndMinute = prefs.getInt(
                TimerConfigContract.KEY_SCHEDULE_END,
                TimerConfigContract.DEFAULT_SCHEDULE_END
            ),
            autoWake = prefs.getBoolean(
                TimerConfigContract.KEY_AUTO_WAKE,
                TimerConfigContract.DEFAULT_AUTO_WAKE
            ),
        )

        cached = fresh
        lastReadNs = now
        return fresh
    }
}
