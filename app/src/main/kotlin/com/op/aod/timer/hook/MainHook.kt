package com.op.aod.timer.hook

import android.content.Context
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.op.aod.timer.BuildConfig
import com.op.aod.timer.hook.BurnInProbe.hookBurnInProbe
import com.op.aod.timer.hook.ForceDeBurnInHook.hookForceDeBurnIn
import com.op.aod.timer.hook.KeepAodHook.hookKeepAodDuration

/**
 * 模块主入口：只向 SystemUI 进程分发"保持 AOD 显示"这一个 Hook。
 */
object MainHook : YukiBaseHooker() {

    /**
     * 当前宿主进程的 Application Context。
     *
     * 使用可重试获取而非 lazy 缓存，避免进程启动早期拿到 null 后被永久缓存。
     */
    val hostAppContext: Context?
        get() {
            _cachedContext?.let { return it }
            val ctx = fetchContext()
            if (ctx != null) _cachedContext = ctx
            return ctx
        }

    @Volatile
    private var _cachedContext: Context? = null

    private fun fetchContext(): Context? {
        return runCatching {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull()
    }

    override fun onHook() {
        loadApp(name = SYSTEM_UI) {
            runCatching { hookKeepAodDuration() }
            runCatching { hookForceDeBurnIn() }
            // 防烧屏探针只在 DEBUG 构建注册，release 不会执行
            if (BuildConfig.DEBUG) {
                runCatching { hookBurnInProbe() }
            }
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
}
