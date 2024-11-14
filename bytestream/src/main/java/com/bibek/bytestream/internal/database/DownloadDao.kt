package com.bibek.bytestream.internal.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bibek.bytestream.internal.utils.Status
import kotlinx.coroutines.flow.Flow


@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadEntity: DownloadEntity)

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

    @Query("UPDATE downloads SET part1Bytes = :part1, part2Bytes = :part2, part3Bytes = :part3, part4Bytes = :part4, part5Bytes = :part5 WHERE id = :id")
    suspend fun updatePartDownloadBytes(
        id: Int,
        part1: Long = 0,
        part2: Long = 0,
        part3: Long = 0,
        part4: Long = 0,
        part5: Long = 0
    )

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

    @Query("SELECT * FROM downloads WHERE tag = :tag ORDER BY queueTime ASC")
    suspend fun getAllEntityByTag(tag: String): List<DownloadEntity>

    @Update
    suspend fun update(downloadEntity: DownloadEntity)

    @Query("SELECT * FROM downloads where id = :id")
    suspend fun get(id: Int): DownloadEntity?

    @Query("DELETE FROM downloads where id = :id")
    suspend fun remove(id: Int)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("SELECT * FROM downloads WHERE id = :id ORDER BY queueTime ASC")
    fun getEntityByIdFlow(id: Int): Flow<DownloadEntity>

    @Query("SELECT * FROM downloads ORDER BY queueTime ASC")
    fun getAllDownloadFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE tag = :tag ORDER BY queueTime ASC")
    fun getAllEntityByTagFlow(tag: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY queueTime ASC")
    suspend fun getAllDownload(): List<DownloadEntity>
}