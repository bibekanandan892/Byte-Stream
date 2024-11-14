package com.bibek.bytestream.internal.database

import android.content.Context
import androidx.room.Room

internal object DatabaseInstance {
    @Volatile
    private var INSTANCE: DownloadDatabase? = null

    fun getInstance(context: Context): DownloadDatabase {
        if (INSTANCE == null) {
            synchronized(DownloadDatabase::class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context = context,
                        name = "Kookie Database",
                        klass = DownloadDatabase::class.java
                    ).fallbackToDestructiveMigration().build()
                }
            }
        }
        return INSTANCE!!
    }
}