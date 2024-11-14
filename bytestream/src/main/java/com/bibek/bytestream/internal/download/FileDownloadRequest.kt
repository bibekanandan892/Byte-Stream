package com.bibek.bytestream.internal.download

import com.bibek.bytestream.internal.utils.getUniqueId
import kotlinx.serialization.Serializable

@Serializable
data class FileDownloadRequest(
    val downloadUrl: String = "",
    val destinationPath: String = "",
    val outputFileName: String = "",
    val requestTag: String = "",
    val requestId: Int = getUniqueId(downloadUrl, destinationPath, outputFileName),
    val requestHeaders: HashMap<String, String> = hashMapOf(),
    val additionalInfo: String = "",
    val metadata: String = ""
)
