package com.bibek.bytestream.internal.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bibek.bytestream.ByteStream
import com.bibek.bytestream.internal.utils.Constant.CANCEL_BUTTON_LABEL
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_LENGTH_VALUE
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_NOTIFICATION_CHANNEL_DESCRIPTION
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_NOTIFICATION_CHANNEL_IMPORTANCE
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_NOTIFICATION_CHANNEL_NAME
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_SMALL_NOTIFICATION_ICON
import com.bibek.bytestream.internal.utils.Constant.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.bibek.bytestream.internal.utils.Constant.FILE_NAME_KEY
import com.bibek.bytestream.internal.utils.Constant.LENGTH_KEY
import com.bibek.bytestream.internal.utils.Constant.MAX_PROGRESS_VALUE
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_DESCRIPTION_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_IMPORTANCE_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CHANNEL_NAME_KEY
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_ID_KEY
import com.bibek.bytestream.internal.utils.Constant.PROGRESS_KEY
import com.bibek.bytestream.internal.utils.Constant.REQUEST_ID_KEY
import com.bibek.bytestream.internal.utils.Constant.RESUME_BUTTON_LABEL
import com.bibek.bytestream.internal.utils.Constant.RETRY_BUTTON_LABEL
import com.bibek.bytestream.internal.utils.Constant.SMALL_NOTIFICATION_ICON_KEY
import com.bibek.bytestream.internal.utils.DownloadUtil.getTotalLengthText

internal class NotificationReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        intent.action ?: return
        val byteStream = ByteStream.getInstance() ?: return

        when (intent.action) {
            Notification.Cancel.action -> handleAction(
                context, intent, byteStream::cancel
            )

            Notification.Pause.action -> handleAction(
                context, intent, byteStream::pause
            )

            Notification.Resume.action -> handleAction(
                context, intent, byteStream::resume
            )

            Notification.Retry.action -> handleAction(
                context, intent, byteStream::retry
            )

            Notification.Dismissed.action -> {
                val intentOpen =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                intentOpen?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intentOpen?.putExtra(
                    REQUEST_ID_KEY,
                    intent.getStringExtraOrDefault(REQUEST_ID_KEY, "")
                )
                context.startActivity(intentOpen)
            }

            else -> handleNotificationAction(context, intent)

        }
    }

    @SuppressLint("MissingPermission")
    private fun handleNotificationAction(context: Context, intent: Intent) {
        val requestId = intent.extras?.getInt(REQUEST_ID_KEY) ?: return
        val notificationChannelName = intent.getStringExtraOrDefault(
            NOTIFICATION_CHANNEL_NAME_KEY, DEFAULT_NOTIFICATION_CHANNEL_NAME
        )
        val notificationImportance = intent.getIntExtraOrDefault(
            NOTIFICATION_CHANNEL_IMPORTANCE_KEY, DEFAULT_NOTIFICATION_CHANNEL_IMPORTANCE
        )
        val notificationChannelDescription = intent.getStringExtraOrDefault(
            NOTIFICATION_CHANNEL_DESCRIPTION_KEY, DEFAULT_NOTIFICATION_CHANNEL_DESCRIPTION
        )
        val notificationSmallIcon = intent.getIntExtraOrDefault(
            SMALL_NOTIFICATION_ICON_KEY, DEFAULT_SMALL_NOTIFICATION_ICON
        )
        val fileName = intent.extras?.getString(FILE_NAME_KEY) ?: ""
        val currentProgress = intent.getIntExtraOrDefault(PROGRESS_KEY, 0)
        val totalLength = intent.getLongExtraOrDefault(LENGTH_KEY, DEFAULT_LENGTH_VALUE)
        val notificationId = requestId + 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                context,
                notificationChannelName,
                notificationImportance,
                notificationChannelDescription
            )
        }

        val notificationBuilder =
            NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(notificationSmallIcon)
                .setContentText(getNotificationText(intent.action, totalLength, currentProgress))
                .setContentTitle(fileName)
                .setContentIntent(
                    createPendingIntent(
                        context,
                        requestId,
                        Notification.Dismissed.action
                    )
                )
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)

        addActionButtons(
            context,
            intent.action,
            notificationId,
            notificationBuilder,
            currentProgress
        )

        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }


    private fun handleAction(context: Context, intent: Intent, action: (Int) -> Unit) {
        val requestId = intent.extras?.getInt(REQUEST_ID_KEY)
        val notificationId = intent.extras?.getInt(NOTIFICATION_ID_KEY)
        notificationId?.let { NotificationManagerCompat.from(context).cancel(it) }
        requestId?.let(action)
    }


    private fun getNotificationText(action: String?, totalLength: Long, currentProgress: Int) =
        when (action) {
            DownloadState.Completed.action -> "Download successful. (${
                getTotalLengthText(
                    totalLength
                )
            })"

            DownloadState.Failed.action -> "Download failed."
            DownloadState.Paused.action -> "Download paused."
            else -> "Download cancelled."
        }.plus(if (action == DownloadState.Paused.action || action == DownloadState.Failed.action) " $currentProgress%" else "")

    private fun addActionButtons(
        context: Context,
        action: String?,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        currentProgress: Int
    ) {
        when (action) {
            DownloadState.Failed.action -> {
                builder.addAction(
                    -1,
                    RETRY_BUTTON_LABEL,
                    createPendingIntent(
                        context,
                        notificationId,
                        Notification.Retry.action
                    )
                )
                    .addAction(
                        -1,
                        CANCEL_BUTTON_LABEL,
                        createPendingIntent(
                            context,
                            notificationId,
                            Notification.Cancel.action
                        )
                    )
                    .setProgress(MAX_PROGRESS_VALUE, currentProgress, false)
            }

            DownloadState.Paused.action -> {
                builder.addAction(
                    -1,
                    RESUME_BUTTON_LABEL,
                    createPendingIntent(
                        context,
                        notificationId,
                        Notification.Resume.action
                    )
                )
                    .addAction(
                        -1,
                        CANCEL_BUTTON_LABEL,
                        createPendingIntent(
                            context,
                            notificationId,
                            Notification.Cancel.action
                        )
                    )
                    .setProgress(MAX_PROGRESS_VALUE, currentProgress, false)
            }
        }
    }

    private fun createPendingIntent(
        context: Context,
        notificationId: Int,
        action: String
    ): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(NOTIFICATION_ID_KEY, notificationId)
            putExtra(REQUEST_ID_KEY, notificationId - 1)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        name: String,
        importance: Int,
        description: String
    ) {
        val channel =
            NotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                this.description = description
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun Intent.getStringExtraOrDefault(key: String, defaultValue: String) =
        extras?.getString(key) ?: defaultValue

    private fun Intent.getIntExtraOrDefault(key: String, defaultValue: Int) =
        extras?.getInt(key) ?: defaultValue

    private fun Intent.getLongExtraOrDefault(key: String, defaultValue: Long) =
        extras?.getLong(key) ?: defaultValue
}
