package com.bibek.bytestream.internal.utils
import android.os.Environment
import android.webkit.URLUtil
import java.io.File
import java.security.MessageDigest
import java.util.UUID

fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
    // Combine the URL, directory path, and filename into a single string with separators
    val combinedString = "$url${File.separator}$dirPath${File.separator}$fileName"

    val hashBytes: ByteArray = try {
        // Generate an MD5 hash from the combined string
        MessageDigest.getInstance("MD5").digest(combinedString.toByteArray(Charsets.UTF_8))
    } catch (_: Exception) {
        // If an error occurs, fallback to a custom hash calculation
        return getUniqueIdFallback(url, dirPath, fileName)
    }

    // Convert the MD5 byte array to a hexadecimal string
    val hexString = hashBytes.joinToString("") { byte ->
        "%02x".format(byte)  // Ensures each byte is two hexadecimal digits
    }

    // Return the hash code of the hexadecimal string as a unique integer ID
    return hexString.hashCode()
}

// Fallback function for unique ID generation in case of hashing error
private fun getUniqueIdFallback(url: String, dirPath: String, fileName: String): Int {
    // Use a custom hash formula to generate a unique ID from the inputs
    return (url.hashCode() * 31 + dirPath.hashCode()) * 31 + fileName.hashCode()
}

fun getTemporaryFile(file: File): File {
    return File(file.absolutePath + ".temp")
}

fun deleteFileIfExists(path: String, name: String) {
    val file = File(path, name)
    if (file.exists()) {
        file.delete()
    }

    getTemporaryFile(file).let {
        if (it.exists()) it.delete()
    }
}

fun getDefaultDownloadPath(): String {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
}

fun getFileNameFromUrl(url: String): String {
    val guessFileName = URLUtil.guessFileName(url, null, null)
    return UUID.randomUUID().toString() + "-" + guessFileName
}