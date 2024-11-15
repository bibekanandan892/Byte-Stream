package com.bibek.bytestream.internal.utils

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.bibek.bytestream.DownloadTimeout
import com.bibek.bytestream.NotificationConfig
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_SMALL_NOTIFICATION_ICON
import com.bibek.bytestream.internal.download.FileDownloadRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Utility class for download-related calculations and operations.
 */
internal object DownloadUtil {

    private const val MAX_PERCENT = 100
    private const val MS_IN_SEC = 1000
    private const val KB = 1024
    private const val KB_THRESHOLD = 500

    /**
     * Utility class for time calculations related to download progress.
     */
    internal class TimeUtil {

        companion object {
            private const val SECONDS_IN_MINUTE = 60
            private const val MINUTES_IN_HOUR = 60

            /**
             * Calculates the remaining time for a download based on current speed and progress.
             *
             * @param speedInBytesPerMs Current download speed in bytes per millisecond.
             * @param progressPercent Download progress as a percentage.
             * @param totalBytes Total size of the file in bytes.
             * @return Estimated time remaining in a human-readable format.
             */
            fun calculateRemainingTime(
                speedInBytesPerMs: Float,
                progressPercent: Int,
                totalBytes: Long
            ): String {
                if (speedInBytesPerMs == 0F) return ""

                val speedInBytesPerSec = speedInBytesPerMs * MS_IN_SEC
                val bytesRemaining =
                    (totalBytes * (MAX_PERCENT - progressPercent) / MAX_PERCENT).toFloat()
                val timeLeftInSec = bytesRemaining / speedInBytesPerSec
                val timeLeftInMin = timeLeftInSec / SECONDS_IN_MINUTE
                val timeLeftInHour = timeLeftInMin / SECONDS_IN_MINUTE

                return when {
                    timeLeftInSec < SECONDS_IN_MINUTE -> "%.0f s left".format(timeLeftInSec)
                    timeLeftInMin < SECONDS_IN_MINUTE -> "%.0f mins %.0f s left".format(
                        timeLeftInMin,
                        timeLeftInSec % SECONDS_IN_MINUTE
                    )
                    timeLeftInMin < KB_THRESHOLD -> "%.0f mins left".format(timeLeftInMin)
                    timeLeftInHour < KB_THRESHOLD -> "%.0f hrs %.0f mins left".format(
                        timeLeftInHour,
                        timeLeftInMin % MINUTES_IN_HOUR
                    )
                    else -> "%.0f hrs left".format(timeLeftInHour)
                }
            }
        }
    }

    /**
     * Utility class for speed calculations related to download progress.
     */
    internal class SpeedUtil {

        companion object {
            /**
             * Formats download speed in a human-readable format.
             *
             * @param speedInBytesPerMs Current download speed in bytes per millisecond.
             * @return Speed formatted with appropriate units (b/s, kb/s, mb/s, etc.).
             */
            fun formatSpeed(speedInBytesPerMs: Float): String {
                var value = speedInBytesPerMs * MS_IN_SEC
                val units = arrayOf("b/s", "kb/s", "mb/s", "gb/s")
                var unitIndex = 0

                while (value >= KB_THRESHOLD && unitIndex < units.size - 1) {
                    value /= KB
                    unitIndex++
                }

                return "%.2f %s".format(value, units[unitIndex])
            }
        }
    }

    /**
     * Utility class for formatting file lengths.
     */
    internal class LengthUtil {

        companion object {
            /**
             * Formats file size in a human-readable format.
             *
             * @param totalBytes Total file size in bytes.
             * @return Size formatted with appropriate units (b, kb, mb, etc.).
             */
            fun formatLength(totalBytes: Long): String {
                var value = totalBytes.toFloat()
                val units = arrayOf("b", "kb", "mb", "gb")
                var unitIndex = 0

                while (value >= KB_THRESHOLD && unitIndex < units.size - 1) {
                    value /= KB
                    unitIndex++
                }

                return "%.2f %s".format(value, units[unitIndex])
            }
        }
    }

    /**
     * Retrieves the remaining download time as text.
     */
    fun getRemainingTimeText(
        speedInBytesPerMs: Float,
        progressPercent: Int,
        totalBytes: Long
    ): String {
        return TimeUtil.calculateRemainingTime(speedInBytesPerMs, progressPercent, totalBytes)
    }

    /**
     * Retrieves the current download speed as text.
     */
    fun getSpeedText(speedInBytesPerMs: Float): String {
        return SpeedUtil.formatSpeed(speedInBytesPerMs)
    }

    /**
     * Retrieves the total file size as text.
     */
    fun getTotalLengthText(totalBytes: Long): String {
        return LengthUtil.formatLength(totalBytes)
    }
}

/**
 * Extension function to serialize DownloadTimeout to JSON format.
 */
fun DownloadTimeout.toJson(): String {
    return Json.encodeToString(this)
}

/**
 * Extension function to serialize FileDownloadRequest to JSON format.
 */
fun FileDownloadRequest.toJson(): String {
    return Json.encodeToString(this)
}

/**
 * Deserializes a JSON string into a FileDownloadRequest object.
 */
internal fun jsonToDownloadRequest(jsonStr: String): FileDownloadRequest {
    return Json.decodeFromString(jsonStr)
}

/**
 * Extension function to serialize NotificationConfig to JSON format.
 */
fun NotificationConfig.toJson(): String {
    return Json.encodeToString(this)
}

/**
 * Deserializes a JSON string into a NotificationConfig object.
 * Uses default settings if JSON is empty.
 */
fun jsonToNotificationConfig(jsonStr: String): NotificationConfig {
    if (jsonStr.isEmpty()) {
        return NotificationConfig.Builder().apply {
            smallIcon = DEFAULT_SMALL_NOTIFICATION_ICON
        }.build()
    }
    return Json.decodeFromString(jsonStr)
}

/**
 * Converts a HashMap of headers to a JSON string.
 */
fun hashMapToJson(headers: HashMap<String, String>): String {
    if (headers.isEmpty()) return ""
    return Json.encodeToString(headers)
}

/**
 * Converts a JSON string to a HashMap of headers.
 */
fun jsonToHashMap(jsonString: String): HashMap<String, String> {
    if (jsonString.isEmpty()) return hashMapOf()
    return Json.decodeFromString(jsonString)
}

/**
 * Removes a notification with a given ID.
 *
 * @param context Application context.
 * @param notificationId ID of the notification to remove.
 */
fun removeNotification(context: Context, notificationId: Int) {
    NotificationManagerCompat.from(context).cancel(notificationId)
}
