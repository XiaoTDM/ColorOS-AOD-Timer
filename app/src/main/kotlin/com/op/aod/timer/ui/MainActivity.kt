package com.op.aod.timer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.op.aod.timer.data.TimerConfigStore
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initial = TimerConfigStore.read(contentResolver)

        setContent {
            MiuixTheme {
                TimerScreen(
                    initialMinutes = initial,
                    onSave = { minutes -> TimerConfigStore.write(contentResolver, minutes) }
                )
            }
        }
    }
}

/** 滑条上限（分钟）。达到上限表示"常亮"，对应配置值 -1。 */
private const val MAX_MINUTES = 240f

@OptIn(FlowPreview::class)
@Composable
private fun TimerScreen(
    initialMinutes: Int,
    onSave: (Int) -> Unit
) {
    // -1（常亮）映射到滑条最右端
    var value by remember {
        mutableFloatStateOf(
            if (initialMinutes < 0) MAX_MINUTES
            else initialMinutes.toFloat().coerceIn(0f, MAX_MINUTES)
        )
    }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        snapshotFlow { value }
            .drop(1) // 跳过初始值
            .debounce(300)
            .distinctUntilChanged()
            .collect { currentOnSave(if (it >= MAX_MINUTES) -1 else it.toInt()) }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "AOD 常亮计时",
                color = MiuixTheme.colorScheme.secondaryContainer,
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.background,
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("熄屏后保持 AOD 显示：${durationLabel(value)}")
                    Slider(
                        value = value,
                        onValueChange = { value = it.coerceIn(0f, MAX_MINUTES) },
                        valueRange = 0f..MAX_MINUTES,
                        steps = 239,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Text("0 = 跟随系统；240 = 常亮。只影响节能模式下的显示时长，不改系统设置。")
        }
    }
}

private fun durationLabel(value: Float): String = when {
    value <= 0f -> "跟随系统"
    value >= MAX_MINUTES -> "常亮"
    else -> "${value.toInt()} 分钟"
}
