package com.op.aod.timer.i18n

/** English. */
object EnStrings : AppStrings {

    override val languageCode = "en"

    override val appTitle = "ColorOS AOD Timer"
    override val aboutTitle = "About"
    override val back = "Back"
    override val linksTitle = "Links"

    override val keepAodPrefix = "Keep AOD on for: "
    override val followSystem = "System default"
    override val alwaysOn = "Always on"
    override fun minutes(n: Int) = "$n min"
    override val sliderHint =
        "0 = follow system, 240 = always on. Only affects the energy-saving display duration; system settings are untouched."

    override val scheduleTitle = "Schedule"
    override val scheduleSummary =
        "Keep AOD only within the configured window; hand back to the system otherwise"
    override val startTime = "Start time"
    override val endTime = "End time"
    override fun nextDay(time: String) = "Next day $time"

    override val forceShiftTitle = "Force burn-in shift"
    override val forceShiftSummary =
        "Let the system shift the AOD even in energy-saving mode; slight position jumps may occur"

    override val aboutEntrySummary = "Version, rationale and credits"

    override val pickerTitle = "Select time"
    override val cancel = "Cancel"
    override val confirm = "OK"

    override fun version(name: String, code: Int) = "Version $name ($code)"

    override val aboutSections = listOf(
        AboutSectionData(
            title = "Important",
            lines = listOf(
                "This module targets ColorOS 17 and ColorOS 16 (16.0.5 or later).",
                "If a future system update removes the debug interface used here, the module may stop working.",
            ),
        ),
        AboutSectionData(
            title = "What it does",
            lines = listOf(
                "On devices limited to energy-saving AOD, it delays AOD shutdown to emulate an always-on display.",
                "An optional schedule and an optional forced burn-in shift are available.",
            ),
        ),
        AboutSectionData(
            title = "How it works",
            lines = listOf(
                "It overrides the return value of AODDisplayUtil.getKeepAodOn() so the system keeps the AOD lit.",
                "No rendering is touched and no system setting is written; disabling the module restores stock behaviour.",
            ),
        ),
        AboutSectionData(
            title = "Credits",
            lines = listOf(
                "Original author: ColorOS AOD Enhance (@Qjj7679)",
                "This project borrows its hook structure and is independent of the original module.",
                "UI: Miuix. Hook framework: YukiHookAPI & LSPosed.",
            ),
        ),
        AboutSectionData(
            title = "License",
            lines = listOf("MIT License"),
        ),
    )

    override val aboutLinks = listOf(
        AboutLinkData(
            title = "ColorOS AOD Enhance",
            summary = "Upstream project (@Qjj7679)",
            url = RepoLinks.ENHANCE,
        ),
        AboutLinkData(
            title = "ColorOS AOD Timer",
            summary = "Source code of this module",
            url = RepoLinks.TIMER,
        ),
    )
}
