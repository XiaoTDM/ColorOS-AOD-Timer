package com.op.aod.timer.hook

import com.op.aod.timer.data.TimerConfig
import java.util.Calendar

/**
 * 启用时段的判定。
 *
 * 时间以"当天第几分钟"表示（0..1439）：
 * - `start < end`：普通区间，例如 08:00–22:00；
 * - `start > end`：跨午夜区间，例如 22:00–次日 08:00；
 * - `start == end`：视为全天生效。
 */
internal object Schedule {

    fun currentMinuteOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    fun contains(start: Int, end: Int, now: Int): Boolean = when {
        start == end -> true
        start < end -> now in start until end
        else -> now >= start || now < end
    }

    /** 未启用时段时恒为 true；启用时按窗口判断。 */
    fun isActive(config: TimerConfig, now: Int = currentMinuteOfDay()): Boolean =
        !config.scheduleEnabled ||
            contains(config.scheduleStartMinute, config.scheduleEndMinute, now)
}
