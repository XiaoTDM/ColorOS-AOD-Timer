# ColorOS AOD Timer

ColorOS Always-On Display (AOD) timer module (Xposed / LSPosed).

On devices limited to "energy-saving display", it keeps the AOD on for a
configurable duration, optionally only within a schedule window.
The UI follows the system language.

## Features

- One slider: `0` = follow the system, `1–239` = keep AOD for N minutes after screen-off, `240` = always on
- Schedule: keep AOD only within a time window, overnight windows supported (default `08:00–23:00`)
- One switch: force-enable the burn-in shift (the ROM skips it in energy-saving mode)
- Pure in-memory override: no SecureSetting writes, no rendering changes, no system settings touched
- Setting the slider to 0, disabling the schedule, or disabling the module restores stock behaviour
- Also works for partial AOD; partial AOD already shifts its position on its own
- Localization: Simplified Chinese / English, following the system language
- Coexists with [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance) (separate package, config and entry point)

## Principle

ColorOS's `AODDisplayUtil.getKeepAodOn()` is the ROM's built-in "keep always-on"
debug switch, used as a short-circuit condition in several energy-saving paths:

- `AodUpdateManager.isDisplayModeAllowUpdateClock()`
- `BaseAodClockLayoutController.onEnergySavingNotifyHide()`
- `BaseAodController.isUpsideDownEnable()`

After the method returns, the module rewrites the result to `1` inside the
schedule window and back to `0` outside it.

The window is compared as "minute of day"; crossing midnight is just one extra
`||`. Nothing is rewritten outside the window.

Timing starts at `AodData.setAodIsInShow(true)` and resets when the screen is
interactive. Repeated hiding during screen-off does not reset the timer.

## Configuration storage (since v1.2)

Older versions used LSPosed's **New XSharedPreferences**, which is scheduled for
removal in **LSPosed 2.3.0** and is unavailable while the module app process is
dead. Since v1.2:

| Role | Location |
|---|---|
| Written by the module app | `/data/user_de/0/com.op.aod.timer/files/timer_config.json` |
| Read by SystemUI | `content://com.op.aod.timer.config/timer_config` (read-only provider) |
| Cached by SystemUI | `/data/user_de/0/com.android.systemui/files/aod_timer_config_cache.json` |

Notes:

- Config lives in **device-encrypted** storage, readable before the first unlock;
- The provider is read-only and restricted to SystemUI's uid/package;
- SystemUI refreshes its own cache after every successful read, so the module
  keeps working when the app process is frozen or killed;
- Writes use a temp-file + rename to avoid truncated JSON.

> **Upgrade note**: the old configuration is not migrated. After upgrading to
> v1.2, set the value once again.

## Environment

- ColorOS (Oplus) ROM
- LSPosed, scope: `System UI (com.android.systemui)`
- minSdk 33

## Build

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

For a release build, provide `keystore.properties` in the repository root
(`storeFile` / `storePassword` / `keyAlias` / `keyPassword`), or configure your
own signing in `app/build.gradle`.

## Usage

1. Install the APK, enable "ColorOS AOD Timer" in LSPosed, scope: System UI.
2. Restart System UI.
3. Open the app and move the slider to the desired duration.
4. Check the logs:

```bash
adb logcat -s AOD_Timer:V
```

Expected:

```
AOD_Timer: config loaded: keep=60, schedule=false 480-1380, forceShift=false
AOD_Timer: AOD_KEEP: minutes=60, keep=true
```

With the module app not running:

```
AOD_Timer: config from disk cache: keep=60
```

## Localization

All UI strings are template-based and follow the system language (falling back
to Simplified Chinese).

Adding a language takes three steps:

1. Copy `i18n/EnStrings.kt` to `i18n/XxxStrings.kt` and implement `AppStrings`;
2. Register it in the `Languages.all` list;
3. (Optional) add `res/values-xx/strings.xml` with `app_name` and `xposed_description`.

No layout code needs to change — everything is read via `LocalStrings.current`.

## Known limitations

- If the panel capability code doesn't report the "all-day" flag, the ROM offers
  no burn-in guarantee; long always-on sessions carry burn-in risk
- Enabling it together with another module that overrides `getKeepAodOn()` makes
  them fight each other; keep only one
- After the configured duration, hiding happens on the ROM's next energy-saving
  check and may take seconds
- The schedule window does not light the AOD up: if the AOD was already hidden
  when the window starts, one manual tap is required
- After installation the app must be opened once and a value saved, so SystemUI
  has a cache to fall back on
- Auto-wake is intentionally not implemented: the wake entry has no occlusion
  check and would light the screen up while covered

## Disclaimer

- Inspired by [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)
- AI-assisted

## License

MIT. The configuration channel and hook structure are based on Qjj7679's
[ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance); the
original copyright notice is retained in [LICENSE](LICENSE).
