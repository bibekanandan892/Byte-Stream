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

class ByteStream private constructor(
    private val context: Context,
    private val downloadConfig: DownloadTimeout,
    private val notificationConfig: NotificationConfig,
    private val logger: Logger,
    private val okHttpClient: OkHttpClient
) {

    class Builder {
        private var _notificationConfig: NotificationConfig = NotificationConfig.Builder().build()
        private var _downloadTimeout: DownloadTimeout = DownloadTimeout.Builder().build()
        private lateinit var _logger: Logger
        private lateinit var _okHttpClient: OkHttpClient
        fun configureNotification(notificationConfig: NotificationConfig.Builder.() -> Unit) {
            _notificationConfig = NotificationConfig.Builder().apply(notificationConfig).build()
        }

        fun configureDownloadTimeout(downloadTimeout: DownloadTimeout.Builder.() -> Unit) {
            _downloadTimeout = DownloadTimeout.Builder().apply(downloadTimeout).build()
        }

        var logger: Logger
            get() = _logger
            set(value) {
                _logger = value
            }

        var okHttpClient: OkHttpClient
            get() = _okHttpClient
            set(value) {
                _okHttpClient = value
            }

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
        /**
         * The ApplicationContext is tied to the lifecycle of the entire app, not individual activities, so this avoids memory leaks.
         */
        @Volatile
        private var instance: ByteStream? = null

        fun create(context: Context, builder: Builder.() -> Unit): ByteStream {
            return instance ?: synchronized(this) {
                instance ?: Builder().apply(builder).build(context.applicationContext).also {
                    instance = it
                }
            }
        }

        internal fun getInstance(): ByteStream? = instance
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

    fun cancel(id: Int) = downloadManager.cancelAsync(id = id)
    fun cancel(tag: String) = downloadManager.cancelAsync(tag)

    fun pause(id: Int) = downloadManager.pauseAsync(id)
    fun pause(tag: String) = downloadManager.pauseAsync(tag)

    fun resume(id: Int) = downloadManager.resumeAsync(id = id)
    fun resume(tag: String) = downloadManager.resumeAsync(tag)

    fun retry(id: Int) = downloadManager.retryAsync(id = id)
    fun retry(tag: String) = downloadManager.retryAsync(tag)
    fun clearDb(id: Int, deleteFile: Boolean = true) =
        downloadManager.clearDbAsync(id, deleteFile)

    fun observeDownloads(): Flow<List<DownloadModel>> = downloadManager.observeAllDownloads()

}
