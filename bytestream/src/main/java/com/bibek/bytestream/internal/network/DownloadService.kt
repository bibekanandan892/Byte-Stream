package com.bibek.bytestream.internal.network

import com.bibek.bytestream.internal.utils.Constant.RANGE_HEADER
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2

internal object DownloadService {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    @Throws(IOException::class)
    fun downloadFromUrl(url: String, headers: MutableMap<String, String>): Response {
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .build()

        return okHttpClient.newCall(request).execute()
    }


    fun getHeaderValue(url: String, headerKey: String, headers: Map<String, String>): String? {
        // Build the request with provided headers
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .head() // HEAD request to only retrieve headers without downloading the body
            .build()

        // Execute the request
        val response = okHttpClient.newCall(request).execute()

        // Return the specified header value, or null if not present
        return if (response.isSuccessful) {
            response.header(headerKey)
        } else {
            null
        }
    }


    fun getTotalBytes(url: String): Long {
        // Add a "Range" header to start from byte 0
        val headers = mapOf(RANGE_HEADER to "bytes=0-")

        // Build and execute the request
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .build()

        val response: Response = okHttpClient.newCall(request).execute()

        // Check if the request was successful and get the content length
        return if (response.isSuccessful) {
            response.body?.contentLength() ?: throw IOException("Response body is null")
        } else {
            throw IOException("Failed to fetch file size: ${response.message}")
        }.also {
            response.close()
        }
    }
}
