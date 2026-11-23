# AOD Timer — TODO

> 发布前如不想公开此文件，可直接删除。

## 1.1.1 已完成

- [x] 时段控制：只在设定的时间窗口内保持 AOD（支持跨午夜，默认 08:00–23:00）
- [x] 跨午夜时结束时间显示「次日 HH:mm」
- [x] UI：暗色模式跟随系统、状态栏 edge-to-edge、Miuix 时间滚轮
- [x] 关于页模板 + 底部链接（上游项目 / 本模块）
- [x] 应用名改为「ColorOS AOD 计时修改」
- [x] 移除自动唤醒（遮挡场景误点亮，弊大于利），代码注释保留
- [x] 删除旧的 Provider 残留、版本号与文档更新

## 已废弃

- [x] ~~时段开始时自动唤醒 AOD~~

  曾用 `OplusWakeUpController.notifyWakeUpCallback(0)` 实现，真机可点亮，
  但该入口没有遮挡判断，补传感器镜像后仍依赖 ROM 内部类结构，
  且需要常驻轮询线程。代码保留在 `hook/AutoWakeHook.kt`（整体注释）。

## 已迁移

- [x] 配置存储脱离 XSharedPreferences
  改为「设备加密目录 JSON 文件 + 只读 ContentProvider」，
  Provider 仅允许 system uid / `com.android.systemui` 读取。
  已移除 `xposedsharedprefs`，LSPosed 的弃用警告随之消失。
  注意：旧配置不会自动迁移，升级后需重新设置一次。

## 1.2（进行中）

- [x] 多语言模板化：`i18n/AppStrings.kt` + 简体中文 / English，跟随系统语言
- [ ] 发布前：README 中英文说明、Release Note、打 Tag `v1.2`

## 后续（不阻塞发布）

- [ ] Hook 策略链与降级：`getKeepAodOn` → `onEnergySavingNotifyHide` →
      `isDisplayModeAllowUpdateClock`，并输出当前生效策略。
- [ ] `Schedule` 纯函数单元测试（跨午夜 / 普通区间 / 全天）。
- [ ] 时段关闭时把相关选项置灰。
- [ ] 若将来重做自动唤醒，优先复用 ROM 定时显示链路
      （WAKEUP_TIME → mWakeUpReceiver → DozeService），并确认启动前有遮挡判断。
