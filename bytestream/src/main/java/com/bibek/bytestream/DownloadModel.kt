package com.bibek.bytestream

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
