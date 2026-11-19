package com.op.aod.timer.hook

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.timer.BuildConfig

/**
 * 节能模式下按配置时长保持 AOD 显示。
 *
 * 原理
 * ----
 * ColorOS 的 `AODDisplayUtil.getKeepAodOn()`（int）是 ROM 自带的"保持常亮"调试开关，
 * 在多处节能逻辑里被短路使用：
 *
 * - `AodUpdateManager.isDisplayModeAllowUpdateClock()`：为 1 时直接 return true；
 * - `BaseAodClockLayoutController.onEnergySavingNotifyHide()`：为 1 时直接 return，
 *   不执行隐藏；
 * - `BaseAodController.isUpsideDownEnable()`：为 1 时视为常亮。
 *
 * 模块在该方法返回后按时间窗口改写结果，只影响这一个内存值，
 * 不写 SecureSetting、不改渲染路径、不动用户偏好，滑条归零即完全恢复系统行为。
 *
 * 计时
 * ----
 * 以 `AodData.setAodIsInShow(true)` 作为本次熄屏会话的起点；亮屏
 * （`PowerManager.isInteractive`）时清零。息屏期间节能策略的反复隐藏/重现
 * 不会重置计时，只有真正重新熄屏才重新计时。
 */
internal object KeepAodHook {

    private const val TAG = "AOD_Timer"

    private const val AOD_DATA = "com.oplus.systemui.aod.aodclock.constant.AodData"
    private const val DISPLAY_UTIL = "com.oplus.systemui.aod.display.AODDisplayUtil"

    /** 本次熄屏会话开始时刻（elapsedRealtime），0 表示未开始。 */
    @Volatile
    private var showStartMs = 0L

    fun YukiBaseHooker.hookKeepAodDuration() {
        // 记录 / 重置计时起点
        runCatching {
            AOD_DATA
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "setAodIsInShow"
                    parameters(Boolean::class)
                }
                .hook {
                    after {
                        val showing = args(0).any() as? Boolean ?: return@after
                        if (showing) {
                            if (showStartMs == 0L) {
                                showStartMs = SystemClock.elapsedRealtime()
                                debugLog("session start")
                            }
                        } else if (isScreenInteractive()) {
                            // 亮屏才算会话结束；息屏期间的自动隐藏不重置计时
                            showStartMs = 0L
                            debugLog("session reset (screen on)")
                        }
                    }
                }
        }.onFailure {
            debugLog("setAodIsInShow hook failed, ${it.message}")
        }

        // 时间窗口内伪装成 "keep aod on"
        runCatching {
            DISPLAY_UTIL
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "getKeepAodOn" }
                .hook {
                    after {
                        val minutes = TimerConfigReader.read(MainHook.hostAppContext).keepAodMinutes
                        if (minutes == 0) return@after

                        val keep = shouldKeep(minutes)
                        result = if (keep) 1 else 0
                        debugLog("getKeepAodOn -> ${if (keep) 1 else 0} (minutes=$minutes)")
                    }
                }
        }.onFailure {
            debugLog("getKeepAodOn hook failed, ${it.message}")
        }
    }

    private fun shouldKeep(minutes: Int): Boolean {
        if (minutes < 0) return true

        val start = showStartMs
        if (start == 0L) return false
        return SystemClock.elapsedRealtime() - start < minutes * 60_000L
    }

    private fun isScreenInteractive(): Boolean = runCatching {
        val ctx = MainHook.hostAppContext ?: return false
        (ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
    }.getOrDefault(false)

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "AOD_KEEP: $message")
    }
}
