# ColorOS AOD Timer

ColorOS 息屏显示（AOD）常亮计时模块（Xposed / LSPosed）。

只做一件事：当系统只允许"节能显示"时，按设定时长保持 AOD 显示。

## 功能

- 一条滑条：`0` = 跟随系统，`1–239` = 熄屏后保持 N 分钟，`240` = 常亮
- 纯内存改写，不写 SecureSetting、不改渲染路径、不动系统设置
- 滑条归零或停用模块即完全恢复系统行为
- 与 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance) 可共存（包名、Provider、入口均独立）

## 原理

ColorOS 的 `AODDisplayUtil.getKeepAodOn()` 是 ROM 自带的"保持常亮"调试开关，被多处节能逻辑作为短路条件：

- `AodUpdateManager.isDisplayModeAllowUpdateClock()`
- `BaseAodClockLayoutController.onEnergySavingNotifyHide()`
- `BaseAodController.isUpsideDownEnable()`

模块在该方法返回后，于时间窗口内把结果改写为 `1`，窗口外还原为 `0`。

计时起点取自 `AodData.setAodIsInShow(true)`；亮屏（`PowerManager.isInteractive`）时清零。
息屏期间节能策略的反复隐藏/重现不会重置计时，只有重新熄屏才重新计时。

## 环境

- ColorOS（Oplus）系统
- LSPosed，作用域：`系统界面（com.android.systemui）`
- minSdk 33

## 构建

```bash
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

Release 构建需要在根目录提供 `keystore.properties`（`storeFile` / `storePassword` / `keyAlias` / `keyPassword`），
或在 `app/build.gradle` 中改用你自己的签名配置。

## 使用

1. 安装 APK，在 LSPosed 中启用「AOD 常亮计时」，作用域勾选系统界面
2. 重启系统界面
3. 打开应用，把滑条拨到需要的时长
4. 验证日志：

```bash
adb logcat -s AOD_Timer:D
```

预期：

```
AOD_KEEP: session start
AOD_KEEP: getKeepAodOn -> 1 (minutes=60)
```

## 已知限制

- 面板能力码未上报"全天"位时，ROM 本身不提供防烧屏保证；长期常亮请注意烧屏风险
- 与其它改写 `getKeepAodOn()` 的模块同时启用会互相覆盖，请只保留一个
- 到达设定时长后由 ROM 的下一次节能判定执行隐藏，可能有数秒到数十秒延迟

## 许可证

MIT。配置通道与 Hook 结构参考自 Qjj7679 的 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)，
原项目版权声明已保留在 [LICENSE](LICENSE) 中。
