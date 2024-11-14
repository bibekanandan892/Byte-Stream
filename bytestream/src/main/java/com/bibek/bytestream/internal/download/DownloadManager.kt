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

    private suspend fun observerWorkInfos() {
        workManager.getWorkInfosForUniqueWorkFlow(DOWNLOAD_TAG)
            .flowOn(Dispatchers.IO)
            .collectLatest { workInfos ->
                workInfos.forEach { workInfo ->
                    logWorkInfo(workInfo)
                }
            }
    }

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

    private fun getCancelledMessage(downloadEntity: DownloadEntity?): String {
        return if (downloadEntity?.action == Action.PAUSE) "Download Paused"
        else "Download Cancelled"
    }

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


    private suspend fun getDownloadWorkEntityFromUUID(uuid: UUID): DownloadEntity? {
        return downloadDao.getAllDownload().find { it.uuid == uuid.toString() }
    }

    fun DownloadEntity.isInProgress(): Boolean {
        return currentStatus == Status.IN_PROGRESS
    }

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


    private suspend fun performActionOnAllWithTag(tag: String, action: suspend (Int) -> Unit) {
        downloadDao.getAllEntityByTag(tag).forEach {
            action(it.id)
        }
    }

    private suspend fun findDownloadEntityFromUUID(uuid: UUID): DownloadEntity? {
        return downloadDao.getAllDownload().find { it.uuid == uuid.toString() }
    }

    fun downloadAsync(fileDownloadRequest: FileDownloadRequest) =
        launch { download(fileDownloadRequest) }

    fun resumeAsync(id: Int) = launch { updateUserAction(id = id, action = Action.RESUME) }
    fun resumeAsync(tag: String) =
        launch { performActionOnAllWithTag(tag = tag) { resumeAsync(it) } }

    fun cancelAsync(id: Int) = launch { updateUserAction(id = id, action = Action.CANCEL) }
    fun cancelAsync(tag: String) = launch { performActionOnAllWithTag(tag) { cancelAsync(it) } }
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
    fun observeDownloadById(id: Int) =
        downloadDao.getEntityByIdFlow(id).filterNotNull().distinctUntilChanged()
            .map { it.toDownloadModel() }

    fun observeDownloadsByTag(tag: String): Flow<List<DownloadModel>> {
        return downloadDao.getAllEntityByTagFlow(tag)
            .map { it.map(DownloadEntity::toDownloadModel) }
    }

    fun observeAllDownloads(): Flow<List<DownloadModel>> = downloadDao.getAllDownloadFlow()
        .map { entityList -> entityList.map { it.toDownloadModel() } }


}