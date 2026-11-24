# 自动唤醒（Auto Wake）— 结论与方法总结

> 本文档记录 `v1.2` 自动唤醒功能的调研结论、ROM 内部链路、当前实现与合并注意事项。
> 支持代码位于本分支的 `hook/AutoWakeHook.kt`。

## 一、结论

| 问题 | 结论 |
|---|---|
| 能否主动点亮 AOD | 能。入口是 `OplusWakeUpController.notifyWakeUpCallback(0)`，与黑屏单击同一条链 |
| 能否直接复用遮挡判断 | 不能。该入口只打 `reject` 日志，回调照样分发 |
| 遮挡状态从哪来 | 镜像 ROM 手势传感器回调最可靠；`getProxNear()` 在 AOD 隐藏时会被重置成 false |
| 遮挡后移开能否补亮 | 能，但必须自己做重试；单次触发会错过 |

## 二、关键 ROM 链路

### 1. 点击唤醒链

```
黑屏手势码 16
  → OplusWakeUpController$AodSingleClickWakeUpCallback.onClick
  → OplusWakeUpController.notifyWakeUpCallback(0)
```

### 2. 遮挡门禁的位置

在 `OplusBlackScreenGestureControllExImpl$sensorListener$1.onSensorChanged`：

- near → handler 消息 `10009`（禁用点击节点）
- far  → handler 消息 `10010`（恢复）

也就是说，口袋里点不亮是**服务层**挡的，不是 `notifyWakeUpCallback` 挡的。
`notifyWakeUpCallback` 内部对 `proximityNear` 只打印 `reject wakeup while near`，
随后仍然遍历并分发回调。

### 3. `proximityNear` 为什么不能复用

`unregisterWakeUpMonitor()` 会把 `OplusWakeUpController.proximityNear` 置回 `false`。
AOD 隐藏时正好处于这个状态，此时读到的"不遮挡"是假值。

### 4. 传感器类型

同一个手势监听同时处理：

- `Sensor.TYPE_PROXIMITY`（8）
- OPPO 手势传感器（`33171066`）

near 判定与 ROM 保持一致：

- type 8：`value < sensor.maximumRange`
- 33171066：`value == 0f`

### 5. ROM 自带的定时唤醒链（备选方案）

```
WAKEUP_TIME 广播
  → AodWakeUpReceiver
  → AodUpdateManager.mWakeUpReceiver
```

限制：`AodUpdateManager.registerWakeupBroadcast()` 只在
`AODDisplayUtil.mPerformAodType != 0 && isCurrentAodEnableTimeOpen() && !checkInDuration()`
时注册；`setWakeupAlarm()` 负责计算下一次开始时间。

若要复用这条链，需要同时模拟：

- `isCurrentAodEnableTimeOpen()`
- `getBeginHour()` / `getBeginMin()`
- `getEndHour()` / `getEndMin()`

并处理 `CommonUtils.checkInDuration()` 直接读 SecureSetting 的问题。

### 6. 其他门禁

- `AodManager.notNeedWakeAod()`：时段外、禅定模式、节能场景等
- `AodManager.isInCall()`：通话中

## 三、当前实现

### 何时才需要自动唤醒

- **需要**：进入时段时 AOD 已被隐藏（例如长时间遮盖），此后移除遮挡需要主动点亮。
- **不需要**：时段内 AOD 已经亮过。此时 `getKeepAodOn()` 已被改写为 `1`（伪常亮），
  遮挡造成的亮/暗由 ROM 原生接近逻辑处理——覆盖时临时隐藏，`far` 时自动恢复，
  AOD 会话并未结束。
- 因此 `wake()` 成功后 `settledThisWindow = true`、本时段不再尝试，是正确的交接语义；
  不需要为"重复遮盖/掀开"做重新点亮、指数退避或超时心跳。
- 同理，"进入时段时屏幕亮着"也无须特殊处理：之后锁屏会走原生 AOD 流程。

### 已知但不处理的边界

- SystemUI 中途重启会丢失 `settledThisWindow`，且 `showStartMs` 可能仍是旧会话的值，
  极端情况下补亮后保持时长会偏短。发生概率极低，判定为可接受，不做额外处理。
  （若将来要修：唤醒成功后调用 `KeepAodHook.resetSession()` 重置保持计时即可。）

### 调度

```
工作线程按需等待
  ├─ near 状态变化 → 立即唤醒线程重新判断
  ├─ 无变化 → 按「重试间隔」超时兜底
  ├─ 进入时段后的首次尝试
  ├─ 超过「重试总时长」→ 本时段不再尝试
  └─ 已亮 / 亮屏 / ROM 拒绝 → 本时段结束
```

### wake() 判断顺序

1. 屏幕亮 → 结束（无意义）
2. AOD 已在显示 → 结束
3. 遮挡 → 重试
4. `notNeedWakeAod` / 通话中 → 结束
5. 反射调用 `OplusWakeUpController.notifyWakeUpCallback(0)`

### 遮挡判断

- 优先使用镜像：`OplusBlackScreenGestureControllExImpl$sensorListener$1.onSensorChanged`
- 镜像数据 60 秒内有效；过期或无数据时自行采样 500ms

### 配置项

| 键 | 含义 | 默认 | 范围 |
|---|---|---|---|
| `auto_wake` | 时段开始时尝试主动唤醒 | `false` | — |
| `wake_retry_minutes` | 遮挡跳过后的重试总时长（分钟） | `30` | `0–240`，步进 10 |
| `wake_retry_interval_seconds` | 遮挡期间的重试间隔（秒） | `60` | `30–300`，步进 30 |
| `wake_fallback_proximity` | 镜像失效时允许自采样接近传感器 | `false` | —（开启后可能增加误触发概率） |

## 四、分支现状（合并状态）

- ✅ `assembleDebug` 通过
- ✅ `MainHook` 已接线 `hookAutoWake()`
- ✅ 配置链已支持 `auto_wake` / `wake_retry_minutes`
      （`TimerConfigContract` / `TimerConfigFile` / `TimerConfigProvider` / `TimerConfigReader`）
- ✅ UI 已暴露两个开关：自动唤醒开关 + 遮挡重试滑条（0–30 分钟，0 = 不重试）
- ✅ 保存配置不再用默认值覆盖这两项（二者已纳入 UI 快照）

## 五、风险与替代方案

- 镜像依赖内部类名 `OplusBlackScreenGestureControllExImpl$sensorListener$1`，
  ROM 改名后会降级为自采样（日志：`proximity mirror hook failed`）
- 自采样会短暂注册接近传感器 500ms；重试间隔可调，用来兜住退化场景
- 自采样默认关闭（`wake_fallback_proximity`）：关闭时拿不到镜像数据按"遮挡"处理，
  不冒险点亮；开启后会在镜像失效时主动采样，可能增加误触发概率
- near 变化即时触发，遮挡期间几乎不产生额外轮询；
  时段外低频复查（60s），避免常驻 15s 轮询
- 主动点亮本身有耗电、烧屏、夜间打扰风险，
  因此设计为默认关闭，重试窗口与间隔由用户配置
- 更稳但更重的长期方案：模拟"定时显示"（见 2.5），成本约 5 个落点

## 六、验证日志

```bash
adb logcat -s AOD_Timer:I | findstr AUTO_WAKE
```

关键行：

```text
AUTO_WAKE: proximity mirror: near=true (type=8, value=0.0)
AUTO_WAKE: entered window
AUTO_WAKE: skip: proximity covered, will retry
AUTO_WAKE: proximity changed, re-check
AUTO_WAKE: calling notifyWakeUpCallback(0)
AUTO_WAKE: notifyWakeUpCallback(0) invoked
```

## 七、文件清单

| 文件 | 内容 |
|---|---|
| `hook/AutoWakeHook.kt` | 全部实现：镜像、门禁、重试、唤醒 |
| `hook/MainHook.kt` | `runCatching { hookAutoWake() }` |
| `data/TimerConfigContract.kt` | `auto_wake` / `wake_retry_minutes` 键与默认值 |
| `data/TimerConfigFile.kt` | JSON 读写 |
| `data/TimerConfigProvider.kt` | 只读列与访问控制 |
| `hook/TimerConfigReader.kt` | Provider 查询与磁盘缓存兜底 |
