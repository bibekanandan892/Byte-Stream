package com.bibek.bytestream.internal.utils

import com.bibek.bytestream.DownloadModel
import com.bibek.bytestream.internal.database.DownloadEntity
import com.bibek.bytestream.internal.download.FileDownloadRequest

/**
 * Extension function to convert a [DownloadEntity] to a [DownloadModel].
 *
 * This function maps fields from the database entity [DownloadEntity] to a [DownloadModel]
 * that can be used to represent download information in the app.
 *
 * @receiver [DownloadEntity] The database entity representing the download details.
 * @return [DownloadModel] The data model containing download details for use in the app.
 */
internal fun DownloadEntity.toDownloadModel(): DownloadModel {
    // Possible statuses that a download can have.
    val statuses = listOf(
        Status.QUEUED,
        Status.STARTED,
        Status.IN_PROGRESS,
        Status.SUCCESS,
        Status.CANCELLED,
        Status.FAILED,
        Status.PAUSED,
        Status.IN_PROGRESS
    )

    return DownloadModel(
        url = url,  // URL of the file to be downloaded
        path = filePath,  // Destination path where the file is saved
        fileName = fileName,  // Name of the downloaded file
        tag = tag,  // Tag associated with the download
        id = id,  // Unique identifier for the download
        headers = jsonToHashMap(headerJson),  // Headers for the download request
        timeQueued = queueTime,  // Time when the download was added to the queue
        status = statuses.find { it == currentStatus } ?: Status.DEFAULT,  // Current status of the download
        total = totalBytes,  // Total size of the file in bytes
        progress = if (totalBytes.toInt() != 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,  // Download progress as a percentage
        speedInBytePerMs = speedPerMs,  // Download speed in bytes per millisecond
        lastModified = modifiedTime,  // Last modified timestamp of the file
        eTag = eTag,  // ETag associated with the download
        metaData = metadata,  // Additional metadata for the download
        failureReason = errorReason  // Reason for failure if download fails
    )
}

/**
 * Extension function to convert a [DownloadEntity] to a [FileDownloadRequest].
 *
 * This function maps fields from the database entity [DownloadEntity] to a [FileDownloadRequest],
 * which is used to initiate or manage a download request.
 *
 * @receiver [DownloadEntity] The database entity representing the download details.
 * @return [FileDownloadRequest] The request object used to manage or initiate a file download.
 */
internal fun DownloadEntity.toFileDownloadRequest(): FileDownloadRequest {
    return FileDownloadRequest(
        downloadUrl = url,  // URL of the file to be downloaded
        destinationPath = filePath,  // Destination path where the file will be saved
        outputFileName = fileName,  // Name of the file to be saved
        requestTag = tag,  // Tag associated with the download request
        requestId = id,  // Unique identifier for the download request
        requestHeaders = jsonToHashMap(headerJson),  // Headers for the download request
        metadata = metadata  // Additional metadata for the download request
    )
}
