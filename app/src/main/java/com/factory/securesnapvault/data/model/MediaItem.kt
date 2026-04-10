package com.factory.securesnapvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE,
    VIDEO
}

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val originalFileName: String,
    val mediaType: MediaType,
    val vaultPath: String,
    val thumbnailPath: String? = null,
    val fileSize: Long = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val mimeType: String = "",
    val duration: Long = 0
)
