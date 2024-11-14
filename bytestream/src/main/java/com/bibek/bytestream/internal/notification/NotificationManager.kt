package com.bibek.bytestream.internal.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.bibek.bytestream.NotificationConfig
import com.bibek.bytestream.internal.utils.Constant.CANCEL_BUTTON_LABEL
import com.bibek.bytestream.internal.utils.Constant.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.bibek.bytestream.internal.utils.Constant.FILE_NAME_KEY
import com.bibek.bytestream.internal.utils.Constant.LENGTH_KEY
import com.bibek.bytestream.internal.utils.Constant.MAX_PROGRESS_VALUE
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_IMPORTANCE_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_NAME_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_ID_KEY
import com.bibek.bytestream.internal.utils.Constant.PAUSE_BUTTON_LABEL
import com.bibek.bytestream.internal.utils.Constant.PROGRESS_KEY
import com.bibek.bytestream.internal.utils.Constant.REQUEST_ID_KEY
import com.bibek.bytestream.internal.utils.Constant.SMALL_NOTIFICATION_ICON_KEY
import com.bibek.bytestream.internal.utils.DownloadUtil
import com.bibek.bytestream.internal.utils.removeNotification

@SuppressLint("WrongConstant")
internal class NotificationManager(
    private val context: Context,
    private val notificationConfig: NotificationConfig,
    private val requestId: Int,
    private val fileName: String
) {
    private var foregroundInfo: ForegroundInfo? = null
    private val notificationId = requestId
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(notificationConfig.smallIcon)
            setContentTitle("Downloading $fileName")
            setOnlyAlertOnce(true)
            setOngoing(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
        }
    }

    fun updateNotification(
        progress: Int = 0,
        speedInBPerMs: Float = 0F,
        length: Long = 0L,
        update: Boolean = false
    ): ForegroundInfo? {
        if (update) {
            updateProgressNotification(progress, speedInBPerMs, length)
        } else {
            setupInitialNotification(progress)
        }
        return foregroundInfo
    }

    private fun setupInitialNotification(progress: Int) {
        removeNotification(context, requestId)
        removeNotification(context, requestId + 1)
        val pendingIntentOpen = createPendingIntent()
        val pendingIntentDismiss = createNotificationActionIntent(Notification.Dismissed.action)
        val pendingIntentPause = createNotificationActionIntent(Notification.Pause.action)
        val pendingIntentCancel = createNotificationActionIntent(Notification.Cancel.action)
        notificationBuilder.apply {
            setContentIntent(pendingIntentOpen)
            setProgress(MAX_PROGRESS_VALUE, progress, false)
            addAction(0, PAUSE_BUTTON_LABEL, pendingIntentPause)
            addAction(0, CANCEL_BUTTON_LABEL, pendingIntentCancel)
            setDeleteIntent(pendingIntentDismiss)
        }

        foregroundInfo = createForegroundInfo()
    }

    private fun updateProgressNotification(progress: Int, speedInBPerMs: Float, length: Long) {
        notificationBuilder.apply {
            setProgress(MAX_PROGRESS_VALUE, progress, false)
            setContentText(
                setContentTextNotification(
                    speedInBPerMs = speedInBPerMs,
                    progress = progress,
                    length
                )
            )
            setSubText("$progress%")
        }
        foregroundInfo = createForegroundInfo()
    }

    private fun setContentTextNotification(
        speedInBPerMs: Float,
        progress: Int,
        length: Long
    ): String {
        val timeLeftText = DownloadUtil.getRemainingTimeText(
            speedInBytesPerMs = speedInBPerMs,
            progressPercent = progress,
            totalBytes = length
        )
        val speedText = DownloadUtil.getSpeedText(speedInBytesPerMs = speedInBPerMs)
        val lengthText = DownloadUtil.getTotalLengthText(length)

        return listOfNotNull(
            if (notificationConfig.showTime) timeLeftText else null,
            if (notificationConfig.showSpeed) speedText else null,
            if (notificationConfig.showSize) "total: $lengthText" else null
        ).joinToString()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(REQUEST_ID_KEY, requestId)
        }
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notificationId,
            notificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            notificationConfig.channelName,
            notificationConfig.importance
        ).apply {
            description = notificationConfig.channelDescription
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun createNotificationActionIntent(action: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(NOTIFICATION_ID_KEY, requestId)
            putExtra(REQUEST_ID_KEY, requestId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun sendCompletionNotification(totalLength: Int) {
        sendBroadcastNotification(DownloadState.Completed, totalLength)
    }

    fun sendFailureNotification(currentProgress: Int, totalLength: Long) {
        sendBroadcastNotification(DownloadState.Failed, currentProgress, totalLength = totalLength)
    }

    fun sendCancelNotification() {
        sendBroadcastNotification(DownloadState.Cancel)
    }

    fun sendPauseNotification(currentProgress: Int) {
        sendBroadcastNotification(downloadState = DownloadState.Paused, progress = currentProgress)
    }

    fun sendSuccessNotification(totalLength: Long) {
        sendBroadcastNotification(
            downloadState = DownloadState.Completed,
            totalLength = totalLength,
        )
    }

    private fun sendBroadcastNotification(
        downloadState: DownloadState,
        progress: Int = 0,
        totalLength: Long = 0L
    ) {
        context.sendBroadcast(Intent(context, NotificationReceiver::class.java).apply {
            this.action = downloadState.action
            putExtra(NOTIFICATION_CHANNEL_NAME_KEY, notificationConfig.channelName)
            putExtra(NOTIFICATION_CHANNEL_IMPORTANCE_KEY, notificationConfig.importance)
            putExtra(NOTIFICATION_CHANNEL_DESCRIPTION_KEY, notificationConfig.channelDescription)
            putExtra(SMALL_NOTIFICATION_ICON_KEY, notificationConfig.smallIcon)
            putExtra(FILE_NAME_KEY, fileName)
            putExtra(REQUEST_ID_KEY, requestId)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
            putExtra(LENGTH_KEY, totalLength)
            putExtra(PROGRESS_KEY, progress)
        })
    }

}