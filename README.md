# ColorOS AOD Timer

ColorOS 息屏显示 (AOD) 常亮计时模块 (LSPosed)。

在系统只提供"节能显示"的机型上，按设定时长保持 AOD；可选启用时段，
只在指定时间窗口内生效。

## 功能

- 一条滑条：`0` = 跟随系统，`1–239` = 熄屏后保持 N 分钟，`240` = 常亮
- 启用时段：只在设定的时间窗口内保持 AOD 点亮，支持跨午夜 (如 `22:00–08:00`)
- 一个开关：强制启用防烧屏位移 (系统在省电常亮时默认不做位移)
- 纯内存改写，不写 SecureSetting、不修改渲染路径、不修改系统设置
- 滑条归零、处于不生效时段或停用模块即完全恢复系统行为
- 对局部息屏同样生效；局部息屏自带位置偏移，无需开启强制位移
- 与 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance) 可共存 (包名、Provider、入口均独立)

## 原理

ColorOS 的 `AODDisplayUtil.getKeepAodOn()` 是 ROM 自带的"保持常亮"调试开关，被多处节能逻辑作为短路条件：

- `AodUpdateManager.isDisplayModeAllowUpdateClock()`
- `BaseAodClockLayoutController.onEnergySavingNotifyHide()`
- `BaseAodController.isUpsideDownEnable()`

模块在该方法返回后，于时间窗口内把结果改写为 `1`，窗口外还原为 `0`。

时段窗口在 Hook 内按"当天第几分钟"比较，跨午夜只是多一个 `||` 判断；
窗口外不做任何改写。

计时起点取自 `AodData.setAodIsInShow(true)`；亮屏（`PowerManager.isInteractive`）时清零。
息屏期间节能策略的反复隐藏/重现不会重置计时，只有重新熄屏才重新计时。

## 环境

- ColorOS (Oplus) 17 (仅在 17.0.1.200 测试通过，先前版本不保证可用性)
- LSPosed，作用域：`系统界面 (com.android.systemui)`
- minSdk 33

## 构建

- 带日志调试包

```bash
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

- 正式发布包

```bash
./gradlew :app:assembleRelease
```

需要在根目录提供 `keystore.properties` (`storeFile` / `storePassword` / `keyAlias` / `keyPassword`)，或在 `app/build.gradle` 中改用你自己的签名配置。

产物：`app/build/outputs/apk/release/app-release.apk`。

## 使用

1. 前往  [Releases](https://github.com/XiaoTDM/ColorOS-AOD-Timer/releases/latest) 下载并安装 app_release.apk，在 LSPosed 中启用「ColorOS AOD 计时修改」，作用域勾选系统界面 (Xposed 可能不兼容)
2. 重启系统界面
3. 打开 App，把滑条拨到需要的时长
4. 使用 app_debug.apk 时可验证日志：

```bash
adb logcat -s AOD_Timer:D
```

预期：

```
AOD_KEEP: session start
AOD_KEEP: getKeepAodOn -> 1 (minutes=60)
```

## 已知限制

- 面板能力码未上报"全天"位时，ROM 本身不提供防烧屏保证；长期常亮请注意烧屏风险 (1.0.1 已提供强制开启防烧屏偏移的功能)
- 与其它改写 `getKeepAodOn()` 的模块同时启用会互相覆盖，请只保留一个
- 到达设定时长后由 ROM 的下一次节能判定执行隐藏，可能有数秒到数十秒延迟
- 时段窗口不会主动点亮 AOD：若窗口开始时 AOD 已被隐藏，需要手动点亮一次
- v1.1 起配置通过 `YukiHookPrefsBridge` 直接读取，重启后无需再打开一次 App
- 自动唤醒功能已在 v1.1.1 移除（遮挡场景会被误点亮，弊大于利）

## 声明

- 项目启发自 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)
- AI 使用

## 许可证

MIT。配置通道与 Hook 结构参考自 Qjj7679 的 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)，
原项目版权声明已保留在 [LICENSE](LICENSE) 中。
