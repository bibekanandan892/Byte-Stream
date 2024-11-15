package com.bibek.bytestream.internal.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database that holds the data for downloads.
 *
 * This database contains the [DownloadEntity] table, which stores information about
 * individual downloads. The database version is set to 1.
 *
 * @see DownloadEntity The entity representing a download.
 * @see DownloadDao The DAO (Data Access Object) that provides methods to interact with
 * the database.
 */
@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
internal abstract class DownloadDatabase : RoomDatabase() {
    /**
     * Provides access to the [DownloadDao], which contains the methods for performing
     * database operations on the download data.
     *
     * @return An instance of [DownloadDao].
     */
    abstract fun downloadDao(): DownloadDao
}
