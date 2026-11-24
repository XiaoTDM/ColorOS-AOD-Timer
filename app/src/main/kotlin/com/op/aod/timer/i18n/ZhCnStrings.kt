package com.op.aod.timer.i18n

/** 简体中文（默认语言）。 */
object ZhCnStrings : AppStrings {

    override val languageCode = "zh"

    override val appTitle = "ColorOS AOD 计时修改"
    override val aboutTitle = "关于"
    override val back = "返回"
    override val linksTitle = "链接"

    override val keepAodPrefix = "熄屏后保持 AOD 显示："
    override val followSystem = "跟随系统"
    override val alwaysOn = "常亮"
    override fun minutes(n: Int) = "$n 分钟"
    override val sliderHint = "0 = 跟随系统；240 = 常亮。只影响节能模式下的显示时长，不修改系统设置"

    override val scheduleTitle = "启用时段"
    override val scheduleSummary = "只在设定的时间窗口内保持 AOD，其余时间交还系统控制"
    override val startTime = "开始时间"
    override val endTime = "结束时间"
    override fun nextDay(time: String) = "次日 $time"

    override val forceShiftTitle = "强制启用防烧屏位移"
    override val forceShiftSummary = "让系统在省电常亮时也执行全景位移；可能出现轻微位置跳变"

    override val autoWakeTitle = "时段开始时自动唤醒"
    override val autoWakeSummary = "到点尝试点亮 AOD；被遮挡时按下方时长重试（实验性）"
    override val retryPrefix = "遮挡后重试："
    override val retryOff = "不重试"
    override fun retryMinutes(n: Int) = "$n 分钟"
    override val retryIntervalPrefix = "重试间隔："
    override fun interval(seconds: Int) =
        if (seconds < 60) "$seconds 秒" else "${seconds / 60} 分钟"
    override val fallbackTitle = "镜像失效时自采样接近传感器"
    override val fallbackSummary = "默认关闭；开启后镜像不可用时会主动采样，可能增加误触发概率"

    override val aboutEntrySummary = "版本、原理与致谢"

    override val pickerTitle = "选择时间"
    override val cancel = "取消"
    override val confirm = "确定"

    override fun version(name: String, code: Int) = "版本 $name（$code）"

    override val aboutSections = listOf(
        AboutSectionData(
            title = "重要提示",
            lines = listOf(
                "此模块理论上适用于 ColorOS 17 及 ColorOS 16 16.0.5 以后的版本；",
                "如果后续系统更新导致调试接口失效，模块可能无法工作。",
            ),
        ),
        AboutSectionData(
            title = "它做什么",
            lines = listOf(
                "此模块为部分仅支持 AOD 节能显示的机型通过延迟 AOD 关闭从而模拟全天候显示模式。",
                "可选模块生效时间段及是否强制启用防烧屏偏移。",
            ),
        ),
        AboutSectionData(
            title = "实现方式",
            lines = listOf(
                "通过改写 AODDisplayUtil.getKeepAodOn() 的返回值，使系统保持 AOD 点亮。",
                "此功能不涉及任何渲染修改，同时不会更改系统设置，关闭开关或禁用模块即恢复系统逻辑。",
            ),
        ),
        AboutSectionData(
            title = "致谢",
            lines = listOf(
                "原作者: ColorOS AOD Enhance (@Qjj7679)",
                "此项目参考其 Hook 结构制作，与原模块独立。",
                "使用 UI 组件 Miuix；Hook 框架 YukiHookAPI & LSPosed。",
            ),
        ),
        AboutSectionData(
            title = "许可",
            lines = listOf("MIT License"),
        ),
    )

    override val aboutLinks = listOf(
        AboutLinkData(
            title = "ColorOS AOD Enhance",
            summary = "上游项目（@Qjj7679）",
            url = RepoLinks.ENHANCE,
        ),
        AboutLinkData(
            title = "ColorOS AOD Timer",
            summary = "本模块源码",
            url = RepoLinks.TIMER,
        ),
    )
}
