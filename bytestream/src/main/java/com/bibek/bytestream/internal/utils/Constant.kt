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


    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    const val NOTIFICATION_CHANNEL_NAME_KEY = "notification_channel_name_key"
    const val DEFAULT_NOTIFICATION_CHANNEL_NAME = "Download Progress"
    const val NOTIFICATION_CHANNEL_DESCRIPTION_KEY = "notification_channel_description_key"
    const val DEFAULT_NOTIFICATION_CHANNEL_DESCRIPTION = "Notification for file download status"
    const val NOTIFICATION_CHANNEL_IMPORTANCE_KEY = "notification_channel_importance_key"
    const val DEFAULT_NOTIFICATION_CHANNEL_IMPORTANCE = 2 // LOW
    const val SMALL_NOTIFICATION_ICON_KEY = "small_notification_icon_key"
    const val DEFAULT_SMALL_NOTIFICATION_ICON = -1
    const val NOTIFICATION_ID_KEY = "notification_id_key"

    // Cancel
    const val CANCEL_BUTTON_LABEL = "Cancel"
    const val PAUSE_BUTTON_LABEL = "Pause"
    const val RESUME_BUTTON_LABEL = "Resume"
    const val RETRY_BUTTON_LABEL = "Retry"

    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME = "File Download"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION = "Notify file download status"
    const val DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE = 2 // LOW
}