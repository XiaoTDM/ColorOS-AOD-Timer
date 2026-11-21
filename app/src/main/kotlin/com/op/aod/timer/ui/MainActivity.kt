package com.op.aod.timer.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
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
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            MiuixTheme(ThemeController(ColorSchemeMode.System)) {
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
    var showAbout by remember { mutableStateOf(false) }

    // 读不到就一直重试，不拿默认值初始化 UI
    LaunchedEffect(Unit) {
        while (config == null) {
            config = withContext(Dispatchers.IO) { TimerConfigStore.read(context) }
            if (config == null) delay(500)
        }
    }

    BackHandler(enabled = showAbout) { showAbout = false }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = if (showAbout) "关于" else "ColorOS AOD 计时修改",
                color = MiuixTheme.colorScheme.secondaryContainer,
                navigationIcon = {
                    if (showAbout) {
                        TextButton("返回", onClick = { showAbout = false })
                    }
                },
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues ->
        when {
            showAbout -> AboutScreen(paddingValues)
            config == null -> LoadingContent(paddingValues)
            else -> SettingsScreen(
                initial = config!!,
                paddingValues = paddingValues,
                onOpenAbout = { showAbout = true },
                onSave = { updated -> TimerConfigStore.write(context, updated) },
            )
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text("正在读取配置…")
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun SettingsScreen(
    initial: TimerConfig,
    paddingValues: PaddingValues,
    onOpenAbout: () -> Unit,
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
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    val currentOnSave by rememberUpdatedState(onSave)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("熄屏后保持 AOD 显示：${durationLabel(value)}")
                Slider(
                    value = value,
                    onValueChange = { value = it.coerceIn(0f, MAX_MINUTES) },
                    valueRange = 0f..MAX_MINUTES,
                    steps = 239,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Text(
                    text = "0 = 跟随系统；240 = 常亮。只影响节能模式下的显示时长，不改系统设置。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
                onClick = { editingStart = true },
            )
            ArrowPreference(
                title = "结束时间",
                summary = formatEndTime(scheduleStart, scheduleEnd),
                onClick = { editingEnd = true },
            )
            // TODO: 自动唤醒已废弃（遮挡场景会被误点亮），此处保留位置，
            //       若将来改为复用 ROM 定时唤醒链再恢复。
            // SwitchPreference(
            //     title = "时段开始时自动唤醒",
            //     summary = "到点后尝试主动点亮 AOD（实验性，可能无效）",
            //     checked = autoWake,
            //     onCheckedChange = { autoWake = it },
            // )
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

        Card(
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            ArrowPreference(
                title = "关于",
                summary = "版本、原理与致谢",
                onClick = onOpenAbout,
            )
        }
    }

    if (editingStart) {
        TimePickerDialogMiuix(
            initialMinute = scheduleStart,
            onConfirm = {
                scheduleStart = it
                editingStart = false
            },
            onDismiss = { editingStart = false },
        )
    }

    if (editingEnd) {
        TimePickerDialogMiuix(
            initialMinute = scheduleEnd,
            onConfirm = {
                scheduleEnd = it
                editingEnd = false
            },
            onDismiss = { editingEnd = false },
        )
    }
}

/** Miuix 风格的时分选择器，替代系统原生 TimePickerDialog。 */
@Composable
private fun TimePickerDialogMiuix(
    initialMinute: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialMinute / 60) }
    var minute by remember { mutableIntStateOf(initialMinute % 60) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("选择时间")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NumberPicker(
                        hour,
                        { hour = it },
                        Modifier
                            .width(72.dp)
                            .height(240.dp),
                        true,
                        0..23,
                        { "%02d".format(it) },
                        5,
                    )
                    Text(":")
                    NumberPicker(
                        minute,
                        { minute = it },
                        Modifier
                            .width(72.dp)
                            .height(240.dp),
                        true,
                        0..59,
                        { "%02d".format(it) },
                        5,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton("取消", onDismiss)
                    TextButton("确定", onClick = { onConfirm(hour * 60 + minute) })
                }
            }
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

/** 结束时间早于开始时间时标注"次日"，避免被误认为填反。 */
private fun formatEndTime(startMinute: Int, endMinute: Int): String {
    val text = formatTime(endMinute)
    return if (endMinute < startMinute) "次日 $text" else text
}
