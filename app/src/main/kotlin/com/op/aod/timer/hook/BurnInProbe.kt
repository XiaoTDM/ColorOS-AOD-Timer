package com.op.aod.timer.hook

import android.content.Context
import android.util.Log
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass

/**
 * 防烧屏探针（仅 DEBUG 构建注册）。
 *
 * 目的：确认 AOD 的防烧屏位移是否真的在发生，避免靠截图猜测。
 *
 * 打印内容：
 * - `updateNextPosition(int)`：新的 topMargin，以及是否与上次不同；
 * - `resetBurninPosition()`：位移重置事件；
 * - 同时附上 `AodData.getAodBurningTop()`（本机配置的位移幅度）和
 *   `mDisableAodBurninAnim`（是否禁用了位移动画）。
 *
 * 用法：
 *   adb logcat -s AOD_Probe:D
 *
 * 判读：
 * - `changed=false` 连续出现 → 位移没有发生（幅度为 0 或被禁用）；
 * - `changed=true` 且 newTop 在若干像素内来回 → 位移有效；
 * - `burningTop=0` → 该机型配置里根本没有给偏移量。
 */
internal object BurnInProbe {

    private const val TAG = "AOD_Probe"

    private const val AOD_CLOCK_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodClockLayout"
    private const val AOD_DATA = "com.oplus.systemui.aod.aodclock.constant.AodData"
    private const val PANORAMIC_DEBURN =
        "com.oplus.systemui.aod.controller.PanoramicAODUIDeBurnInControllerImpl"

    @Volatile
    private var lastTop = Int.MIN_VALUE

    @Volatile
    private var lastY: Float? = null

    fun YukiBaseHooker.hookBurnInProbe() {
        // 全景 AOD 真正的位移控制器：它的 performAodUpdate 才是每分钟更新时移动
        // 时钟/通知/深景元素的地方。
        runCatching {
            PANORAMIC_DEBURN
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "shouldDoPanoramicAodUIDeBurnIn"
                }
                .hook {
                    after {
                        Log.d(
                            TAG,
                            "shouldDoPanoramicAodUIDeBurnIn = ${result<Boolean>()} ${dump()}"
                        )
                    }
                }
        }.onFailure {
            Log.d(TAG, "shouldDoPanoramicAodUIDeBurnIn hook failed: ${it.message}")
        }

        runCatching {
            PANORAMIC_DEBURN
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "performAodUpdate"
                    emptyParameters()
                }
                .hook {
                    after {
                        val controller = instance<Any>()
                        val y = controller.floatField("currentPanoramicElementY")
                        val base = controller.floatField("styleClockTranslationY")
                        val changed = y != lastY
                        lastY = y
                        Log.d(
                            TAG,
                            "panoramic performAodUpdate y=$y base=$base changed=$changed ${dump()}"
                        )
                    }
                }
        }.onFailure {
            Log.d(TAG, "panoramic performAodUpdate hook failed: ${it.message}")
        }

        // 最可靠的测量点：每秒/每分钟都会执行的刷新回调。
        // 直接读布局在屏幕上的绝对坐标，不依赖任何内部位移方法。
        runCatching {
            AOD_CLOCK_LAYOUT
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "performAodUpdate"
                    parameters(Boolean::class)
                }
                .hook {
                    after {
                        val view = instance<View>()
                        val loc = IntArray(2)
                        view.getLocationOnScreen(loc)
                        Log.d(
                            TAG,
                            "performAodUpdate screenX=${loc[0]} screenY=${loc[1]} ${dump()}"
                        )
                    }
                }
        }.onFailure {
            Log.d(TAG, "performAodUpdate hook failed: ${it.message}")
        }

        // 主刷新路径：无参重载。即使位置没变，每次时钟刷新都会走这里，
        // 用来区分"方法没被调用"和"调用了但位置不变"。
        runCatching {
            AOD_CLOCK_LAYOUT
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "updateNextPosition"
                    emptyParameters()
                }
                .hook {
                    before {
                        Log.d(TAG, "updateNextPosition() called ${dump()}")
                    }
                }
        }.onFailure {
            Log.d(TAG, "updateNextPosition() hook failed: ${it.message}")
        }

        // 私有的 boolean 重载（真正做位置计算的那个）
        runCatching {
            AOD_CLOCK_LAYOUT
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "updateNextPosition"
                    parameters(Boolean::class)
                }
                .hook {
                    before {
                        Log.d(TAG, "updateNextPosition(boolean=${args(0).any()}) called")
                    }
                }
        }.onFailure {
            Log.d(TAG, "updateNextPosition(boolean) hook failed: ${it.message}")
        }

        // 公开的 int 重载：真正应用 topMargin 的地方
        runCatching {
            AOD_CLOCK_LAYOUT
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "updateNextPosition"
                    parameters(Int::class)
                }
                .hook {
                    after {
                        val top = args(0).any() as? Int ?: return@after
                        val changed = top != lastTop
                        lastTop = top
                        Log.d(TAG, "updateNextPosition newTop=$top changed=$changed ${dump()}")
                    }
                }
        }.onFailure {
            Log.d(TAG, "updateNextPosition(int) hook failed: ${it.message}")
        }

        runCatching {
            AOD_CLOCK_LAYOUT
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "resetBurninPosition" }
                .hook {
                    after {
                        lastTop = Int.MIN_VALUE
                        Log.d(TAG, "resetBurninPosition ${dump()}")
                    }
                }
        }.onFailure {
            Log.d(TAG, "resetBurninPosition hook failed: ${it.message}")
        }

        Log.d(TAG, "burn-in probe installed")
    }

    /** 读取本机配置的位移幅度与开关，失败返回可读的错误文本。 */
    private fun dump(): String = runCatching {
        val ctx = MainHook.hostAppContext ?: return "(no host context)"
        val clazz = Class.forName(AOD_DATA, false, ctx.classLoader)
        val instance = clazz.getMethod("getInstance", Context::class.java).invoke(null, ctx)

        val burningTop = runCatching { clazz.getMethod("getAodBurningTop").invoke(instance) }.getOrNull()
        val fieldTop = runCatching { clazz.getField("mAodBurningTop").getInt(instance) }.getOrNull()
        val disableAnim = runCatching { clazz.getField("mDisableAodBurninAnim").getBoolean(instance) }.getOrNull()

        "burningTop=$burningTop fieldTop=$fieldTop disableBurnInAnim=$disableAnim"
    }.getOrElse { "dump failed: ${it.message}" }
}

/** 读取 float 字段，失败返回 null。 */
private fun Any.floatField(name: String): Float? = runCatching {
    val field = javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.getFloat(this)
}.getOrNull()
