package com.op.aod.timer.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * 跨进程配置通道：UI 进程写入，SystemUI 进程读取。
 */
class TimerConfigProvider : ContentProvider() {

    companion object {
        private const val PREFS_NAME = "timer_config"

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(TimerConfigContract.AUTHORITY, TimerConfigContract.PATH, 1)
        }

        private val COLUMNS = arrayOf(
            TimerConfigContract.KEY_KEEP_AOD_MINUTES,
            TimerConfigContract.KEY_FORCE_DEBURN_IN,
        )
    }

    override fun onCreate(): Boolean = true

    private fun prefs() = context
        ?.createDeviceProtectedStorageContext()
        ?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != 1) return null
        val p = prefs() ?: return null
        return MatrixCursor(COLUMNS).apply {
            addRow(
                arrayOf<Any>(
                    p.getInt(
                        TimerConfigContract.KEY_KEEP_AOD_MINUTES,
                        TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
                    ),
                    if (p.getBoolean(
                            TimerConfigContract.KEY_FORCE_DEBURN_IN,
                            TimerConfigContract.DEFAULT_FORCE_DEBURN_IN
                        )
                    ) 1 else 0,
                )
            )
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (matcher.match(uri) != 1) return 0
        val p = prefs() ?: return 0
        val editor = p.edit()
        values?.let {
            if (it.containsKey(TimerConfigContract.KEY_KEEP_AOD_MINUTES)) {
                editor.putInt(
                    TimerConfigContract.KEY_KEEP_AOD_MINUTES,
                    it.getAsInteger(TimerConfigContract.KEY_KEEP_AOD_MINUTES)
                        ?: TimerConfigContract.DEFAULT_KEEP_AOD_MINUTES
                )
            }
            if (it.containsKey(TimerConfigContract.KEY_FORCE_DEBURN_IN)) {
                editor.putBoolean(
                    TimerConfigContract.KEY_FORCE_DEBURN_IN,
                    it.getAsBoolean(TimerConfigContract.KEY_FORCE_DEBURN_IN)
                        ?: TimerConfigContract.DEFAULT_FORCE_DEBURN_IN
                )
            }
        }
        // 同步落盘：配置写入频率很低，用 commit() 保证异常重启时不会丢
        editor.commit()
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? =
        if (matcher.match(uri) == 1) {
            "vnd.android.cursor.item/vnd.${TimerConfigContract.AUTHORITY}.${TimerConfigContract.PATH}"
        } else {
            null
        }
}
