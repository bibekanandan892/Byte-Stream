package com.bibek.bytestream.internal.notification

/**
 * Represents different types of notifications that can be triggered within the app.
 *
 * Each notification type is associated with a specific action string that can be used
 * to identify the notification event in the app.
 *
 * @property action A string identifier for the specific notification action.
 */
sealed class Notification(val action: String) {

    /**
     * Notification action for resuming a paused operation.
     */
    data object Resume : Notification("ACTION_NOTIFICATION_RESUME_CLICK")

    /**
     * Notification action for retrying a failed operation.
     */
    data object Retry : Notification("ACTION_NOTIFICATION_RETRY_CLICK")

    /**
     * Notification action for pausing an ongoing operation.
     */
    data object Pause : Notification("ACTION_NOTIFICATION_PAUSE_CLICK")

    /**
     * Notification action for canceling an ongoing operation.
     */
    data object Cancel : Notification("ACTION_NOTIFICATION_CANCEL_CLICK")

    /**
     * Notification action for dismissing a notification.
     */
    data object Dismissed : Notification("ACTION_NOTIFICATION_DISMISSED")
}

/**
 * Represents various states a download operation can be in.
 *
 * Each state is associated with a specific action string that can be used
 * to handle and identify different download states.
 *
 * @property action A string identifier for the specific download state.
 */
sealed class DownloadState(val action: String) {

    /**
     * State indicating that the download has completed successfully.
     */
    data object Completed : DownloadState("DOWNLOAD_COMPLETED")

    /**
     * State indicating that the download has failed.
     */
    data object Failed : DownloadState("DOWNLOAD_FAILED")

    /**
     * State indicating that the download has been paused.
     */
    data object Paused : DownloadState("DOWNLOAD_PAUSED")

    /**
     * State indicating that the download has been canceled.
     */
    data object Cancel : DownloadState("DOWNLOAD_CANCEL")
}
