package com.bibek.bytestream

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.WorkManager
import com.bibek.bytestream.internal.database.DatabaseInstance
import com.bibek.bytestream.internal.download.DownloadManager
import com.bibek.bytestream.internal.download.FileDownloadRequest
import com.bibek.bytestream.internal.utils.DownloadLogger
import com.bibek.bytestream.internal.utils.getFileNameFromUrl
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Singleton class responsible for managing file downloads within the app.
 *
 * @property context The application context.
 * @property downloadConfig Configuration for download timeouts.
 * @property notificationConfig Configuration for notifications during downloads.
 * @property logger Logger for tracking download events.
 * @property okHttpClient The HTTP client used for making download requests.
 */
class ByteStream private constructor(
    private val context: Context,
    private val downloadConfig: DownloadTimeout,
    private val notificationConfig: NotificationConfig,
    private val logger: Logger,
    private val okHttpClient: OkHttpClient
) {

    /**
     * Builder class for creating an instance of ByteStream with custom configurations.
     */
    class Builder {
        private var _notificationConfig: NotificationConfig = NotificationConfig.Builder().build()
        private var _downloadTimeout: DownloadTimeout = DownloadTimeout.Builder().build()
        private lateinit var _logger: Logger
        private lateinit var _okHttpClient: OkHttpClient

        /**
         * Configures the notification settings for downloads.
         */
        fun configureNotification(notificationConfig: NotificationConfig.Builder.() -> Unit) {
            _notificationConfig = NotificationConfig.Builder().apply(notificationConfig).build()
        }

        /**
         * Configures the timeout settings for downloads.
         */
        fun configureDownloadTimeout(downloadTimeout: DownloadTimeout.Builder.() -> Unit) {
            _downloadTimeout = DownloadTimeout.Builder().apply(downloadTimeout).build()
        }

        /**
         * Logger to be used for tracking events during downloads.
         */
        var logger: Logger
            get() = _logger
            set(value) {
                _logger = value
            }

        /**
         * HTTP client to be used for downloading files.
         */
        var okHttpClient: OkHttpClient
            get() = _okHttpClient
            set(value) {
                _okHttpClient = value
            }

        /**
         * Builds an instance of ByteStream.
         *
         * @param context Application context for managing resources.
         * @return An instance of ByteStream.
         */
        fun build(context: Context): ByteStream {
            if (!::_okHttpClient.isInitialized) {
                okHttpClient = OkHttpClient
                    .Builder()
                    .connectTimeout(_downloadTimeout.connectTimeOutInMs, TimeUnit.MILLISECONDS)
                    .readTimeout(_downloadTimeout.readTimeOutInMs, TimeUnit.MILLISECONDS)
                    .build()
            }
            if (!::_logger.isInitialized) {
                _logger = DownloadLogger(false)
            }
            return ByteStream(
                context = context.applicationContext,
                downloadConfig = _downloadTimeout,
                notificationConfig = _notificationConfig,
                logger = _logger,
                okHttpClient = _okHttpClient
            )
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ByteStream? = null

        /**
         * Creates and initializes a singleton instance of ByteStream.
         *
         * @param context Application context.
         * @param builder A lambda to configure the ByteStream instance.
         * @return A singleton instance of ByteStream.
         */
        fun create(context: Context, builder: Builder.() -> Unit): ByteStream {
            return instance ?: synchronized(this) {
                instance ?: Builder().apply(builder).build(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Retrieves the singleton instance of ByteStream, if available.
         */
        internal fun getInstance(): ByteStream? = instance

        /**
         * Provides the OkHttpClient used for making HTTP requests in downloads.
         */
        internal fun getOkHttpClient(): OkHttpClient = instance!!.okHttpClient
    }

    private val downloadManager by lazy {
        DownloadManager(
            context,
            DatabaseInstance.getInstance(context).downloadDao(),
            WorkManager.getInstance(context),
            downloadConfig,
            notificationConfig,
            logger
        )
    }

    /**
     * Initiates a file download.
     *
     * @param url URL of the file to download.
     * @param path Destination path where the file will be saved.
     * @param fileName Optional custom name for the downloaded file.
     * @param tag Optional tag to identify the download request.
     * @param metaData Optional metadata associated with the download.
     * @param headers Optional headers to be sent with the download request.
     * @return A unique ID representing the download request.
     */
    fun download(
        url: String, path: String, fileName: String = getFileNameFromUrl(url),
        tag: String = "", metaData: String = "", headers: HashMap<String, String> = hashMapOf()
    ) =
        FileDownloadRequest(
            downloadUrl = url,
            destinationPath = path,
            outputFileName = fileName,
            requestTag = tag,
            metadata = metaData,
            requestHeaders = headers
        ).apply {
            downloadManager.downloadAsync(this)
        }.requestId

    /**
     * Cancels an ongoing download by its ID.
     *
     * @param id The unique ID of the download request.
     */
    fun cancel(id: Int) = downloadManager.cancelAsync(id = id)

    /**
     * Cancels an ongoing download by its tag.
     *
     * @param tag The tag associated with the download request.
     */
    fun cancel(tag: String) = downloadManager.cancelAsync(tag)

    /**
     * Pauses an ongoing download by its ID.
     *
     * @param id The unique ID of the download request.
     */
    fun pause(id: Int) = downloadManager.pauseAsync(id)

    /**
     * Pauses an ongoing download by its tag.
     *
     * @param tag The tag associated with the download request.
     */
    fun pause(tag: String) = downloadManager.pauseAsync(tag)

    /**
     * Resumes a paused download by its ID.
     *
     * @param id The unique ID of the download request.
     */
    fun resume(id: Int) = downloadManager.resumeAsync(id = id)

    /**
     * Resumes a paused download by its tag.
     *
     * @param tag The tag associated with the download request.
     */
    fun resume(tag: String) = downloadManager.resumeAsync(tag)

    /**
     * Retries a failed download by its ID.
     *
     * @param id The unique ID of the download request.
     */
    fun retry(id: Int) = downloadManager.retryAsync(id = id)

    /**
     * Retries a failed download by its tag.
     *
     * @param tag The tag associated with the download request.
     */
    fun retry(tag: String) = downloadManager.retryAsync(tag)

    /**
     * Clears the download entry from the database.
     *
     * @param id The unique ID of the download request.
     * @param deleteFile If true, the downloaded file will also be deleted.
     */
    fun clearDb(id: Int, deleteFile: Boolean = true) =
        downloadManager.clearDbAsync(id, deleteFile)

    /**
     * Observes all download events, returning a flow of download models.
     *
     * @return A flow of list of DownloadModel, providing updates on download events.
     */
    fun observeDownloads(): Flow<List<DownloadModel>> = downloadManager.observeAllDownloads()

}
