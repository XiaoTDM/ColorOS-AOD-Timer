package com.op.aod.timer.i18n

import java.util.Locale

/**
 * 语言注册表。
 *
 * 新增语言只需：
 *   1. 新建 `XxxStrings.kt` 实现 [AppStrings]；
 *   2. 把它加入 [all]；
 *   3. （可选）在 `res/values-xx/strings.xml` 里补应用名。
 */
object Languages {

    private val all: List<AppStrings> = listOf(
        ZhCnStrings,
        EnStrings,
    )

    /** 按系统语言选择；没有匹配时回退到简体中文。 */
    fun resolve(locale: Locale = Locale.getDefault()): AppStrings {
        val language = locale.language.lowercase(Locale.ROOT)
        return all.firstOrNull { it.languageCode.equals(language, ignoreCase = true) }
            ?: ZhCnStrings
    }
}
