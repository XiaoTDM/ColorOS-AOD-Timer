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
import com.op.aod.timer.i18n.AboutSectionData
import com.op.aod.timer.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于页：内容来自 [com.op.aod.timer.i18n.AppStrings]，布局与语言无关。
 */
@Composable
fun AboutScreen(paddingValues: PaddingValues) {
    val strings = LocalStrings.current
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
                Text(strings.appTitle)
                Text(strings.version(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            }
        }

        strings.aboutSections.forEach { section ->
            Section(section)
        }

        if (strings.aboutLinks.isNotEmpty()) {
            Text(
                text = strings.linksTitle,
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
                strings.aboutLinks.forEach { link ->
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
}

@Composable
private fun Section(section: AboutSectionData) {
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
