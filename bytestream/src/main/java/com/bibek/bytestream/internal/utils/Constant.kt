package com.bibek.bytestream.internal.utils

object Constant {
    const val DEFAULT_VALUE_READ_TIMEOUT_MS = 10000L
    const val DEFAULT_VALUE_CONNECT_TIMEOUT_MS = 10000L

    const val KEY_EXCEPTION = "key_exception"
    const val RANGE_HEADER = "Range"
    const val HTTP_STATUS_RANGE_NOT_SATISFIABLE = 416
    const val DOWNLOAD_TAG = "downloads"
    const val FILE_NAME_KEY = "key_fileName"
    const val STATE_KEY = "key_state"
    const val PROGRESS_KEY = "key_progress"
    const val MAX_PROGRESS_VALUE = 100
    const val PROGRESS_STATUS = "progress"
    const val STARTED_STATUS = "started"
    const val LENGTH_KEY = "key_length"
    const val DEFAULT_LENGTH_VALUE = 0L
    const val REQUEST_ID_KEY = "key_request_id"
    const val DOWNLOAD_REQUEST_KEY = "key_download_request"
    const val NOTIFICATION_CONFIG_KEY = "key_notification_config"
    const val ETAG_HEADER_KEY = "ETag"
}