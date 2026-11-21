package com.op.aod.timer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.op.aod.timer.BuildConfig
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ============================================================
// 关于页内容模板
// 直接修改下面的 aboutSections 即可，页面布局会自动渲染。
// 每个 section 渲染为「标题 + 一张全宽卡片」，lines 里每项一行。
// ============================================================

private data class AboutSection(
    val title: String,
    val lines: List<String>,
)

private data class AboutLink(
    val title: String,
    val summary: String,
    val url: String,
)

private const val ENHANCE_REPO_URL = "https://github.com/Qjj7679/ColorOS_Aod_Enhance"

/** TODO: 替换成你的仓库地址 */
private const val TIMER_REPO_URL = "https://github.com/XiaoTDM/ColorOS-AOD-Timer"

private val aboutLinks = listOf(
    AboutLink(
        title = "ColorOS AOD Enhance",
        summary = "上游项目 (@Qjj7679)",
        url = ENHANCE_REPO_URL,
    ),
    AboutLink(
        title = "ColorOS AOD Timer",
        summary = "本模块仓库",
        url = TIMER_REPO_URL,
    ),
)


private val aboutSections = listOf(
    AboutSection(
        title = "重要提示", // TODO: 按需修改
        lines = listOf(
            "此模块理论上适用于 ColorOS 17 及 ColorOS 16 16.0.5 以后的版本；",
            "如果后续系统更新导致调试接口失效，模块可能无法工作。",
        ),
    ),
    AboutSection(
        title = "它做什么", // TODO: 按需修改
        lines = listOf(
            "此模块为部分仅支持 AOD 节能显示的机型通过延迟 AOD 关闭从而模拟全天候显示模式。",
            "可选模块生效时间段及是否强制启用防烧屏偏移。"
        ),
    ),
    AboutSection(
        title = "实现方式", // TODO: 按需修改
        lines = listOf(
            "通过改写 AODDisplayUtil.getKeepAodOn() 的返回值，使系统保持 AOD 点亮。",
            "此功能不涉及任何渲染修改，同时不会更改系统设置，关闭开关或禁用模块即恢复系统逻辑。",
        ),
    ),
    AboutSection(
        title = "致谢", // TODO: 按需修改
        lines = listOf(
            "原作者: ColorOS AOD Enhance (@Qjj7679)",
            "参考其 Hook 结构制作，与原项目独立。",
            "UI 组件: MiuixHook",
            "Hook 框架 YukiHookAPI & LSPosed",
        ),
    ),
    AboutSection(
        title = "许可", // TODO: 按需修改
        lines = listOf(
            "MIT License",
        ),
    ),
)

@Composable
fun AboutScreen(paddingValues: PaddingValues) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("ColorOS AOD 计时修改")
                Text("版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }
        }

        aboutSections.forEach { section ->
            Text(
                text = section.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.background,
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    section.lines.forEach { line ->
                        Text(
                            text = line,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Text(
            text = "链接",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            aboutLinks.forEach { link ->
                ArrowPreference(
                    title = link.title,
                    summary = link.summary,
                    onClick = {
                        // 没有浏览器等情况下不要让应用崩溃
                        runCatching { uriHandler.openUri(link.url) }
                    },
                )
            }
        }
    }
}
