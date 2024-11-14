package com.bibek.bytestream.internal.database

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
internal abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}