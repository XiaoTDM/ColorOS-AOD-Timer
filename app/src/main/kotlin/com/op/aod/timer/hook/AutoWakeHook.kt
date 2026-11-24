package com.op.aod.timer.hook

// 实验性：时段开始时的自动唤醒。
// notifyWakeUpCallback(0) 自身没有遮挡判断，这里通过镜像 ROM 手势传感器事件补上，
// 拿不到事件时退回主动采样；ROM 改动类结构时镜像会失效并自动降级。

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.timer.data.TimerConfigContract
import com.op.aod.timer.util.Logs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

internal object AutoWakeHook {

    private const val TAG = "AOD_Timer"

    private const val WAKE_CONTROLLER = "com.oplus.systemui.aod.display.OplusWakeUpController"
    private const val AOD_DATA = "com.oplus.systemui.aod.aodclock.constant.AodData"

    /** OPPO 自定义手势传感器类型。 */
    private const val OPLUS_GESTURE_SENSOR = 33171066

    private const val GESTURE_SENSOR_LISTENER =
        "com.oplus.systemui.keyguard.gesture.OplusBlackScreenGestureControllExImpl\$sensorListener\$1"

    @Volatile
    private var mirroredNear: Boolean? = null

    /** 镜像状态的时间戳，用于判断是否过期。 */
    @Volatile
    private var mirroredAt = 0L

    private const val MIRROR_MAX_AGE_MS = 60_000L

    /** 接近状态变化触发器：传感器回调 → 工作线程。 */
    private val triggerLock = ReentrantLock()
    private val triggerCondition = triggerLock.newCondition()

    @Volatile
    private var pendingTrigger = false

    fun YukiBaseHooker.hookAutoWake() {
        hookProximityMirror()

        thread(name = "aod-timer-autowake", isDaemon = true) {
            var wasActive = false
            var windowStartElapsed = 0L
            var lastAttemptElapsed = 0L
            var settledThisWindow = false

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

                    val now = SystemClock.elapsedRealtime()

                    if (inWindow && !wasActive) {
                        windowStartElapsed = now
                        lastAttemptElapsed = 0L
                        settledThisWindow = false
                        Logs.i("AUTO_WAKE: entered window")
                    }
                    wasActive = inWindow

                    if (!inWindow) {
                        // 时段外：低频复查配置与时段
                        awaitProximityChange(60_000L)
                        continue
                    }

                    if (settledThisWindow) {
                        // 本时段已确定不需要再试（已亮/亮屏/ROM 拒绝）
                        awaitProximityChange(60_000L)
                        continue
                    }

                    val retryWindowMs = config.wakeRetryMinutes.coerceAtLeast(0) * 60_000L
                    val withinRetry =
                        retryWindowMs > 0L && now - windowStartElapsed <= retryWindowMs
                    if (!withinRetry) {
                        // 超过重试总时长：本时段不再尝试，等时段结束
                        awaitProximityChange(60_000L)
                        continue
                    }

                    if (lastAttemptElapsed == 0L) {
                        // 进入时段后的首次尝试
                        lastAttemptElapsed = now
                        settledThisWindow = wake(context)
                        continue
                    }

                    // near 状态变化会立刻唤醒本线程；超时则按配置间隔兜底
                    val intervalSeconds = config.wakeRetryIntervalSeconds.coerceIn(
                        TimerConfigContract.MIN_WAKE_RETRY_INTERVAL_SECONDS,
                        TimerConfigContract.MAX_WAKE_RETRY_INTERVAL_SECONDS,
                    )
                    val triggered = awaitProximityChange(intervalSeconds * 1_000L)
                    lastAttemptElapsed = SystemClock.elapsedRealtime()
                    if (triggered) debugLog("proximity changed, re-check")
                    settledThisWindow = wake(context)
                } catch (_: Throwable) {
                    Thread.sleep(5_000)
                }
            }
        }
    }

    /** 等待接近状态变化或超时，返回 true 表示由变化事件唤醒。 */
    private fun awaitProximityChange(timeoutMs: Long): Boolean {
        triggerLock.lock()
        try {
            if (pendingTrigger) {
                pendingTrigger = false
                return true
            }
            triggerCondition.await(timeoutMs, TimeUnit.MILLISECONDS)
            val fired = pendingTrigger
            pendingTrigger = false
            return fired
        } finally {
            triggerLock.unlock()
        }
    }

    private fun signalProximityChange() {
        triggerLock.lock()
        try {
            pendingTrigger = true
            triggerCondition.signalAll()
        } finally {
            triggerLock.unlock()
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
                        // 标准接近传感器，或 OPPO 的手势传感器
                        if (sensor.type != Sensor.TYPE_PROXIMITY && sensor.type != OPLUS_GESTURE_SENSOR) {
                            return@after
                        }
                        val value = event.values.firstOrNull() ?: return@after
                        val near = if (sensor.type == Sensor.TYPE_PROXIMITY) {
                            value < sensor.maximumRange
                        } else {
                            value == 0.0f
                        }
                        if (mirroredNear != near) {
                            mirroredNear = near
                            mirroredAt = SystemClock.elapsedRealtime()
                            debugLog("proximity mirror: near=$near (type=${sensor.type}, value=$value)")
                            // near 发生变化：立刻叫醒工作线程重新判断
                            signalProximityChange()
                        } else {
                            mirroredAt = SystemClock.elapsedRealtime()
                        }
                    }
                }
            debugLog("proximity mirror installed")
        }.onFailure {
            debugLog("proximity mirror hook failed: ${it.message}")
            Logs.w("AUTO_WAKE: proximity mirror hook failed: ${it.message}")
        }
    }

    /**
     * 尝试唤醒一次。
     *
     * @return true 表示本次时段不需要再试（已亮/亮屏/ROM 拒绝）；
     *         false 表示被遮挡跳过，稍后应重试。
     */
    private fun wake(context: Context): Boolean {
        if (isInteractive(context)) {
            debugLog("skip: screen on")
            return true
        }
        if (isAodShowing(context)) {
            debugLog("skip: AOD already showing")
            return true
        }
        val config = TimerConfigReader.read(context)
        if (isCovered(context, config.wakeFallbackProximity)) {
            debugLog("skip: proximity covered, will retry")
            return false
        }
        if (notNeedWakeAod(context)) {
            debugLog("skip: notNeedWakeAod")
            return true
        }

        debugLog("calling notifyWakeUpCallback(0)")
        Logs.i("AUTO_WAKE: calling notifyWakeUpCallback(0)")
        return runCatching {
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
            Logs.i("AUTO_WAKE: notifyWakeUpCallback(0) invoked")
        }.onFailure {
            debugLog("notifyWakeUpCallback failed: ${it.message}")
            Logs.w("AUTO_WAKE: notifyWakeUpCallback failed: ${it.message}")
        }.isSuccess
    }

    private fun isCovered(context: Context, allowFallback: Boolean): Boolean {
        val mirrored = mirroredNear
        if (mirrored != null &&
            SystemClock.elapsedRealtime() - mirroredAt < MIRROR_MAX_AGE_MS
        ) {
            return mirrored
        }

        if (!allowFallback) {
            // 默认策略：拿不到遮挡状态时不冒险点亮，按"遮挡"处理
            debugLog("proximity unknown, fallback disabled -> assume covered")
            return true
        }

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
        Logs.d("AUTO_WAKE: $message")
    }
}
