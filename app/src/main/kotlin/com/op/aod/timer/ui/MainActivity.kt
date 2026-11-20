package com.op.aod.timer.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.op.aod.timer.data.TimerConfig
import com.op.aod.timer.data.TimerConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                TimerApp()
            }
        }
    }
}

/** 滑条上限（分钟）。达到上限表示"常亮"，对应配置值 -1。 */
private const val MAX_MINUTES = 240f

@Composable
private fun TimerApp() {
    val context = LocalContext.current
    var config by remember { mutableStateOf<TimerConfig?>(null) }

    // 读不到就一直重试，不拿默认值初始化 UI
    LaunchedEffect(Unit) {
        while (config == null) {
            config = withContext(Dispatchers.IO) { TimerConfigStore.read(context) }
            if (config == null) delay(500)
        }
    }

    val loaded = config
    if (loaded == null) {
        LoadingScreen()
    } else {
        TimerScreen(
            initial = loaded,
            onSave = { updated -> TimerConfigStore.write(context, updated) }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "AOD 常亮计时",
                color = MiuixTheme.colorScheme.secondaryContainer,
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text("正在读取配置…")
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun TimerScreen(
    initial: TimerConfig,
    onSave: (TimerConfig) -> Unit
) {
    // -1（常亮）映射到滑条最右端
    var value by remember {
        mutableFloatStateOf(
            if (initial.keepAodMinutes < 0) MAX_MINUTES
            else initial.keepAodMinutes.toFloat().coerceIn(0f, MAX_MINUTES)
        )
    }
    var forceDeburn by remember { mutableStateOf(initial.forceDeburnIn) }
    var scheduleEnabled by remember { mutableStateOf(initial.scheduleEnabled) }
    var scheduleStart by remember { mutableIntStateOf(initial.scheduleStartMinute) }
    var scheduleEnd by remember { mutableIntStateOf(initial.scheduleEndMinute) }

    val currentOnSave by rememberUpdatedState(onSave)
    val context = LocalContext.current

    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            current / 60,
            current % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            TimerConfig(
                keepAodMinutes = if (value >= MAX_MINUTES) -1 else value.toInt(),
                forceDeburnIn = forceDeburn,
                scheduleEnabled = scheduleEnabled,
                scheduleStartMinute = scheduleStart,
                scheduleEndMinute = scheduleEnd,
            )
        }
            .drop(1) // 跳过初始值
            .debounce(300)
            .distinctUntilChanged()
            .collect { currentOnSave(it) }
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

            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.background,
                )
            ) {
                SwitchPreference(
                    title = "启用时段",
                    summary = "只在设定的时间窗口内保持 AOD，其余时间交还系统",
                    checked = scheduleEnabled,
                    onCheckedChange = { scheduleEnabled = it },
                )
                ArrowPreference(
                    title = "开始时间",
                    summary = formatTime(scheduleStart),
                    onClick = { pickTime(scheduleStart) { scheduleStart = it } },
                )
                ArrowPreference(
                    title = "结束时间",
                    summary = formatTime(scheduleEnd),
                    onClick = { pickTime(scheduleEnd) { scheduleEnd = it } },
                )
            }

            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.background,
                )
            ) {
                SwitchPreference(
                    title = "强制启用防烧屏位移",
                    summary = "让系统在省电常亮时也执行全景位移；可能出现轻微位置跳变",
                    checked = forceDeburn,
                    onCheckedChange = { forceDeburn = it },
                )
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

private fun formatTime(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
