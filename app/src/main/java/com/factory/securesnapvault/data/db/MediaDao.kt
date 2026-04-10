package com.factory.securesnapvault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.factory.securesnapvault.data.model.MediaItem
import com.factory.securesnapvault.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateAdded DESC")
    fun getMediaByType(type: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaItem: MediaItem): Long

    @Delete
    suspend fun deleteMedia(mediaItem: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    fun getMediaCountByType(type: MediaType): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM media_items")
    fun getTotalSize(): Flow<Long?>
}
