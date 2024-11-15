package com.bibek.bytestream

import android.app.Application
import com.bibek.bytestream.R

/**
 * Custom Application class that initializes and configures the ByteStream library.
 * This class is responsible for setting up ByteStream when the application starts.
 */
class DownloadApplication : Application() {

    // Instance of ByteStream that will be initialized in onCreate
    lateinit var byteStream: ByteStream

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects have been created. Initializes the ByteStream instance
     * and configures its notification settings.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize the ByteStream instance with application context and configure notifications
        byteStream = ByteStream.create(applicationContext) {
            configureNotification {
                enabled = true                // Enable notifications for ByteStream
                smallIcon = R.drawable.download_icon  // Set the icon for the notification
            }
        }
    }
}
