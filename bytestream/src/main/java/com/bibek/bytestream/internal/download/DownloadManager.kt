package com.bibek.bytestream.internal.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bibek.bytestream.DownloadModel
import com.bibek.bytestream.DownloadTimeout
import com.bibek.bytestream.Logger
import com.bibek.bytestream.NotificationConfig
import com.bibek.bytestream.internal.database.DownloadDao
import com.bibek.bytestream.internal.database.DownloadEntity
import com.bibek.bytestream.internal.utils.Action
import com.bibek.bytestream.internal.utils.Constant.DOWNLOAD_REQUEST_KEY
import com.bibek.bytestream.internal.utils.Constant.DOWNLOAD_TAG
import com.bibek.bytestream.internal.utils.Constant.NOTIFICATION_CONFIG_KEY
import com.bibek.bytestream.internal.utils.Constant.PROGRESS_STATUS
import com.bibek.bytestream.internal.utils.Constant.STARTED_STATUS
import com.bibek.bytestream.internal.utils.Constant.STATE_KEY
import com.bibek.bytestream.internal.utils.Status
import com.bibek.bytestream.internal.utils.deleteFileIfExists
import com.bibek.bytestream.internal.utils.hashMapToJson
import com.bibek.bytestream.internal.utils.removeNotification
import com.bibek.bytestream.internal.utils.toDownloadModel
import com.bibek.bytestream.internal.utils.toFileDownloadRequest
import com.bibek.bytestream.internal.utils.toJson
import com.bibek.bytestream.internal.worker.DownloadWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * A manager class responsible for handling download tasks, including scheduling, pausing,
 * resuming, and canceling downloads, as well as managing download states and observing progress.
 *
 * @property context The application context used for notifications and other app-level actions.
 * @property downloadDao Data access object for interacting with the download database.
 * @property workManager The WorkManager instance used for scheduling and tracking download tasks.
 * @property downloadTimeout Timeout configuration for downloads.
 * @property notificationConfig Configuration for notifications related to downloads.
 * @property logger Logger instance for logging events and errors.
 */
internal class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager,
    private val downloadTimeout: DownloadTimeout,
    private val notificationConfig: NotificationConfig,
    private val logger: Logger,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.log("Exception in DownloadManager Scope: ${throwable.message}")
        }

    init {
        launch {
            observerWorkInfos()
        }
    }

    /**
     * Observes the WorkManager's work info updates for download tasks and logs the states.
     */
    private suspend fun observerWorkInfos() {
        workManager.getWorkInfosForUniqueWorkFlow(DOWNLOAD_TAG)
            .flowOn(Dispatchers.IO)
            .collectLatest { workInfos ->
                workInfos.forEach { workInfo ->
                    logWorkInfo(workInfo)
                }
            }
    }

    /**
     * Logs the details of a work request based on its state.
     *
     * @param workInfo The WorkInfo object representing the download task's current state.
     */
    private suspend fun logWorkInfo(workInfo: WorkInfo) {
        val downloadEntity = getDownloadWorkEntityFromUUID(workInfo.id)
        val message = when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> "Download Queued"
            WorkInfo.State.RUNNING -> getRunningMessage(downloadEntity, workInfo)
            WorkInfo.State.SUCCEEDED -> "Download Success"
            WorkInfo.State.FAILED -> "Download Failed: Reason: ${downloadEntity?.errorReason}"
            WorkInfo.State.CANCELLED -> getCancelledMessage(downloadEntity)
            else -> null
        }
        message?.let { logger.log("FileName: ${downloadEntity?.fileName}, ID: ${downloadEntity?.id} - $it") }
    }

    /**
     * Retrieves a message for the canceled download task.
     *
     * @param downloadEntity The download entity associated with the canceled task.
     * @return A message indicating the cancellation status.
     */
    private fun getCancelledMessage(downloadEntity: DownloadEntity?): String {
        return if (downloadEntity?.action == Action.PAUSE) "Download Paused"
        else "Download Cancelled"
    }

    /**
     * Retrieves a message for the running download task, including the progress percentage.
     *
     * @param downloadEntity The download entity associated with the running task.
     * @param workInfo The WorkInfo object representing the download task.
     * @return A message indicating the download status.
     */
    private fun getRunningMessage(downloadEntity: DownloadEntity?, workInfo: WorkInfo): String {
        return when (workInfo.progress.getString(STATE_KEY)) {
            STARTED_STATUS -> "Download Started"
            PROGRESS_STATUS -> {
                val percent =
                    ((downloadEntity?.downloadedBytes ?: 0) * 100 / (downloadEntity?.totalBytes
                        ?: 1)).toInt()
                "Download in Progress, $percent%"
            }

            else -> ""
        }
    }

    /**
     * Updates an existing download entry with the new work request details.
     *
     * @param downloadEntity The existing download entity.
     * @param workRequest The new work request associated with the download.
     */
    private suspend fun updateExistingDownload(
        downloadEntity: DownloadEntity,
        workRequest: OneTimeWorkRequest
    ) {
        if (downloadEntity.uuid != workRequest.id.toString() && !downloadEntity.isInProgress()) {
            downloadDao.update(
                downloadEntity.copy(
                    uuid = workRequest.id.toString(),
                    currentStatus = Status.QUEUED
                )
            )
        }
    }

    /**
     * Retrieves a download entity from the database based on the work request UUID.
     *
     * @param uuid The UUID of the work request.
     * @return The corresponding DownloadEntity or null if not found.
     */
    private suspend fun getDownloadWorkEntityFromUUID(uuid: UUID): DownloadEntity? {
        return downloadDao.getAllDownload().find { it.uuid == uuid.toString() }
    }

    /**
     * Checks whether the download entity's current status is "in progress".
     *
     * @return True if the download status is "in progress", false otherwise.
     */
    private fun DownloadEntity.isInProgress(): Boolean {
        return currentStatus == Status.IN_PROGRESS
    }

    /**
     * Initiates a download task by creating a OneTimeWorkRequest and enqueuing it.
     *
     * @param fileDownloadRequest The request containing the details for the download task.
     */
    private suspend fun download(fileDownloadRequest: FileDownloadRequest) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val inputData = Data.Builder()
            .putString(DOWNLOAD_REQUEST_KEY, fileDownloadRequest.toJson())
            .putString(NOTIFICATION_CONFIG_KEY, notificationConfig.toJson())
            .build()
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(DOWNLOAD_TAG)
            .setConstraints(constraints)
            .build()

        downloadDao.get(fileDownloadRequest.requestId)
            ?.let { updateExistingDownload(downloadEntity = it, downloadWorkRequest) }
        if (downloadDao.get(id = fileDownloadRequest.requestId) == null) {
            insertNewDownload(
                fileDownloadRequest = fileDownloadRequest,
                workRequest = downloadWorkRequest
            )
        }
        workManager.enqueueUniqueWork(
            fileDownloadRequest.requestId.toString(),
            ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    /**
     * Inserts a new download entry into the database and schedules it for execution.
     *
     * @param fileDownloadRequest The request containing the details for the download task.
     * @param workRequest The OneTimeWorkRequest created for the download task.
     */
    private suspend fun insertNewDownload(
        fileDownloadRequest: FileDownloadRequest,
        workRequest: OneTimeWorkRequest
    ) {
        fileDownloadRequest.apply {
            deleteFileIfExists(destinationPath, outputFileName)
            downloadDao.insert(
                DownloadEntity(
                    filePath = destinationPath,
                    url = downloadUrl,
                    fileName = outputFileName,
                    tag = requestTag,
                    id = requestId,
                    headerJson = hashMapToJson(headers = requestHeaders),
                    queueTime = System.currentTimeMillis(),
                    currentStatus = Status.QUEUED,
                    uuid = workRequest.id.toString(),
                    action = Action.START,
                    modifiedTime = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Updates the action of a download task (e.g., pause, cancel, resume, retry).
     *
     * @param id The ID of the download task to update.
     * @param action The action to perform on the task.
     */
    private suspend fun updateUserAction(id: Int, action: String) {
        val download = downloadDao.get(id)

        if (download != null) {
            download.copy(action = action, modifiedTime = System.currentTimeMillis())
                .let {
                    downloadDao.update(it)
                }
            when (action) {
                Action.CANCEL -> workManager.cancelUniqueWork(id.toString())
                Action.PAUSE -> workManager.cancelUniqueWork(id.toString())
                Action.RESUME -> download(fileDownloadRequest = download.toFileDownloadRequest())
                Action.RETRY -> download(fileDownloadRequest = download.toFileDownloadRequest())
                else -> {
                    // do nothing
                }
            }
        }
    }

    /**
     * Performs a specified action on all downloads with the given tag.
     *
     * @param tag The tag identifying the group of downloads.
     * @param action The action to perform on each download.
     */
    private suspend fun performActionOnAllWithTag(tag: String, action: suspend (Int) -> Unit) {
        downloadDao.getAllEntityByTag(tag).forEach {
            action(it.id)
        }
    }

    /**
     * Retrieves a download entity from the database based on the work request UUID.
     *
     * @param uuid The UUID of the work request.
     * @return The corresponding DownloadEntity or null if not found.
     */
    private suspend fun findDownloadEntityFromUUID(uuid: UUID): DownloadEntity? {
        return downloadDao.getAllDownload().find { it.uuid == uuid.toString() }
    }

    // The public methods to interact with the download manager, launching corresponding actions asynchronously.

    fun downloadAsync(fileDownloadRequest: FileDownloadRequest) = launch {
        download(fileDownloadRequest)
    }

    fun resumeAsync(id: Int) = launch {
        updateUserAction(id = id, action = Action.RESUME)
    }

    fun resumeAsync(tag: String) = launch {
        performActionOnAllWithTag(tag = tag) { resumeAsync(it) }
    }

    fun cancelAsync(id: Int) = launch {
        updateUserAction(id = id, action = Action.CANCEL)
    }

    fun cancelAsync(tag: String) = launch {
        performActionOnAllWithTag(tag) { cancelAsync(it) }
    }

    fun pauseAsync(id: Int) = launch {
        updateUserAction(id = id, action = Action.PAUSE)
    }

    fun pauseAsync(tag: String) = launch { performActionOnAllWithTag(tag = tag) { pauseAsync(it) } }

    fun retryAsync(id: Int) = launch { updateUserAction(id = id, action = Action.RETRY) }

    fun retryAsync(tag: String) = launch { performActionOnAllWithTag(tag = tag) { retryAsync(it) } }

    fun clearDbAsync(id: Int, deleteFile: Boolean) =
        launch { clearDownload(id = id, deleteFile = deleteFile) }

    fun clearDbAsync(tag: String, deleteFile: Boolean) =
        launch { performActionOnAllWithTag(tag) { clearDbAsync(it, deleteFile) } }

    /**
     * Clears a download task from the database, cancels its work, and optionally deletes the downloaded file.
     *
     * @param id The ID of the download task to clear.
     * @param deleteFile Whether to delete the downloaded file associated with the task.
     */
    private suspend fun clearDownload(id: Int, deleteFile: Boolean) {
        workManager.cancelUniqueWork(id.toString())
        downloadDao.get(id)?.let { downloadEntity ->
            if (deleteFile) deleteFileIfExists(
                downloadEntity.filePath,
                downloadEntity.fileName
            )
            removeNotification(context = context, notificationId = id)
            downloadDao.remove(id)
        }
    }

    // Observers for download progress

    /**
     * Observes the progress of a download by its ID.
     *
     * @param id The ID of the download task.
     * @return A Flow that emits the download progress as a DownloadModel.
     */
    fun observeDownloadById(id: Int) =
        downloadDao.getEntityByIdFlow(id).filterNotNull().distinctUntilChanged()
            .map { it.toDownloadModel() }

    /**
     * Observes the progress of all downloads with a specific tag.
     *
     * @param tag The tag identifying the group of downloads.
     * @return A Flow that emits the list of DownloadModel representing the downloads.
     */
    fun observeDownloadsByTag(tag: String): Flow<List<DownloadModel>> {
        return downloadDao.getAllEntityByTagFlow(tag)
            .map { it.map(DownloadEntity::toDownloadModel) }
    }

    /**
     * Observes the progress of all downloads.
     *
     * @return A Flow that emits the list of DownloadModel representing all downloads.
     */
    fun observeAllDownloads(): Flow<List<DownloadModel>> = downloadDao.getAllDownloadFlow()
        .map { entityList -> entityList.map { it.toDownloadModel() } }
}
