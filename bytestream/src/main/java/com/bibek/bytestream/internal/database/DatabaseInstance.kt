package com.bibek.bytestream.internal.database

import android.content.Context
import androidx.room.Room
import com.bibek.bytestream.internal.utils.Constant.DATABASE_NAME

/**
 * Singleton object to provide a single instance of the [DownloadDatabase] throughout the app.
 * This class ensures that the database instance is created only once, and it uses
 * synchronized blocks to prevent concurrent access in a multi-threaded environment.
 */
internal object DatabaseInstance {

    // Volatile ensures the most up-to-date value of INSTANCE is visible to all threads
    @Volatile
    private var INSTANCE: DownloadDatabase? = null

    /**
     * Returns a singleton instance of the [DownloadDatabase].
     * If the instance is not created yet, it creates a new instance using Room's database builder.
     * The database will be named "Kookie Database", and it will fallback to a destructive migration
     * in case of version mismatches.
     *
     * @param context The application context used to access the database.
     * @return The singleton instance of the [DownloadDatabase].
     */
    fun getInstance(context: Context): DownloadDatabase {
        // Double-check locking pattern to ensure thread-safety
        if (INSTANCE == null) {
            synchronized(DownloadDatabase::class) {
                // Second check within the synchronized block to prevent multiple instances
                if (INSTANCE == null) {
                    // Initialize the database instance with Room database builder
                    INSTANCE = Room.databaseBuilder(
                        context = context,
                        name = DATABASE_NAME,
                        klass = DownloadDatabase::class.java
                    )
                        // Allows Room to recreate the database if there's a migration issue
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
        }
        return INSTANCE!!  // Return the instance, assuming it's initialized
    }
}
