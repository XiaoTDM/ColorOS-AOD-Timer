package com.op.aod.timer.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.op.aod.timer.i18n.AppStrings
import com.op.aod.timer.i18n.Languages
import com.op.aod.timer.i18n.LocalStrings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
            val strings = remember { Languages.resolve() }
            CompositionLocalProvider(LocalStrings provides strings) {
                MiuixTheme(ThemeController(ColorSchemeMode.System)) {
                    TimerApp()
                }
            }
        }
    }
}

/** 滑条上限（分钟）。达到上限表示"常亮"，对应配置值 -1。 */
private const val MAX_MINUTES = 240f

@Composable
private fun TimerApp() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val initialConfig = remember { TimerConfigStore.read(context) }
    var showAbout by remember { mutableStateOf(false) }

    BackHandler(enabled = showAbout) { showAbout = false }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = if (showAbout) strings.aboutTitle else strings.appTitle,
                color = MiuixTheme.colorScheme.secondaryContainer,
                navigationIcon = {
                    if (showAbout) {
                        TextButton(strings.back, onClick = { showAbout = false })
                    }
                },
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues ->
        if (showAbout) {
            AboutScreen(paddingValues)
        } else {
            SettingsScreen(
                initial = initialConfig,
                paddingValues = paddingValues,
                onOpenAbout = { showAbout = true },
                onSave = { updated -> TimerConfigStore.write(context, updated) },
            )
        }
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
    val strings = LocalStrings.current

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
            .drop(1)
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
                Text(strings.keepAodPrefix + durationLabel(value, strings))
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
                    text = strings.sliderHint,
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
                title = strings.scheduleTitle,
                summary = strings.scheduleSummary,
                checked = scheduleEnabled,
                onCheckedChange = { scheduleEnabled = it },
            )
            ArrowPreference(
                title = strings.startTime,
                summary = formatTime(scheduleStart),
                onClick = { editingStart = true },
            )
            ArrowPreference(
                title = strings.endTime,
                summary = endTimeLabel(scheduleStart, scheduleEnd, strings),
                onClick = { editingEnd = true },
            )
        }

        Card(
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.background,
            )
        ) {
            SwitchPreference(
                title = strings.forceShiftTitle,
                summary = strings.forceShiftSummary,
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
                title = strings.aboutTitle,
                summary = strings.aboutEntrySummary,
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
    val strings = LocalStrings.current
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
                Text(strings.pickerTitle)

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
                    TextButton(strings.cancel, onDismiss)
                    TextButton(strings.confirm, onClick = { onConfirm(hour * 60 + minute) })
                }
            }
        }
    }
}

private fun durationLabel(value: Float, strings: AppStrings): String = when {
    value <= 0f -> strings.followSystem
    value >= MAX_MINUTES -> strings.alwaysOn
    else -> strings.minutes(value.toInt())
}

private fun formatTime(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

/** 结束时间早于开始时间时标注"次日"，避免被误认为填反。 */
private fun endTimeLabel(startMinute: Int, endMinute: Int, strings: AppStrings): String {
    val text = formatTime(endMinute)
    return if (endMinute < startMinute) strings.nextDay(text) else text
}
