package com.bibek.bytestream.internal.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bibek.bytestream.internal.utils.Action
import com.bibek.bytestream.internal.utils.Status
import kotlinx.serialization.Serializable

/**
 * Data class representing a download entity in the database.
 *
 * This class is used to define the structure of a download record,
 * including metadata such as URL, file path, file name, and download status.
 * The entity is annotated with @Entity to define it as a Room database table.
 */
@Serializable
@Entity(tableName = "downloads")
data class DownloadEntity(
    /**
     * URL of the file to be downloaded.
     */
    var url: String = "",

    /**
     * File path where the downloaded file will be saved.
     */
    var filePath: String = "",

    /**
     * Name of the downloaded file.
     */
    var fileName: String = "",

    /**
     * Optional tag to categorize or identify the download.
     */
    var tag: String = "",

    /**
     * Unique identifier for the download record.
     */
    @PrimaryKey
    var id: Int = 0,

    /**
     * JSON string containing additional headers for the download request.
     */
    var headerJson: String = "",

    /**
     * Timestamp when the download was added to the queue.
     */
    var queueTime: Long = 0,

    /**
     * Current status of the download.
     * Uses a default value from the Status enum.
     */
    var currentStatus: String = Status.DEFAULT,

    /**
     * Total size of the file to be downloaded, in bytes.
     */
    var totalBytes: Long = 0,

    /**
     * Number of bytes downloaded so far.
     */
    var downloadedBytes: Long = 0,

    /**
     * Number of bytes downloaded for the first part of a multipart download.
     */
    var part1Bytes: Long = 0,

    /**
     * Number of bytes downloaded for the second part of a multipart download.
     */
    var part2Bytes: Long = 0,

    /**
     * Number of bytes downloaded for the third part of a multipart download.
     */
    var part3Bytes: Long = 0,

    /**
     * Number of bytes downloaded for the fourth part of a multipart download.
     */
    var part4Bytes: Long = 0,

    /**
     * Number of bytes downloaded for the fifth part of a multipart download.
     */
    var part5Bytes: Long = 0,

    /**
     * Download speed in bytes per millisecond.
     */
    var speedPerMs: Float = 0f,

    /**
     * Unique identifier for the download, used to track downloads.
     */
    var uuid: String = "",

    /**
     * Timestamp of the last modification to the download.
     */
    var modifiedTime: Long = 0,

    /**
     * Entity tag (ETag) for caching and versioning of the downloaded file.
     */
    var eTag: String = "",

    /**
     * Action currently being performed on the download.
     * Uses a default value from the Action enum.
     */
    var action: String = Action.DEFAULT,

    /**
     * JSON string containing additional metadata related to the download.
     */
    var metadata: String = "",

    /**
     * Error message describing the reason for a failed download, if any.
     */
    var errorReason: String = ""
)
