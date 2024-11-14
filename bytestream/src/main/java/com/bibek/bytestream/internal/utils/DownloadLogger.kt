package com.bibek.bytestream.internal.utils

import android.util.Log
import com.bibek.bytestream.LogType
import com.bibek.bytestream.Logger

internal class DownloadLogger(private val enableLogs: Boolean) : Logger {
    override fun log(tag: String?, message: String?, throwable: Throwable?, type: LogType) {
        if (enableLogs) {
            when (type) {
                LogType.Verbose -> Log.v(tag, message, throwable)
                LogType.Debug -> Log.d(tag, message, throwable)
                LogType.Info -> Log.i(tag, message, throwable)
                LogType.Warn -> Log.w(tag, message, throwable)
                LogType.Error -> Log.e(tag, message, throwable)
            }
        }
    }
}
