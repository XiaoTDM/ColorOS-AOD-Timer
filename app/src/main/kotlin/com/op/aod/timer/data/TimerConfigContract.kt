package com.op.aod.timer.data

/**
 * 模块配置通道的唯一定义（Provider / Store / Hook 三侧共用）。
 */
object TimerConfigContract {

    const val AUTHORITY = "com.op.aod.timer.config"
    const val PATH = "timer_config"

    /** 保持 AOD 显示的时长（分钟）：`0` = 跟随系统，`-1` = 常亮。 */
    const val KEY_KEEP_AOD_MINUTES = "keep_aod_minutes"
    const val DEFAULT_KEEP_AOD_MINUTES = 0

    const val URI_STRING = "content://$AUTHORITY/$PATH"
}
