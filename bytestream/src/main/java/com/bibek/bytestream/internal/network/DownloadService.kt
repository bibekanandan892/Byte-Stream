package com.bibek.bytestream.internal.network

import com.bibek.bytestream.internal.utils.Constant.RANGE_HEADER
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * A service for handling file downloads and fetching HTTP headers using OkHttp.
 * This service provides methods to download data from a URL, retrieve specific HTTP header values,
 * and get the total size of a file available at a URL.
 */
internal object DownloadService {

    // OkHttpClient instance, lazily initialized.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    /**
     * Downloads content from the specified URL using provided headers.
     *
     * @param url The URL to download data from.
     * @param headers A map of headers to include in the request.
     * @return A Response object containing the downloaded data.
     * @throws IOException If there is an error during the download.
     */
    @Throws(IOException::class)
    fun downloadFromUrl(url: String, headers: MutableMap<String, String>): Response {
        // Build the request with the provided headers.
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .build()

        // Execute the request and return the response.
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Retrieves the value of a specified header from the URL response headers.
     * This method sends a HEAD request to fetch only the headers without downloading the content body.
     *
     * @param url The URL to send the request to.
     * @param headerKey The name of the header to retrieve.
     * @param headers A map of headers to include in the request.
     * @return The value of the specified header, or null if the header is not found.
     * @throws IOException If there is an error retrieving the header value.
     */
    fun getHeaderValue(url: String, headerKey: String, headers: Map<String, String>): String? {
        // Build the request with the provided headers.
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .head() // Send a HEAD request to retrieve only headers.
            .build()

        // Execute the request and return the specified header value if successful.
        val response = okHttpClient.newCall(request).execute()

        // Return the header value if the request was successful, else null.
        return if (response.isSuccessful) {
            response.header(headerKey)
        } else {
            null
        }
    }

    /**
     * Retrieves the total size (in bytes) of a file available at a specified URL.
     * This method sends a request with the "Range" header to fetch the content length.
     *
     * @param url The URL of the file to get the size of.
     * @return The total size of the file in bytes.
     * @throws IOException If there is an error retrieving the file size.
     */
    fun getTotalBytes(url: String): Long {
        // Add a "Range" header to request only the first byte, which gives us the content length.
        val headers = mapOf(RANGE_HEADER to "bytes=0-")

        // Build the request to fetch the file size.
        val request = Request.Builder()
            .url(url)
            .headers(Headers.Builder().apply {
                headers.forEach { (key, value) -> add(key, value) }
            }.build())
            .build()

        // Execute the request and retrieve the content length.
        val response: Response = okHttpClient.newCall(request).execute()

        // Return the content length or throw an error if not found.
        return if (response.isSuccessful) {
            response.body?.contentLength() ?: throw IOException("Response body is null")
        } else {
            throw IOException("Failed to fetch file size: ${response.message}")
        }.also {
            // Ensure the response body is closed after usage.
            response.close()
        }
    }
}
