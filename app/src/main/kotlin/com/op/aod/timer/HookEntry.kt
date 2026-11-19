package com.op.aod.timer

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.op.aod.timer.hook.MainHook

/**
 * AOD Timer —— 独立模块入口。
 *
 * 只做一件事：在节能模式下按用户配置的时长保持 AOD 显示。
 * 不涉及亮度、全景能力、唤醒行为等任何其他功能。
 */
@InjectYukiHookWithXposed(
    sourcePath = "src/main",
    modulePackageName = "com.op.aod.timer",
    entryClassName = "HookEntryXposed"
)
object HookEntry : IYukiHookXposedInit {

    override fun onInit() {
        YukiHookAPI.configs {
            debugLog {
                tag = "AOD_Timer"
                isEnable = BuildConfig.DEBUG
                isRecord = false
                elements(TAG, PRIORITY)
            }
            isDebug = BuildConfig.DEBUG
        }
    }

    override fun onHook() = encase(MainHook)
}
