package com.bibek.bytestream.internal.notification

sealed class Notification(val action: String) {
    data object Resume : Notification("ACTION_NOTIFICATION_RESUME_CLICK")
    data object Retry : Notification("ACTION_NOTIFICATION_RETRY_CLICK")
    data object Pause : Notification("ACTION_NOTIFICATION_PAUSE_CLICK")
    data object Cancel : Notification("ACTION_NOTIFICATION_CANCEL_CLICK")
    data object Dismissed : Notification("ACTION_NOTIFICATION_DISMISSED")

}
sealed class DownloadState(val action: String){
    data object Completed : DownloadState("DOWNLOAD_COMPLETED")
    data object Failed : DownloadState("DOWNLOAD_FAILED")
    data object Paused : DownloadState("DOWNLOAD_PAUSED")
    data object Cancel : DownloadState("DOWNLOAD_CANCEL")
}
