package com.bibek.bytestream.internal.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bibek.bytestream.internal.utils.Action
import com.bibek.bytestream.internal.utils.Status
import kotlinx.serialization.Serializable


@Serializable
@Entity(tableName = "downloads")
data class DownloadEntity(
    var url: String = "",
    var filePath: String = "",
    var fileName: String = "",
    var tag: String = "",
    @PrimaryKey
    var id: Int = 0,
    var headerJson: String = "",
    var queueTime: Long = 0,
    var currentStatus: String = Status.DEFAULT,
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var part1Bytes: Long = 0,
    var part2Bytes: Long = 0,
    var part3Bytes: Long = 0,
    var part4Bytes: Long = 0,
    var part5Bytes: Long = 0,
    var speedPerMs: Float = 0f,
    var uuid: String = "",
    var modifiedTime: Long = 0,
    var eTag: String = "",
    var action: String = Action.DEFAULT,
    var metadata: String = "",
    var errorReason: String = ""
)
