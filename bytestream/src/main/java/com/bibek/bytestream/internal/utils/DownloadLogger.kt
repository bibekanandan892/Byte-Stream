package com.bibek.bytestream.internal.utils

import android.util.Log
import com.bibek.bytestream.LogType
import com.bibek.bytestream.Logger

/**
 * A logger class for handling different log types with conditional logging.
 *
 * @property enableLogs Boolean flag to enable or disable logging.
 */
internal class DownloadLogger(private val enableLogs: Boolean) : Logger {

    /**
     * Logs a message with the specified log type, if logging is enabled.
     *
     * @param tag Optional tag for the log message, typically used to indicate the source of the log.
     * @param message The message to log.
     * @param throwable An optional Throwable to log for error or warning cases.
     * @param type The type of log to use, which determines the severity level.
     */
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
