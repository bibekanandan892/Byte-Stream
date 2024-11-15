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

/**
 * Worker class responsible for managing download tasks, updating progress, handling errors,
 * and sending notifications. This worker is initiated by WorkManager and runs on a background coroutine.
 */
internal class DownloadWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val MAX_PERCENT = 100 // Constant for calculating download percentage
    }

    // DAO instance to interact with the download database
    private val downloadDao = DatabaseInstance.getInstance(context).downloadDao()

    // Lazy initialization of NotificationManager with the given configuration
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

    // Lazy initialization of the file download request
    private val fileDownloadRequest by lazy {
        inputData.getString(DOWNLOAD_REQUEST_KEY)?.let {
            jsonToDownloadRequest(it)
        }
    }

    /**
     * Entry point for the worker. This method performs the download task, updating
     * progress and handling success or failure scenarios.
     * @return Result of the work: success, failure, or retry
     */
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun doWork(): Result {
        // Retrieve download request, and return failure if null
        val request = fileDownloadRequest ?: return Result.failure()
        val (downloadUrl, destinationPath, outputFileName, _, requestId, requestHeaders, _) = request

        // Initialize notification for ongoing download
        notificationManager?.updateNotification()?.let {
            setForeground(it)
        }

        return try {
            // Start download and update success status upon completion
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
            // Handle errors and return failure
            GlobalScope.launch {
                handleError(requestId, e, dirPath = destinationPath, fileName = outputFileName)
            }
            Result.failure(
                workDataOf(KEY_EXCEPTION to e.message)
            )
        }
    }

    /**
     * Initiates the download process and updates the status and progress.
     * @param id Request ID of the download
     * @param url URL of the file to download
     * @param dirPath Destination directory path for the downloaded file
     * @param fileName Name of the downloaded file
     * @param headers Optional headers for the download request
     * @param downloadDao DAO instance to update download progress
     * @return Total length of the downloaded file
     */
    private suspend fun startDownload(
        id: Int,
        url: String,
        dirPath: String,
        fileName: String,
        headers: MutableMap<String, String>,
        downloadDao: DownloadDao
    ): Long {
        var progressPercentage = 0
        return DownloadWork(url, dirPath, fileName, downloadDao = downloadDao).startDownload(
            requestId = id,
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

    /**
     * Updates the status of the download in the database.
     * @param id Download request ID
     * @param status Status of the download (e.g., in progress, paused, etc.)
     * @param totalBytes Total size of the file in bytes
     * @param downloadedBytes Bytes downloaded so far
     * @param speed Download speed in bytes per millisecond
     * @param errorReason Optional reason for failure, if applicable
     */
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

    /**
     * Updates the download status to success and records the total length of the downloaded file.
     * @param id Download request ID
     * @param totalLength Total size of the file
     */
    private suspend fun updateSuccessStatus(id: Int, totalLength: Long) {
        downloadDao.get(id)?.copy(
            currentStatus = Status.SUCCESS,
            totalBytes = totalLength,
            modifiedTime = System.currentTimeMillis()
        )?.let { downloadEntity ->
            downloadDao.update(downloadEntity)
        }
    }

    /**
     * Handles errors during the download, categorizing them as cancellations or failures.
     * @param id Download request ID
     * @param e Exception thrown during download
     * @param dirPath Directory path for the file
     * @param fileName Name of the file being downloaded
     */
    private suspend fun handleError(id: Int, e: Exception, dirPath: String, fileName: String) {
        if (e is CancellationException) {
            handleCancellation(id, dirPath, fileName)
        } else {
            handleFailure(id, e)
        }
    }

    /**
     * Handles download cancellation, pausing or canceling based on the current action.
     * @param id Download request ID
     * @param dirPath Directory path of the file
     * @param fileName Name of the file
     */
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

    /**
     * Handles failure during download, updating status and sending failure notification.
     * @param id Download request ID
     * @param e Exception thrown during download
     */
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

    /**
     * Calculates the current download progress percentage.
     * @param id Download request ID
     * @return Download progress as a percentage
     */
    private suspend fun currentProgress(id: Int): Int {
        val downloadEntity = downloadDao.get(id) ?: return 0
        return if (downloadEntity.totalBytes > 0) {
            ((downloadEntity.downloadedBytes * MAX_PERCENT) / downloadEntity.totalBytes).toInt()
        } else {
            0
        }
    }
}
