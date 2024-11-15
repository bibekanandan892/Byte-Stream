package com.bibek.bytestream

import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_CONNECT_TIMEOUT_MS
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_READ_TIMEOUT_MS
import kotlinx.serialization.Serializable
/**
 * A class representing download timeout settings, including connection and read timeout values.
 *
 * This class provides a default configuration with customizable timeout values for managing
 * network requests in a more efficient manner.
 *
 * @property connectTimeOutInMs The connection timeout duration in milliseconds. Defaults to [DEFAULT_VALUE_CONNECT_TIMEOUT_MS].
 * @property readTimeOutInMs The read timeout duration in milliseconds. Defaults to [DEFAULT_VALUE_READ_TIMEOUT_MS].
 */
@Serializable
class DownloadTimeout private constructor(
    val connectTimeOutInMs: Long = DEFAULT_VALUE_CONNECT_TIMEOUT_MS,
    val readTimeOutInMs: Long = DEFAULT_VALUE_READ_TIMEOUT_MS
) {

    /**
     * A builder class for constructing an instance of [DownloadTimeout] with customized values.
     *
     * This class provides a fluent API for setting connection and read timeout values.
     */
    class Builder {
        private var _connectTimeOutInMs: Long = DEFAULT_VALUE_CONNECT_TIMEOUT_MS
        private var _readTimeOutInMs: Long = DEFAULT_VALUE_READ_TIMEOUT_MS

        /**
         * The connection timeout duration in milliseconds.
         * Defaults to [DEFAULT_VALUE_CONNECT_TIMEOUT_MS].
         */
        var connectTimeOutInMs: Long
            get() = _connectTimeOutInMs
            set(value) {
                _connectTimeOutInMs = value
            }

        /**
         * The read timeout duration in milliseconds.
         * Defaults to [DEFAULT_VALUE_READ_TIMEOUT_MS].
         */
        var readTimeOutInMs: Long
            get() = _readTimeOutInMs
            set(value) {
                _readTimeOutInMs = value
            }

        /**
         * Builds and returns an instance of [DownloadTimeout] with the specified timeout values.
         *
         * @return A new instance of [DownloadTimeout].
         */
        fun build(): DownloadTimeout {
            return DownloadTimeout(connectTimeOutInMs, readTimeOutInMs)
        }
    }
}
