package com.op.aod.timer.data

// 本文件原有的 ContentProvider 已移除。
//
// v1.1 起配置读写改走 YukiHookAPI 的 YukiHookPrefsBridge：
// 模块 App 内是普通 SharedPreferences，宿主进程内是 XSharedPreferences，
// 不再需要 ContentProvider 跨进程通道，也不会再出现"重启后要打开一次 App"。
//
// 该文件可以安全删除。
