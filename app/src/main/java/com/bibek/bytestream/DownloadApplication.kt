package com.bibek.bytestream

import android.app.Application

class DownloadApplication : Application() {

    lateinit var byteStream: ByteStream

    override fun onCreate() {
        super.onCreate()
        byteStream = ByteStream.create(applicationContext) {
            configureNotification {
                enabled = true
                smallIcon = R.drawable.download_icon
            }
        }
    }
}