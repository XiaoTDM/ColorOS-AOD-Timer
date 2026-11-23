package com.op.aod.timer.data

import android.content.Context
import android.net.Uri

/**
 * UI 侧配置读写：直接操作文件。
 *
 * 模块 App 与 Provider 运行在同一进程，UI 写入后 Provider 立即可读，
 * 不需要再经过 SharedPreferences / XSharedPreferences。
 */
object TimerConfigStore {

    private val uri: Uri = Uri.parse(TimerConfigContract.URI_STRING)

    fun read(context: Context): TimerConfig = TimerConfigFile.read(context) ?: TimerConfig()

    fun write(context: Context, config: TimerConfig) {
        TimerConfigFile.write(context, config)
    }

    /** 调试用：当前配置文件路径。 */
    fun configPath(context: Context): String = uri.toString()
}
