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

/**
 * This class is responsible for managing the download of a file.
 * It handles checking for existing downloads, resuming partial downloads,
 * updating download progress, and ensuring that the download is consistent with
 * the file on the server (by verifying the ETag header).
 */
internal class DownloadWork(
    private val url: String,
    private val destinationPath: String,
    private val destinationFileName: String,
    private val downloadDao: DownloadDao
) {
    companion object {
        // Interval (in milliseconds) between progress updates
        private const val PROGRESS_UPDATE_INTERVAL = 1500L
    }

    /**
     * Starts the download process for a given file. If the file was partially downloaded previously,
     * it resumes from where it left off. It also updates the download progress periodically.
     *
     * @param requestId The ID associated with the download request.
     * @param headers HTTP headers to send with the request, including any range-related headers.
     * @param onDownloadStart A callback invoked when the download starts, passing the total length to be downloaded.
     * @param onDownloadProgress A callback invoked periodically to update the progress of the download.
     * @return The total number of bytes to download.
     * @throws IOException If an error occurs during the download process.
     */
    suspend fun startDownload(
        requestId: Int,
        headers: MutableMap<String, String> = mutableMapOf(),
        onDownloadStart: suspend (speed: Long) -> Unit,
        onDownloadProgress: suspend (downloadedBytes: Long, totalLength: Long, speed: Float) -> Unit
    ): Long {
        // Retrieve the ETag for the current file from the server
        val latestETag = DownloadService.getHeaderValue(
            url = url, headerKey =
            ETAG_HEADER_KEY, headers = headers
        ) ?: ""
        // Get the stored ETag from the database (if any)
        val existingETag = downloadDao.get(requestId)?.eTag ?: ""

        // If the ETag has changed, reset the download data
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

        // Check if a partial file exists and resume from the last downloaded byte
        if (tempFile.exists()) {
            downloadedBytes = tempFile.length()
            headers[RANGE_HEADER] = "bytes=$downloadedBytes-"  // Set the Range header to resume download
        }

        // Start the download request
        var response = DownloadService.downloadFromUrl(url = url, headers = headers)

        // If the server returns a 416 Range Not Satisfiable error or the download was redirected, reset and try again
        if (response.code == HTTP_STATUS_RANGE_NOT_SATISFIABLE || isRedirected(response.request.url.toString())) {
            deleteFileIfExists(destinationPath, destinationFileName)
            headers.remove(RANGE_HEADER)  // Clear the Range header for a fresh start
            downloadedBytes = 0L
            response = DownloadService.downloadFromUrl(url = url, headers = headers)
        }

        // If the download fails, throw an IOException
        if (!response.isSuccessful || response.body == null) {
            throw IOException("Failed to download file: ${response.message}")
        }

        val responseBody = response.body ?: throw IOException("Error: Response body is null")

        // Get the total content length for the file to be downloaded
        var totalBytesToDownload = responseBody.contentLength().takeIf { it >= 0 }
            ?: throw IOException("Invalid content length")

        totalBytesToDownload += downloadedBytes  // Add already downloaded bytes to the total length

        var progressBytes = 0L

        // Start reading the response body and writing it to a temporary file
        responseBody.byteStream().use { input ->
            FileOutputStream(tempFile, true).use { output ->
                // If we are resuming, set the starting point for progress
                if (downloadedBytes != 0L) {
                    progressBytes = downloadedBytes
                } else {
                    onDownloadStart(totalBytesToDownload)  // Notify start of download
                }

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                var lastUpdateTime = System.currentTimeMillis()

                // Read the file in chunks and write it to the temp file
                while (input.read(buffer).also { bytesRead = it } >= 0) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    progressBytes += bytesRead

                    // Periodically update the progress
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL) {
                        val speed = progressBytes.toFloat() / (currentTime - lastUpdateTime)
                        progressBytes = 0L
                        lastUpdateTime = currentTime
                        onDownloadProgress(downloadedBytes, totalBytesToDownload, speed)
                    }
                }
                // Notify when the download reaches 100%
                onDownloadProgress(totalBytesToDownload, totalBytesToDownload, 0f)
            }
        }

        // Rename the temporary file to the final file once the download is complete
        check(tempFile.renameTo(finalFile)) { "Error: Could not rename temp file" }
        return totalBytesToDownload
    }

    /**
     * Checks if the current URL has been redirected.
     *
     * @param currentUrl The URL of the current request.
     * @return true if the URL has been redirected, false otherwise.
     */
    private fun isRedirected(currentUrl: String): Boolean {
        return currentUrl != url
    }

    /**
     * Resets the download data in the database and deletes any partial files if needed.
     *
     * @param id The ID of the download request.
     * @param latestETag The latest ETag value for the file.
     * @param dirPath The directory where the file is being saved.
     * @param fileName The name of the file being downloaded.
     */
    private suspend fun resetDownloadData(
        id: Int,
        latestETag: String,
        dirPath: String,
        fileName: String
    ) {
        deleteFileIfExists(dirPath, fileName)  // Delete any existing partial file
        // Update the database with the new ETag and modified time
        downloadDao.get(id)?.copy(eTag = latestETag, modifiedTime = System.currentTimeMillis())
            ?.let { updatedEntity ->
                downloadDao.update(updatedEntity)
            }
    }
}
