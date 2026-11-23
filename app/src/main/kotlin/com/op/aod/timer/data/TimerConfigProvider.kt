package com.op.aod.timer.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Process
import android.util.Log

/**
 * 只读配置通道：模块 App 写文件，SystemUI 通过 binder 读取。
 *
 * 取代原先基于 XSharedPreferences 的 `YukiHookPrefsBridge`，
 * 从而不依赖 LSPosed 的 New XSharedPreferences（计划于 2.3.0 移除）。
 *
 * 访问控制：只允许 system uid（SystemUI）与自身 uid 读取；
 * 其他调用者返回空结果并记录日志。
 */
class TimerConfigProvider : ContentProvider() {

    companion object {
        private const val TAG = "AOD_Timer"

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(TimerConfigContract.AUTHORITY, TimerConfigContract.PATH, 1)
        }

        private val COLUMNS = arrayOf(
            "keep_aod_minutes",
            "force_deburn_in",
            "schedule_enabled",
            "schedule_start_minute",
            "schedule_end_minute",
            "auto_wake",
        )
    }

    override fun onCreate(): Boolean = true

    @Volatile
    private var warnedMissing = false

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != 1) return null
        if (!isAllowedCaller()) return null

        val ctx = context ?: return null
        val stored = TimerConfigFile.read(ctx)
        if (stored == null && !warnedMissing) {
            warnedMissing = true
            Log.w(TAG, "config file missing or unreadable, serving defaults")
        }
        val config = stored ?: TimerConfig()
        return MatrixCursor(COLUMNS).apply {
            addRow(
                arrayOf<Any>(
                    config.keepAodMinutes,
                    if (config.forceDeburnIn) 1 else 0,
                    if (config.scheduleEnabled) 1 else 0,
                    config.scheduleStartMinute,
                    config.scheduleEndMinute,
                    if (config.autoWake) 1 else 0,
                )
            )
        }
    }

    private fun isAllowedCaller(): Boolean {
        val uid = Binder.getCallingUid()
        if (uid == Process.SYSTEM_UID || uid == Process.myUid()) return true

        // 不按名称通配，直接问系统这个 uid 下挂载了哪些包。
        // 共享 uid（如 ColorOS 的 10150）也能正确解析出 com.android.systemui。
        val packages = runCatching {
            context?.packageManager?.getPackagesForUid(uid)?.toList()
        }.getOrNull()
        if (packages?.contains("com.android.systemui") == true) return true

        // 兜底：持有 SystemUI 专属权限的进程（与包名无关）
        val hasStatusBarPermission = runCatching {
            context?.checkCallingPermission("android.permission.STATUS_BAR_SERVICE") ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (hasStatusBarPermission) return true

        Log.w(TAG, "config query denied, caller uid=$uid packages=$packages")
        return false
    }

    /** 配置只由模块 App 直接写文件，Provider 不提供写入。 */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? =
        if (matcher.match(uri) == 1) {
            "vnd.android.cursor.item/vnd.${TimerConfigContract.AUTHORITY}.${TimerConfigContract.PATH}"
        } else {
            null
        }
}
