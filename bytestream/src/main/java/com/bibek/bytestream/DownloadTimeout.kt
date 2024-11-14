package com.bibek.bytestream

import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_CONNECT_TIMEOUT_MS
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_READ_TIMEOUT_MS
import kotlinx.serialization.Serializable

@Serializable
class DownloadTimeout private constructor(
    val connectTimeOutInMs: Long = DEFAULT_VALUE_CONNECT_TIMEOUT_MS,
    val readTimeOutInMs: Long = DEFAULT_VALUE_READ_TIMEOUT_MS
) {
    class Builder {
        private var _connectTimeOutInMs: Long = DEFAULT_VALUE_CONNECT_TIMEOUT_MS
        private var _readTimeOutInMs: Long = DEFAULT_VALUE_READ_TIMEOUT_MS

        var connectTimeOutInMs: Long
            get() = _connectTimeOutInMs
            set(value) {
                _connectTimeOutInMs = value
            }

        var readTimeOutInMs: Long
            get() = _readTimeOutInMs
            set(value) {
                _readTimeOutInMs = value
            }

        fun build(): DownloadTimeout {
            return DownloadTimeout(connectTimeOutInMs, readTimeOutInMs)
        }
    }
}
