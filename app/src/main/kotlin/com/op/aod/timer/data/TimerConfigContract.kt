package com.op.aod.timer.data

/**
 * 模块配置的键与默认值。
 *
 * 存储：模块 App 写 JSON 文件（设备加密目录），SystemUI 通过只读
 * ContentProvider 读取，并由 SystemUI 自缓存兜底。
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

    /** 时段开始时尝试主动唤醒 AOD（实验性）。 */
    const val KEY_AUTO_WAKE = "auto_wake"
    const val DEFAULT_AUTO_WAKE = false

    /** 唤醒被遮挡跳过后的重试时长（分钟）：0 = 不重试，最大 240。 */
    const val KEY_WAKE_RETRY_MINUTES = "wake_retry_minutes"
    const val DEFAULT_WAKE_RETRY_MINUTES = 30
    const val MAX_WAKE_RETRY_MINUTES = 240

    /** 遮挡期间的重试间隔（秒）。 */
    const val KEY_WAKE_RETRY_INTERVAL_SECONDS = "wake_retry_interval_seconds"
    const val DEFAULT_WAKE_RETRY_INTERVAL_SECONDS = 60
    const val MIN_WAKE_RETRY_INTERVAL_SECONDS = 30
    const val MAX_WAKE_RETRY_INTERVAL_SECONDS = 300

    /**
     * 镜像失效时是否允许主动采样接近传感器。
     *
     * 默认关闭：拿不到镜像数据时按"遮挡"处理，不冒险点亮。
     * 开启后会在每次判断时注册传感器 500ms，可能增加误触发概率。
     */
    const val KEY_WAKE_FALLBACK_PROXIMITY = "wake_fallback_proximity"
    const val DEFAULT_WAKE_FALLBACK_PROXIMITY = false
}

/** 模块配置快照。 */
data class TimerConfig(
    val keepAodMinutes: Int = TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES,
    val forceDeburnIn: Boolean = TimerConfigContract.DEFAULT_FORCE_DEBURN_IN,
    val scheduleEnabled: Boolean = TimerConfigContract.DEFAULT_SCHEDULE_ENABLED,
    val scheduleStartMinute: Int = TimerConfigContract.DEFAULT_SCHEDULE_START,
    val scheduleEndMinute: Int = TimerConfigContract.DEFAULT_SCHEDULE_END,
    val autoWake: Boolean = TimerConfigContract.DEFAULT_AUTO_WAKE,
    val wakeRetryMinutes: Int = TimerConfigContract.DEFAULT_WAKE_RETRY_MINUTES,
    val wakeRetryIntervalSeconds: Int =
        TimerConfigContract.DEFAULT_WAKE_RETRY_INTERVAL_SECONDS,
    val wakeFallbackProximity: Boolean =
        TimerConfigContract.DEFAULT_WAKE_FALLBACK_PROXIMITY,
)
