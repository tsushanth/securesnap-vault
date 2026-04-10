package com.factory.securesnapvault.data.repository

import android.content.Context
import android.net.Uri
import com.factory.securesnapvault.data.db.VaultDatabase
import com.factory.securesnapvault.data.model.MediaItem
import com.factory.securesnapvault.data.model.MediaType
import com.factory.securesnapvault.util.FileManager
import kotlinx.coroutines.flow.Flow

class MediaRepository(context: Context) {

    private val database = VaultDatabase.getDatabase(context)
    private val mediaDao = database.mediaDao()
    private val fileManager = FileManager(context)

    fun getAllMedia(): Flow<List<MediaItem>> = mediaDao.getAllMedia()

    fun getMediaByType(type: MediaType): Flow<List<MediaItem>> = mediaDao.getMediaByType(type)

    fun getMediaCount(): Flow<Int> = mediaDao.getMediaCount()

    fun getTotalSize(): Flow<Long?> = mediaDao.getTotalSize()

    suspend fun getMediaById(id: Long): MediaItem? = mediaDao.getMediaById(id)

    suspend fun importMedia(uri: Uri): MediaItem {
        val result = fileManager.importMedia(uri)
        val mediaItem = MediaItem(
            fileName = result.fileName,
            originalFileName = result.originalFileName,
            mediaType = result.mediaType,
            vaultPath = result.vaultPath,
            thumbnailPath = result.thumbnailPath,
            fileSize = result.fileSize,
            mimeType = result.mimeType,
            duration = result.duration
        )
        val id = mediaDao.insertMedia(mediaItem)
        return mediaItem.copy(id = id)
    }

    suspend fun deleteMedia(mediaItem: MediaItem) {
        fileManager.deleteVaultFile(mediaItem.vaultPath, mediaItem.thumbnailPath)
        mediaDao.deleteMedia(mediaItem)
    }

    suspend fun deleteMediaById(id: Long) {
        val item = mediaDao.getMediaById(id) ?: return
        fileManager.deleteVaultFile(item.vaultPath, item.thumbnailPath)
        mediaDao.deleteMediaById(id)
    }
}
