package com.bibek.bytestream.internal.download

import com.bibek.bytestream.internal.database.DownloadDao
import com.bibek.bytestream.internal.network.DownloadService
import com.bibek.bytestream.internal.utils.Constant.ETAG_HEADER_KEY
import com.bibek.bytestream.internal.utils.Constant.HTTP_STATUS_RANGE_NOT_SATISFIABLE
import com.bibek.bytestream.internal.utils.Constant.RANGE_HEADER
import com.bibek.bytestream.internal.utils.deleteFileIfExists
import com.bibek.bytestream.internal.utils.getTemporaryFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class DownloadWork(
    private val url: String,
    private val destinationPath: String,
    private val destinationFileName: String,
    private val downloadDao: DownloadDao
) {
    companion object {
        private const val PROGRESS_UPDATE_INTERVAL = 1500L
    }

    suspend fun startDownload(
        requestId: Int,
        headers: MutableMap<String, String> = mutableMapOf(),
        onDownloadStart: suspend (speed: Long) -> Unit,
        onDownloadProgress: suspend (downloadedBytes: Long, totalLength: Long, speed: Float) -> Unit
    ): Long {
        val latestETag = DownloadService.getHeaderValue(
            url = url, headerKey =
            ETAG_HEADER_KEY, headers = headers
        ) ?: ""
        val existingETag = downloadDao.get(requestId)?.eTag ?: ""
        if (latestETag != existingETag) {
            resetDownloadData(
                requestId,
                latestETag,
                destinationPath,
                fileName = destinationFileName
            )
        }
        var downloadedBytes = 0L
        val finalFile = File(destinationPath, destinationFileName)
        val tempFile = getTemporaryFile(finalFile)

        // Check if a partial file exists and resume
        if (tempFile.exists()) {
            downloadedBytes = tempFile.length()
            headers[RANGE_HEADER] = "bytes=$downloadedBytes-"
        }
        // Make the initial request
        var response = DownloadService.downloadFromUrl(url = url, headers = headers)

        // Handle 416 (Range Not Satisfiable) or redirection cases
        if (response.code == HTTP_STATUS_RANGE_NOT_SATISFIABLE || isRedirected(response.request.url.toString())) {
            deleteFileIfExists(destinationPath, destinationFileName)
            headers.remove(RANGE_HEADER)
            downloadedBytes = 0L
            response = DownloadService.downloadFromUrl(url = url, headers = headers)
        }

        if (!response.isSuccessful || response.body == null) {
            throw IOException("Failed to download file: ${response.message}")
        }

        val responseBody = response.body ?: throw IOException("Error: Response body is null")
        var totalBytesToDownload = responseBody.contentLength().takeIf { it >= 0 }
            ?: throw IOException("Invalid content length")
        totalBytesToDownload += downloadedBytes

        var progressBytes = 0L

        responseBody.byteStream().use { input ->
            FileOutputStream(tempFile, true).use { output ->
                if (downloadedBytes != 0L) {
                    progressBytes = downloadedBytes
                } else {
                    onDownloadStart(totalBytesToDownload)
                }

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                var lastUpdateTime = System.currentTimeMillis()

                while (input.read(buffer).also { bytesRead = it } >= 0) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    progressBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL) {
                        val speed = progressBytes.toFloat() / (currentTime - lastUpdateTime)
                        progressBytes = 0L
                        lastUpdateTime = currentTime
                        onDownloadProgress(downloadedBytes, totalBytesToDownload, speed)
                    }
                }
                onDownloadProgress(totalBytesToDownload, totalBytesToDownload, 0f)
            }
        }
        // Rename temporary file to final file
        check(tempFile.renameTo(finalFile)) { "Error: Could not rename temp file" }
        return totalBytesToDownload
    }

    private fun isRedirected(currentUrl: String): Boolean {
        return currentUrl != url
    }

    private suspend fun resetDownloadData(
        id: Int,
        latestETag: String,
        dirPath: String,
        fileName: String
    ) {
        deleteFileIfExists(dirPath, fileName)
        downloadDao.get(id)?.copy(eTag = latestETag, modifiedTime = System.currentTimeMillis())
            ?.let { updatedEntity ->
                downloadDao.update(updatedEntity)
            }
    }
}
