package com.op.aod.timer.data

/**
 * 模块配置的键与默认值。
 *
 * 读写统一走 YukiHookAPI 的 `YukiHookPrefsBridge`：
 * 模块 App 内等价于普通 SharedPreferences，被 Hook 的 SystemUI 进程内
 * 等价于 XSharedPreferences，因此不依赖模块 App 是否启动过。
 */
object TimerConfigContract {

    /** Provider authority 与路径。 */
    const val AUTHORITY = "com.op.aod.timer.config"
    const val PATH = "timer_config"
    const val URI_STRING = "content://$AUTHORITY/$PATH"

    /** 保持 AOD 显示的时长（分钟）：`0` = 跟随系统，`-1` = 常亮。 */
    const val KEY_KEEP_AOD_MINUTES = "keep_aod_minutes"
    const val DEFAULT_KEEP_AOD_MINUTES = 0

    /** 强制让全景 AOD 的防烧屏位移判定成立。 */
    const val KEY_FORCE_DEBURN_IN = "force_deburn_in"
    const val DEFAULT_FORCE_DEBURN_IN = false

    /** 启用时段：只在设定的时间窗口内让模块生效。 */
    const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    const val DEFAULT_SCHEDULE_ENABLED = false

    /** 时段起点，从 0 点起的分钟数。默认 08:00。 */
    const val KEY_SCHEDULE_START = "schedule_start_minute"
    const val DEFAULT_SCHEDULE_START = 8 * 60

    /** 时段终点，从 0 点起的分钟数。默认 23:00。 */
    const val KEY_SCHEDULE_END = "schedule_end_minute"
    const val DEFAULT_SCHEDULE_END = 23 * 60

    /**
     * 已废弃：自动唤醒功能已移除，保留键名仅为兼容旧配置。
     * 见 hook/AutoWakeHook.kt 中的说明。
     */
    const val KEY_AUTO_WAKE = "auto_wake"
    const val DEFAULT_AUTO_WAKE = false
}

/** 模块配置快照。 */
data class TimerConfig(
    val keepAodMinutes: Int = TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES,
    val forceDeburnIn: Boolean = TimerConfigContract.DEFAULT_FORCE_DEBURN_IN,
    val scheduleEnabled: Boolean = TimerConfigContract.DEFAULT_SCHEDULE_ENABLED,
    val scheduleStartMinute: Int = TimerConfigContract.DEFAULT_SCHEDULE_START,
    val scheduleEndMinute: Int = TimerConfigContract.DEFAULT_SCHEDULE_END,
    val autoWake: Boolean = TimerConfigContract.DEFAULT_AUTO_WAKE,
)
