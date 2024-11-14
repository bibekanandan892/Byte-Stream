package com.bibek.bytestream.internal.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bibek.bytestream.internal.database.DatabaseInstance
import com.bibek.bytestream.internal.database.DownloadDao
import com.bibek.bytestream.internal.notification.NotificationManager
import com.bibek.bytestream.internal.utils.Action
import com.bibek.bytestream.internal.utils.Constant.DOWNLOAD_REQUEST_KEY
import com.bibek.bytestream.internal.utils.Constant.KEY_EXCEPTION
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CONFIG_KEY
import com.bibek.bytestream.internal.utils.Status
import com.bibek.bytestream.internal.utils.deleteFileIfExists
import com.bibek.bytestream.internal.utils.jsonToDownloadRequest
import com.bibek.bytestream.internal.utils.jsonToNotificationConfig
import com.bibek.bytestream.internal.download.DownloadWork

import kotlinx.coroutines.*

internal class DownloadWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val MAX_PERCENT = 100
    }

    private val downloadDao = DatabaseInstance.getInstance(context).downloadDao()
    private val notificationManager by lazy {
        inputData.getString(NOTIFICATION_CONFIG_KEY)?.let {
            jsonToNotificationConfig(it).takeIf { config -> config.enabled }?.let { config ->
                NotificationManager(
                    context,
                    config,
                    requestId = fileDownloadRequest?.requestId ?: 0,
                    fileDownloadRequest?.outputFileName ?: ""
                )
            }
        }
    }

    private val fileDownloadRequest by lazy {
        inputData.getString(DOWNLOAD_REQUEST_KEY)?.let {
            jsonToDownloadRequest(it)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun doWork(): Result {

        val request = fileDownloadRequest ?: return Result.failure()
        val (downloadUrl, destinationPath, outputFileName, _, requestId, requestHeaders, _) = request
        notificationManager?.updateNotification()?.let {
            setForeground(it)
        }


        return try {
            val totalLength = startDownload(
                id = requestId,
                url = downloadUrl,
                dirPath = destinationPath,
                fileName = outputFileName,
                headers = requestHeaders,
                downloadDao = downloadDao
            )
            updateSuccessStatus(requestId, totalLength)
            notificationManager?.sendSuccessNotification(totalLength)
            Result.success()
        } catch (e: Exception) {
            GlobalScope.launch {
                handleError(requestId, e, dirPath = destinationPath, fileName = outputFileName)

            }
            Result.failure(
                workDataOf(KEY_EXCEPTION to e.message)
            )
        }
    }



    private suspend fun startDownload(
        id: Int,
        url: String,
        dirPath: String,
        fileName: String,
        headers: MutableMap<String, String>,
        downloadDao: DownloadDao
    ): Long {
        var progressPercentage = 0
        return DownloadWork(url, dirPath, fileName, downloadDao = downloadDao).startDownload(requestId = id,
            headers = headers,
            onDownloadStart = { length ->
                updateDownloadStatus(id, Status.STARTED, length)
            },
            onDownloadProgress = { downloadedBytes, length, speed ->
                val progress = (downloadedBytes * 100 / length).toInt()
                if (progressPercentage != progress) {
                    progressPercentage = progress
                    updateDownloadStatus(
                        id = id,
                        status = Status.IN_PROGRESS,
                        totalBytes = length,
                        downloadedBytes = downloadedBytes,
                        speed = speed
                    )
                }
                notificationManager?.updateNotification(progress, speed, length, true)?.let {
                    setForeground(it)
                }
            }

        )
    }

    private suspend fun updateDownloadStatus(
        id: Int,
        status: String,
        totalBytes: Long = 0L,
        downloadedBytes: Long = 0L,
        speed: Float = 0f,
        errorReason: String = ""
    ) {
        downloadDao.get(id)?.copy(
            currentStatus = status,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            speedPerMs = speed,
            modifiedTime = System.currentTimeMillis(),
            errorReason = errorReason
        )?.let { downloadEntity ->
            downloadDao.update(downloadEntity)
        }
    }

    private suspend fun updateSuccessStatus(id: Int, totalLength: Long) {
        downloadDao.get(id)?.copy(
            currentStatus = Status.SUCCESS,
            totalBytes = totalLength,
            modifiedTime = System.currentTimeMillis()
        )?.let { downloadEntity ->
            downloadDao.update(downloadEntity)
        }
    }

    private suspend fun handleError(id: Int, e: Exception, dirPath: String, fileName: String) {
        if (e is CancellationException) {
            handleCancellation(id, dirPath, fileName)
        } else {
            handleFailure(id, e)
        }
    }

    private suspend fun handleCancellation(id: Int, dirPath: String, fileName: String) {
        val downloadEntity = downloadDao.get(id) ?: return
        downloadEntity.apply {
            when (downloadEntity.action) {
                Action.PAUSE -> {
                    updateDownloadStatus(
                        id = id,
                        status = Status.PAUSED,
                        totalBytes = totalBytes,
                        downloadedBytes = downloadedBytes,
                        speed = speedPerMs,
                        errorReason = errorReason
                    )
                    notificationManager?.sendPauseNotification(currentProgress(id))
                }

                else -> {
                    updateDownloadStatus(
                        id = id,
                        status = Status.CANCELLED,
                        totalBytes = totalBytes,
                        downloadedBytes = downloadedBytes,
                        speed = speedPerMs,
                        errorReason = errorReason
                    )
                    deleteFileIfExists(dirPath, fileName)
                    notificationManager?.sendCancelNotification()
                }
            }
        }
    }

    private suspend fun handleFailure(id: Int, e: Exception) {
        downloadDao.get(id)?.apply {
            updateDownloadStatus(
                id = id,
                status = Status.PAUSED,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                speed = speedPerMs,
                errorReason = e.localizedMessage ?: errorReason
            )
            notificationManager?.sendFailureNotification(currentProgress(id), 0)
        }
    }

    private suspend fun currentProgress(id: Int): Int {
        val downloadEntity = downloadDao.get(id) ?: return 0
        return if (downloadEntity.totalBytes > 0) {
            ((downloadEntity.downloadedBytes * MAX_PERCENT) / downloadEntity.totalBytes).toInt()
        } else {
            0
        }
    }
}
