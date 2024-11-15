package com.bibek.bytestream.internal.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bibek.bytestream.internal.utils.Status
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the downloads table in the database.
 * This interface contains methods to insert, update, retrieve, and delete download entities.
 */
@Dao
interface DownloadDao {

    /**
     * Inserts a download entity into the downloads table.
     * If a conflict occurs (e.g., same primary key), it replaces the existing entity.
     * @param downloadEntity The entity to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadEntity: DownloadEntity)

    /**
     * Updates the byte count and status of a specific part of the download entity by its ID.
     * The byte count is updated differently based on the specified part number.
     * @param bytes The number of bytes to add.
     * @param id The ID of the download entity.
     * @param part The part number (1 or 2) to update.
     */
    @Transaction
    suspend fun updateBytes(bytes: Long, id: Int, part: Int) {
        if (part == 1) {
            get(id)?.copy(
                downloadedBytes = get(id)?.downloadedBytes?.plus(bytes) ?: 0,
                part1Bytes = get(id)?.part1Bytes?.plus(bytes) ?: 0,
                currentStatus = if (get(id)?.currentStatus == Status.PAUSED) Status.PAUSED else Status.IN_PROGRESS
            )?.let { update(it) }
        } else {
            get(id)?.copy(
                downloadedBytes = get(id)?.downloadedBytes?.plus(bytes) ?: 0,
                part2Bytes = get(id)?.part2Bytes?.plus(bytes) ?: 0,
                currentStatus = if (get(id)?.currentStatus == Status.PAUSED) Status.PAUSED else Status.PAUSED
            )?.let { update(it) }
        }
    }

    /**
     * Updates the byte counts for multiple parts of a download entity identified by its ID.
     * @param id The ID of the download entity.
     * @param part1 - part5 Bytes for each respective part, default is 0.
     */
    @Query("UPDATE downloads SET part1Bytes = :part1, part2Bytes = :part2, part3Bytes = :part3, part4Bytes = :part4, part5Bytes = :part5 WHERE id = :id")
    suspend fun updatePartDownloadBytes(
        id: Int,
        part1: Long = 0,
        part2: Long = 0,
        part3: Long = 0,
        part4: Long = 0,
        part5: Long = 0
    )

    /**
     * Retrieves the byte count of a specific part in a download entity by part number and ID.
     * @param id The ID of the download entity.
     * @param partNumber The part number (1-5).
     * @return The byte count of the specified part.
     */
    @Query(
        """
        SELECT CASE 
            WHEN :partNumber = 1 THEN part1Bytes
            WHEN :partNumber = 2 THEN part2Bytes
            WHEN :partNumber = 3 THEN part3Bytes
            WHEN :partNumber = 4 THEN part4Bytes
            WHEN :partNumber = 5 THEN part5Bytes
            ELSE 0
        END
        FROM downloads WHERE id = :id
    """
    )
    suspend fun getPartBytes(id: Int, partNumber: Int): Long

    /**
     * Adds bytes to a specific part of a download entity and updates its status and download speed.
     * @param id The ID of the download entity.
     * @param partNumber The part number (1-5).
     * @param bytes The number of bytes to add.
     * @param speed The download speed.
     * @param status The status of the download.
     */
    @Query(
        """
        UPDATE downloads 
        SET 
            downloadedBytes = downloadedBytes + :bytes,
            speedPerMs = :speed,
            currentStatus = :status,
            part1Bytes = CASE WHEN :partNumber = 1 THEN part1Bytes + :bytes ELSE part1Bytes END,
            part2Bytes = CASE WHEN :partNumber = 2 THEN part2Bytes + :bytes ELSE part2Bytes END,
            part3Bytes = CASE WHEN :partNumber = 3 THEN part3Bytes + :bytes ELSE part3Bytes END,
            part4Bytes = CASE WHEN :partNumber = 4 THEN part4Bytes + :bytes ELSE part4Bytes END,
            part5Bytes = CASE WHEN :partNumber = 5 THEN part5Bytes + :bytes ELSE part5Bytes END
        WHERE id = :id
    """
    )
    suspend fun addBytesToPart(
        id: Int,
        partNumber: Int,
        bytes: Long,
        speed: Float,
        status: String
    )

    // The following methods update byte counts for individual parts.
    @Query("UPDATE downloads SET part1Bytes = :part1 WHERE id = :id")
    suspend fun updatePart1Bytes(id: Int, part1: Long)

    @Query("UPDATE downloads SET part2Bytes = :part2 WHERE id = :id")
    suspend fun updatePart2Bytes(id: Int, part2: Long)

    @Query("UPDATE downloads SET part3Bytes = :part3 WHERE id = :id")
    suspend fun updatePart3Bytes(id: Int, part3: Long)

    @Query("UPDATE downloads SET part4Bytes = :part4 WHERE id = :id")
    suspend fun updatePart4Bytes(id: Int, part4: Long)

    @Query("UPDATE downloads SET part5Bytes = :part5 WHERE id = :id")
    suspend fun updatePart5Bytes(id: Int, part5: Long)

    // The following methods add bytes to each specific part.
    @Query("UPDATE downloads SET part1Bytes = part1Bytes + :bytes WHERE id = :id")
    suspend fun addToPart1Bytes(id: Int, bytes: Long)

    @Query("UPDATE downloads SET part2Bytes = part2Bytes + :bytes WHERE id = :id")
    suspend fun addToPart2Bytes(id: Int, bytes: Long)

    @Query("UPDATE downloads SET part3Bytes = part3Bytes + :bytes WHERE id = :id")
    suspend fun addToPart3Bytes(id: Int, bytes: Long)

    @Query("UPDATE downloads SET part4Bytes = part4Bytes + :bytes WHERE id = :id")
    suspend fun addToPart4Bytes(id: Int, bytes: Long)

    @Query("UPDATE downloads SET part5Bytes = part5Bytes + :bytes WHERE id = :id")
    suspend fun addToPart5Bytes(id: Int, bytes: Long)

    /**
     * Retrieves all download entities with a specific tag, sorted by queue time in ascending order.
     * @param tag The tag of the download entities to retrieve.
     * @return A list of download entities.
     */
    @Query("SELECT * FROM downloads WHERE tag = :tag ORDER BY queueTime ASC")
    suspend fun getAllEntityByTag(tag: String): List<DownloadEntity>

    /**
     * Updates the specified download entity in the downloads table.
     * @param downloadEntity The download entity to update.
     */
    @Update
    suspend fun update(downloadEntity: DownloadEntity)

    /**
     * Retrieves a download entity by its ID.
     * @param id The ID of the download entity.
     * @return The download entity or null if not found.
     */
    @Query("SELECT * FROM downloads where id = :id")
    suspend fun get(id: Int): DownloadEntity?

    /**
     * Removes a download entity by its ID.
     * @param id The ID of the download entity to delete.
     */
    @Query("DELETE FROM downloads where id = :id")
    suspend fun remove(id: Int)

    /**
     * Deletes all download entities in the downloads table.
     */
    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    /**
     * Retrieves a flow of a specific download entity by ID, sorted by queue time.
     * @param id The ID of the download entity.
     * @return A flow emitting the download entity.
     */
    @Query("SELECT * FROM downloads WHERE id = :id ORDER BY queueTime ASC")
    fun getEntityByIdFlow(id: Int): Flow<DownloadEntity>

    /**
     * Retrieves a flow of all download entities sorted by queue time in ascending order.
     * @return A flow emitting the list of download entities.
     */
    @Query("SELECT * FROM downloads ORDER BY queueTime ASC")
    fun getAllDownloadFlow(): Flow<List<DownloadEntity>>

    /**
     * Retrieves a flow of download entities with a specific tag, sorted by queue time.
     * @param tag The tag of the download entities to retrieve.
     * @return A flow emitting the list of download entities.
     */
    @Query("SELECT * FROM downloads WHERE tag = :tag ORDER BY queueTime ASC")
    fun getAllEntityByTagFlow(tag: String): Flow<List<DownloadEntity>>

    /**
     * Retrieves all download entities sorted by queue time in ascending order.
     * @return A list of all download entities.
     */
    @Query("SELECT * FROM downloads ORDER BY queueTime ASC")
    suspend fun getAllDownload(): List<DownloadEntity>
}
