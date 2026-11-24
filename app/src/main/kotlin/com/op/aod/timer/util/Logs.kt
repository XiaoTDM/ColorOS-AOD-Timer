package com.op.aod.timer.util

import android.util.Log
import com.op.aod.timer.BuildConfig

/**
 * 统一日志出口。
 *
 * - [d]：调试日志，仅 debug 包输出；
 * - [i] / [w]：关键日志，release 保留（数量要少，避免刷屏）。
 */
internal object Logs {

    private const val TAG = "AOD_Timer"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }
}
