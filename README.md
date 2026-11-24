# ColorOS AOD Timer

ColorOS 息屏显示（AOD）计时模块（Xposed / LSPosed）。

在系统只提供"节能显示"的机型上，按设定时长保持 AOD；可选启用时段，
只在指定时间窗口内生效。界面跟随系统语言。

> **当前版本 1.2.1-Beta**：自动唤醒为实验性功能，需要跨夜长时间验证。
> 遇到异常请附 `adb logcat -s AOD_Timer:V` 的日志。

## 功能

- 一条滑条：`0` = 跟随系统，`1–239` = 熄屏后保持 N 分钟，`240` = 常亮
- 启用时段：只在窗口内保持 AOD，支持跨午夜（默认 `08:00–23:00`）
- 一个开关：强制启用防烧屏位移（系统在省电常亮时默认不做位移）
- 时段开始时自动唤醒（实验性，默认关闭）：到点尝试点亮 AOD；被遮挡时在
  「重试总时长 0–240 分钟」内按「重试间隔 30–300 秒」重试，遮挡状态变化会立即重试
- 纯内存改写，不写 SecureSetting、不改渲染路径、不动系统设置
- 滑条归零、关闭时段或停用模块即完全恢复系统行为
- 对局部息屏同样生效；局部息屏自带位置偏移，无需开启强制位移
- 多语言：简体中文 / English，跟随系统
- 与 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance) 可共存（包名、配置、入口均独立）

## 原理

ColorOS 的 `AODDisplayUtil.getKeepAodOn()` 是 ROM 自带的"保持常亮"调试开关，被多处节能逻辑作为短路条件：

- `AodUpdateManager.isDisplayModeAllowUpdateClock()`
- `BaseAodClockLayoutController.onEnergySavingNotifyHide()`
- `BaseAodController.isUpsideDownEnable()`

模块在该方法返回后，于时间窗口内把结果改写为 `1`，窗口外还原为 `0`。

时段窗口按"当天第几分钟"比较，跨午夜只是多一个 `||` 判断；窗口外不做任何改写。

计时起点取自 `AodData.setAodIsInShow(true)`；亮屏（`PowerManager.isInteractive`）时清零。
息屏期间节能策略的反复隐藏/重现不会重置计时，只有重新熄屏才重新计时。

## 配置存储（v1.2 起）

旧版本通过 LSPosed 的 **New XSharedPreferences** 读写配置。该特性计划在
**LSPosed 2.3.0 移除**，且模块 App 进程不在时无法读取，因此 v1.2 改为：

| 角色 | 位置 |
|---|---|
| 模块 App 写入 | `/data/user_de/0/com.op.aod.timer/files/timer_config.json` |
| SystemUI 读取 | `content://com.op.aod.timer.config/timer_config`（只读 Provider） |
| SystemUI 缓存 | `/data/user_de/0/com.android.systemui/files/aod_timer_config_cache.json` |

几点说明：

- 配置放在**设备加密（DE）**目录，开机后、首次解锁前也能读到；
- Provider 只读，且只允许 SystemUI 的 uid/包名访问；
- SystemUI 每次成功读取都会更新自己的缓存，**模块 App 进程被冻结或杀掉后仍可工作**；
- 写入采用「临时文件 + 重命名」，避免异常中断留下半截 JSON。

> **升级注意**：旧配置不会自动迁移，升级到 v1.2 后需要重新设置一次数值。

## 环境

- ColorOS（Oplus）系统
- LSPosed，作用域：`系统界面（com.android.systemui）`
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

1. 安装 APK，在 LSPosed 中启用「ColorOS AOD 计时修改」，作用域勾选系统界面
2. 重启系统界面
3. 打开应用，把滑条拨到需要的时长
4. 使用 Debug 版本时可验证日志：

```bash
adb logcat -s AOD_Timer:V
```

预期：

```
AOD_Timer: config loaded: keep=60, schedule=false 480-1380, forceShift=false
AOD_Timer: AOD_KEEP: minutes=60, keep=true
```

模块 App 进程不在运行时：

```
AOD_Timer: config from disk cache: keep=60
```

开启「自动唤醒」后，时段起点会看到：

```
AOD_Timer: AUTO_WAKE: entered window
AOD_Timer: AUTO_WAKE: skip: proximity covered, will retry   # 被遮挡时
AOD_Timer: AUTO_WAKE: calling notifyWakeUpCallback(0)
```

## 多语言

界面文案采用「模板化」设计，跟随系统语言（无匹配时回退简体中文）。

新增一种语言只需要三步：

1. 复制 `i18n/EnStrings.kt` 为 `i18n/XxxStrings.kt`，实现 `AppStrings` 接口；
2. 在 `i18n/Languages.kt` 的 `all` 列表里注册；
3. （可选）新建 `res/values-xx/strings.xml`，补上 `app_name` 与 `xposed_description`。

布局代码不需要改动——所有文案都通过 `LocalStrings.current` 获取。

## 已知限制

- 面板能力码未上报"全天"位时，ROM 本身不提供防烧屏保证；长期常亮请注意烧屏风险
- 与其它改写 `getKeepAodOn()` 的模块同时启用会互相覆盖，请只保留一个
- 到达设定时长后由 ROM 的下一次节能判定执行隐藏，可能有数秒到数十秒延迟
- 时段窗口不会主动点亮 AOD（除非开启「自动唤醒」）；未开启时若窗口开始时 AOD 已被隐藏，需要手动点亮一次
- 首次安装后需成功打开一次 App 并保存配置，SystemUI 才有可用的缓存
- 自动唤醒为实验性功能：遮挡判断优先镜像 ROM 手势传感器回调，ROM 改动后会降级为自采样；
  主动点亮仍有耗电与夜间打扰风险，默认关闭
- 自动唤醒的重试总时长最长 240 分钟、间隔最长 300 秒；时段外仅每 60 秒复查一次配置
- 「镜像失效时自采样接近传感器」默认关闭；开启后可能增加误触发概率

## 声明

- 项目启发自 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)
- AI 使用

## 许可证

MIT。配置通道与 Hook 结构参考自 Qjj7679 的 [ColorOS AOD Enhance](https://github.com/Qjj7679/ColorOS_Aod_Enhance)，
原项目版权声明已保留在 [LICENSE](LICENSE) 中。
