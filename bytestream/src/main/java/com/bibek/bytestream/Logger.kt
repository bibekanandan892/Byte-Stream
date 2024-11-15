package com.bibek.bytestream

/**
 * Interface representing a Logger, which provides a mechanism to log messages
 * with different severity levels. The `log` method allows specifying a custom tag,
 * message, throwable, and log type.
 */
interface Logger {

    companion object {
        /** Default tag for logging messages */
        const val TAG = "ByteStream Log"
    }

    /**
     * Logs a message with the specified parameters.
     *
     * @param tag Optional tag to use for the log message. Defaults to [TAG].
     * @param message The message to log. Defaults to an empty string.
     * @param throwable An optional [Throwable] to log with the message. Defaults to null.
     * @param type The type of log message, indicating severity. Defaults to [LogType.Debug].
     */
    fun log(
        tag: String? = TAG,
        message: String? = "",
        throwable: Throwable? = null,
        type: LogType = LogType.Debug
    )
}

/**
 * Sealed interface representing the different types of log messages, indicating
 * the severity of the log.
 */
sealed interface LogType {
    /** Log message type for verbose logging. */
    data object Verbose : LogType

    /** Log message type for debug-level logging. */
    data object Debug : LogType

    /** Log message type for informational messages. */
    data object Info : LogType

    /** Log message type for warning messages. */
    data object Warn : LogType

    /** Log message type for error messages. */
    data object Error : LogType
}
