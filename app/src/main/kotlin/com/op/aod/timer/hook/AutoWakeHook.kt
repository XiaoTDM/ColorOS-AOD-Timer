package com.op.aod.timer.hook

// ============================================================================
// 已废弃：时段开始时的自动唤醒（v1.2 移除）
//
// 废弃原因
// --------
// 1. 唤醒入口 OplusWakeUpController.notifyWakeUpCallback(0) 没有遮挡判断，
//    被遮挡（口袋/桌面朝下）时也会点亮，需要额外维护接近传感器状态；
// 2. 该状态只能镜像 ROM 的手势传感器回调，ROM 一改类名/结构就会退化成自采样，
//    两条路都不够稳；
// 3. 常驻轮询线程 + 主动点亮带来额外的耗电与烧屏风险，收益不足以抵消。
//
// TODO: 若将来重新实现，优先考虑"复用 ROM 定时显示的唤醒链"
//       （WAKEUP_TIME 广播 → mWakeUpReceiver → DozeService），
//       但需要同时模拟 isCurrentAodEnableTimeOpen / getBeginHour 等
//       至少 5 个落点，并处理 CommonUtils.checkInDuration 直接读 SecureSetting 的问题。
//       同时必须确认 DozeService 启动前已有遮挡判断。
// ============================================================================

/*
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

internal object AutoWakeHook {

    private const val TAG = "AOD_Timer"

    private const val WAKE_CONTROLLER = "com.oplus.systemui.aod.display.OplusWakeUpController"
    private const val AOD_DATA = "com.oplus.systemui.aod.aodclock.constant.AodData"

    private const val CHECK_INTERVAL_MS = 15_000L

    private const val GESTURE_SENSOR_LISTENER =
        "com.oplus.systemui.keyguard.gesture.OplusBlackScreenGestureControllExImpl\$sensorListener\$1"

    @Volatile
    private var mirroredNear: Boolean? = null

    fun YukiBaseHooker.hookAutoWake() {
        hookProximityMirror()

        thread(name = "aod-timer-autowake", isDaemon = true) {
            var wasActive = false
            while (true) {
                try {
                    val context = MainHook.hostAppContext
                    if (context == null) {
                        Thread.sleep(5_000)
                        continue
                    }

                    val config = TimerConfigReader.read(context)
                    val inWindow = config.scheduleEnabled &&
                        config.autoWake &&
                        Schedule.contains(
                            config.scheduleStartMinute,
                            config.scheduleEndMinute,
                            Schedule.currentMinuteOfDay(),
                        )

                    if (inWindow && !wasActive) {
                        wake(context)
                    }
                    wasActive = inWindow

                    Thread.sleep(CHECK_INTERVAL_MS)
                } catch (_: Throwable) {
                    Thread.sleep(CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun YukiBaseHooker.hookProximityMirror() {
        runCatching {
            GESTURE_SENSOR_LISTENER
                .toClass(appClassLoader)
                .resolve()
                .firstMethod {
                    name = "onSensorChanged"
                    parameters(SensorEvent::class)
                }
                .hook {
                    after {
                        val event = args(0).any() as? SensorEvent ?: return@after
                        val sensor = event.sensor ?: return@after
                        if (sensor.type != Sensor.TYPE_PROXIMITY) return@after
                        val value = event.values.firstOrNull() ?: return@after
                        mirroredNear = value < sensor.maximumRange
                    }
                }
        }.onFailure {
            debugLog("proximity mirror hook failed: ${it.message}")
        }
    }

    private fun wake(context: Context) {
        if (isInteractive(context)) {
            debugLog("skip: screen on")
            return
        }
        if (isCovered(context)) {
            debugLog("skip: proximity covered")
            return
        }
        if (notNeedWakeAod(context)) {
            debugLog("skip: notNeedWakeAod")
            return
        }
        if (isAodShowing(context)) {
            debugLog("skip: AOD already showing")
            return
        }

        debugLog("window start, calling notifyWakeUpCallback(0)")
        runCatching {
            val clazz = Class.forName(WAKE_CONTROLLER, false, context.classLoader)
            val companion = clazz.getField("Companion").get(null)
            val instance = companion.javaClass
                .getMethod("getInstance", Context::class.java)
                .invoke(companion, context)
            instance.javaClass
                .getMethod("notifyWakeUpCallback", java.lang.Integer.TYPE)
                .invoke(instance, 0)
        }.onSuccess {
            debugLog("notifyWakeUpCallback(0) invoked")
        }.onFailure {
            debugLog("notifyWakeUpCallback failed: ${it.message}")
        }
    }

    private fun isCovered(context: Context): Boolean {
        mirroredNear?.let { return it }
        return sampleProximity(context)
    }

    private fun sampleProximity(context: Context): Boolean = runCatching {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            ?: return false

        val latch = CountDownLatch(1)
        val near = AtomicBoolean(false)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull() ?: return
                near.set(value < sensor.maximumRange)
                latch.countDown()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        latch.await(500, TimeUnit.MILLISECONDS)
        sensorManager.unregisterListener(listener)
        near.get()
    }.getOrDefault(false)

    private fun isInteractive(context: Context): Boolean = runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
    }.getOrDefault(false)

    private fun notNeedWakeAod(context: Context): Boolean = runCatching {
        val clazz = Class.forName(
            "com.oplus.systemui.aod.common.AodManager",
            false,
            context.classLoader
        )
        val instance = clazz.getMethod("getInstance", Context::class.java).invoke(null, context)
        (clazz.getMethod("notNeedWakeAod").invoke(instance) as? Boolean) == true ||
            (clazz.getMethod("isInCall").invoke(instance) as? Boolean) == true
    }.getOrDefault(false)

    private fun isAodShowing(context: Context): Boolean = runCatching {
        val clazz = Class.forName(AOD_DATA, false, context.classLoader)
        val instance = clazz.getMethod("getInstance", Context::class.java).invoke(null, context)
        clazz.getField("mAodIsInShow").getBoolean(instance)
    }.getOrDefault(false)

    private fun debugLog(message: String) {
        Log.i(TAG, "AUTO_WAKE: $message")
    }
}
*/
