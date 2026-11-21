package com.op.aod.timer.data

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs

/**
 * UI 侧配置读写，基于 YukiHookPrefsBridge。
 *
 * 在模块 App 内它等价于普通 SharedPreferences；写入后会被 LSPosed 重定向到
 * 框架的 prefs 目录，宿主进程可以直接读取，因此**不需要模块 App 先启动一次**。
 */
object TimerConfigStore {

    /** 读取配置；prefs 不可用时返回 null，由调用方重试。 */
    fun read(context: Context): TimerConfig? {
        val prefs = context.prefs(TimerConfigContract.PREFS_NAME)
        if (!prefs.isPreferencesAvailable) return null

        return TimerConfig(
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
    }

    fun write(context: Context, config: TimerConfig) {
        val editor = context.prefs(TimerConfigContract.PREFS_NAME).edit()
        editor.putInt(TimerConfigContract.KEY_KEEP_AOD_MINUTES, config.keepAodMinutes)
        editor.putBoolean(TimerConfigContract.KEY_FORCE_DEBURN_IN, config.forceDeburnIn)
        editor.putBoolean(TimerConfigContract.KEY_SCHEDULE_ENABLED, config.scheduleEnabled)
        editor.putInt(TimerConfigContract.KEY_SCHEDULE_START, config.scheduleStartMinute)
        editor.putInt(TimerConfigContract.KEY_SCHEDULE_END, config.scheduleEndMinute)
        editor.putBoolean(TimerConfigContract.KEY_AUTO_WAKE, config.autoWake)
        // 同步落盘，避免异常重启丢配置
        editor.commit()
    }
}
