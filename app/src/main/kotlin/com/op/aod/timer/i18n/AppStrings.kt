package com.op.aod.timer.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/** 关于页的一个文本段落组。 */
data class AboutSectionData(
    val title: String,
    val lines: List<String>,
)

/** 关于页的一个可点击链接。 */
data class AboutLinkData(
    val title: String,
    val summary: String,
    val url: String,
)

/** 仓库地址统一放在这里，各语言共用。 */
object RepoLinks {
    const val ENHANCE = "https://github.com/Qjj7679/ColorOS_Aod_Enhance"

    /** TODO: 替换成你的仓库地址 */
    const val TIMER = "https://github.com/XiaoTDM/ColorOS-AOD-Timer"
}

/**
 * 全部界面文案。
 *
 * 新增语言：
 *   1. 新建一个 `XxxStrings.kt`，实现本接口；
 *   2. 在 [Languages.all] 里注册；
 *   3. （可选）在 `res/values-xx/strings.xml` 里补应用名。
 */
interface AppStrings {

    /** ISO 639-1 语言代码，如 "zh"、"en"。 */
    val languageCode: String

    // 通用
    val appTitle: String
    val aboutTitle: String
    val back: String
    val linksTitle: String

    // 主滑条
    val keepAodPrefix: String
    val followSystem: String
    val alwaysOn: String
    fun minutes(n: Int): String
    val sliderHint: String

    // 时段
    val scheduleTitle: String
    val scheduleSummary: String
    val startTime: String
    val endTime: String
    fun nextDay(time: String): String

    // 防烧屏位移
    val forceShiftTitle: String
    val forceShiftSummary: String

    // 关于入口
    val aboutEntrySummary: String

    // 时间选择器
    val pickerTitle: String
    val cancel: String
    val confirm: String

    // 版本
    fun version(name: String, code: Int): String

    // 关于页内容
    val aboutSections: List<AboutSectionData>
    val aboutLinks: List<AboutLinkData>
}

/** 当前语言；未提供时回退到简体中文。 */
val LocalStrings = staticCompositionLocalOf<AppStrings> { ZhCnStrings }
