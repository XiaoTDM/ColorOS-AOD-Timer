# ColorOS AOD Timer

ColorOS Always-On Display (AOD) Force Light Timer Module (Xposed / LSPosed).

Just one thing: When the system only allows 'Energy-saving display,' keep the AOD on for the set duration.

## Functions

- A slider: `0` = follow the system, `1–239` = keep the screen on for N minutes after turning off, `240` = always on
- A switch: force-enable burn-in prevention offset (the system doesn’t make offsets by default when in AOD Energy-saving Mode)
- Purely memory-based rewrite, does not touch SecureSettings, rendering paths, or system settings
- Resetting the slider to zero or disabling the module fully restores system behavior
- Can coexist with [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance) (package name, provider, and entry are all independent)

## 原理
## Principle

ColorOS's `AODDisplayUtil.getKeepAodOn()` is the ROM's built-in 'keep always-on' debug switch and is used as a short-circuit condition in multiple Energy-saving logics:

- `AodUpdateManager.isDisplayModeAllowUpdateClock()`
- `BaseAodClockLayoutController.onEnergySavingNotifyHide()`
- `BaseAodController.isUpsideDownEnable()`

After the module returns from this method, it rewrites the result to `1` within a time window, and restores it to `0` outside the window.

The timing starts from `AodData.setAodIsInShow(true)`; it resets when the screen is on (`PowerManager.isInteractive`). Repeated hiding/appearing during screen-off energy-saving does not reset the timer — only turning off the screen again restarts it.

## Environment

- ColorOS (Oplus) ROM
- LSPosed, Scope: `System UI（com.android.systemui）`
- minSdk 33

## Build

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`。

For a Release build, you need to provide `keystore.properties` (`storeFile` / `storePassword` / `keyAlias` / `keyPassword`) in the root directory, or use your own signing configuration in `app/build.gradle`.

## Usage

1. Install the APK, enable "ColorOS AOD 计时修改" in LSPosed, and check "System UI" as scope.
2. Restart System UI.
3. Open the app and move the slider to the desired duration.
4. Check the logs:

```bash
adb logcat -s AOD_Timer:D
```

Expected:

```
AOD_KEEP: session start
AOD_KEEP: getKeepAodOn -> 1 (minutes=60)
```

## Known Limitations

- If the panel capability code hasn't reported the "all-day" flag, the ROM itself doesn't guarantee burn-in prevention; keep in mind the risk of screen burn if always-on for a long time.
- Enabling this alongside other modules that override `getKeepAodOn()` will cause conflicts, so only keep one.
- Once the set duration is reached, hiding is controlled by the ROM's next Energy-saving check, which may have a delay of a few to several seconds.
- Configurations are managed by the LSPosed framework, and may be lost after a reboot.

## Disclaimer

- Project inspired by [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)
- AI used

## License

MIT. The configuration channels and hook structure are based on Qjj7679's [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance),
with the original project's copyright notice retained in [LICENSE](LICENSE).
