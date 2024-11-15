package com.bibek.bytestream

import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE
import com.bibek.bytestream.internal.utils.Constant.DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME
import kotlinx.serialization.Serializable

/**
 * A configuration class for setting up notification properties such as enabling notifications,
 * setting channel name, description, importance, and display options.
 *
 * This class uses the Builder pattern to allow flexible configuration.
 *
 * @property enabled Whether the notifications are enabled.
 * @property channelName Name of the notification channel.
 * @property channelDescription Description of the notification channel.
 * @property importance Importance level for the notification channel.
 * @property showSpeed Whether to display speed information in the notification.
 * @property showSize Whether to display size information in the notification.
 * @property showTime Whether to display time information in the notification.
 * @property smallIcon Resource ID for the small icon displayed in the notification.
 */
@Serializable
class NotificationConfig private constructor(
    val enabled: Boolean = false,
    val channelName: String = DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME,
    val channelDescription: String = DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION,
    val importance: Int = DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE,
    val showSpeed: Boolean = true,
    val showSize: Boolean = true,
    val showTime: Boolean = true,
    val smallIcon: Int
) {
    /**
     * Builder class for creating instances of [NotificationConfig].
     *
     * Provides a fluent API for configuring the [NotificationConfig] parameters.
     */
    class Builder {
        private var _enabled: Boolean = false
        private var _channelName: String = DEFAULT_VALUE_NOTIFICATION_CHANNEL_NAME
        private var _channelDescription: String = DEFAULT_VALUE_NOTIFICATION_CHANNEL_DESCRIPTION
        private var _importance: Int = DEFAULT_VALUE_NOTIFICATION_CHANNEL_IMPORTANCE
        private var _showSpeed: Boolean = true
        private var _showSize: Boolean = true
        private var _showTime: Boolean = true
        private var _smallIcon: Int = R.drawable.ic_launcher_background

        /**
         * @property enabled Configures whether notifications are enabled.
         */
        var enabled: Boolean
            get() = _enabled
            set(value) {
                _enabled = value
            }

        /**
         * @property channelName Configures the name of the notification channel.
         */
        var channelName: String
            get() = _channelName
            set(value) {
                _channelName = value
            }

        /**
         * @property channelDescription Configures the description of the notification channel.
         */
        var channelDescription: String
            get() = _channelDescription
            set(value) {
                _channelDescription = value
            }

        /**
         * @property importance Sets the importance level for the notification channel.
         */
        var importance: Int
            get() = _importance
            set(value) {
                _importance = value
            }

        /**
         * @property showSpeed Determines if speed information is shown in the notification.
         */
        var showSpeed: Boolean
            get() = _showSpeed
            set(value) {
                _showSpeed = value
            }

        /**
         * @property showSize Determines if size information is shown in the notification.
         */
        var showSize: Boolean
            get() = _showSize
            set(value) {
                _showSize = value
            }

        /**
         * @property showTime Determines if time information is shown in the notification.
         */
        var showTime: Boolean
            get() = _showTime
            set(value) {
                _showTime = value
            }

        /**
         * @property smallIcon Sets the resource ID for the small icon in the notification.
         */
        var smallIcon: Int
            get() = _smallIcon
            set(value) {
                _smallIcon = value
            }

        /**
         * Builds a [NotificationConfig] instance with the configured properties.
         *
         * @return a configured instance of [NotificationConfig].
         */
        fun build(): NotificationConfig {
            return NotificationConfig(
                enabled = _enabled,
                channelName = _channelName,
                channelDescription = _channelDescription,
                importance = _importance,
                showSpeed = _showSpeed,
                showSize = _showSize,
                showTime = _showTime,
                smallIcon = _smallIcon
            )
        }
    }
}
