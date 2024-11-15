package com.bibek.bytestream

/**
 * A model representing a download item with various attributes related to the download state.
 *
 * @property url The URL from where the file will be downloaded.
 * @property path The file system path where the file will be saved.
 * @property fileName The name of the file being downloaded.
 * @property tag A tag for categorizing or identifying the download.
 * @property id A unique identifier for the download task.
 * @property headers Additional HTTP headers required for the download request.
 * @property timeQueued The time (in milliseconds) when the download was added to the queue.
 * @property status The current status of the download (e.g., "Pending," "In Progress," "Completed," "Failed").
 * @property total The total size of the file being downloaded (in bytes).
 * @property progress The current progress of the download, typically represented as a percentage (0 to 100).
 * @property speedInBytePerMs The download speed, measured in bytes per millisecond.
 * @property lastModified The last modified timestamp of the file on the server.
 * @property eTag The ETag (entity tag) value for the file, used for caching and versioning.
 * @property metaData Additional metadata related to the download.
 * @property failureReason The reason for download failure, if applicable.
 */
data class DownloadModel(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int,
    val headers: HashMap<String, String>,
    val timeQueued: Long,
    val status: String,
    val total: Long,
    val progress: Int,
    val speedInBytePerMs: Float,
    val lastModified: Long,
    val eTag: String,
    val metaData: String,
    val failureReason: String
)
