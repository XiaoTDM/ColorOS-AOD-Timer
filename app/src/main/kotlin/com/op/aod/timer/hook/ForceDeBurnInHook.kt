package com.op.aod.timer.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.timer.BuildConfig

/**
 * 强制启用全景 AOD 的防烧屏位移。
 *
 * 背景
 * ----
 * `PanoramicAODUIDeBurnInControllerImpl.shouldDoPanoramicAodUIDeBurnIn()` 的判定是：
 *
 *     isSupportPanoramicAllDay
 *       && (isCurrentAodEnableImmediateOpen()
 *           || (isCurrentAodEnableTimeOpen() && checkInDuration()))
 *       && !getLargeFoldDeviceInPanoramicEnergyState()
 *
 * 面板能力码没有置位"全天"（bit8）、或用户选的是省电模式时，这个判定恒为 false，
 * 于是 `performAodUpdate()` 每次都直接返回，全景位移完全不会发生。
 *
 * 本 Hook 在开关打开时让该判定直接返回 true，从而恢复位移。
 *
 * 安全边界
 * ----
 * 方法内部在 `clockSize == IMMERSED` 分支会先把各元素平移重置回基准再返回 false，
 * 那是沉浸态布局，不属于位移场景；这里对该分支不做覆盖，避免把位移叠加到沉浸态上。
 */
internal object ForceDeBurnInHook {

    private const val TAG = "AOD_Timer"

    private const val CONTROLLER =
        "com.oplus.systemui.aod.controller.PanoramicAODUIDeBurnInControllerImpl"

    fun YukiBaseHooker.hookForceDeBurnIn() {
        runCatching {
            CONTROLLER
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "shouldDoPanoramicAodUIDeBurnIn" }
                .hook {
                    after {
                        val cfg = TimerConfigReader.read(MainHook.hostAppContext)
                        if (!cfg.forceDeburnIn) return@after
                        // 时段外不干预，交还给系统
                        if (!Schedule.isActive(cfg)) return@after
                        if (instance<Any>().isImmersed()) return@after

                        if (result<Boolean>() != true) {
                            result = true
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "AOD_DEBURN: shouldDoPanoramicAodUIDeBurnIn -> true")
                            }
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AOD_DEBURN: hook failed, ${it.message}")
            }
        }
    }
}

/** 读取 `clockSize` 字段判断是否处于沉浸态。读不到时按"非沉浸"处理。 */
private fun Any.isImmersed(): Boolean = runCatching {
    val field = javaClass.getDeclaredField("clockSize")
    field.isAccessible = true
    field.get(this)?.toString() == "IMMERSED"
}.getOrDefault(false)
