package com.bibek.bytestream.internal.utils

import com.bibek.bytestream.DownloadModel
import com.bibek.bytestream.internal.database.DownloadEntity
import com.bibek.bytestream.internal.download.FileDownloadRequest

// Mapper function to convert DownloadEntity to DownloadModel
internal fun DownloadEntity.toDownloadModel(): DownloadModel {
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
        url = url,
        path = filePath,
        fileName = fileName,
        tag = tag,
        id = id,
        headers = jsonToHashMap(headerJson),
        timeQueued = queueTime,
        status = statuses.find { it.toString() == currentStatus } ?: Status.DEFAULT,
        total = totalBytes,
        progress = if (totalBytes.toInt() != 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
        speedInBytePerMs = speedPerMs,
        lastModified = modifiedTime,
        eTag = eTag,
        metaData = metadata,
        failureReason = errorReason
    )
}

internal fun DownloadEntity.toFileDownloadRequest(): FileDownloadRequest {
    return FileDownloadRequest(
        downloadUrl = url,
        destinationPath = filePath,
        outputFileName = fileName,
        requestTag = tag,
        requestId = id,
        requestHeaders = jsonToHashMap(headerJson),
        metadata = metadata
    )
}

