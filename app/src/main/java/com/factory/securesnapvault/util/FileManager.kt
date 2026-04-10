package com.factory.securesnapvault.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.factory.securesnapvault.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportResult(
    val vaultPath: String,
    val thumbnailPath: String?,
    val fileName: String,
    val originalFileName: String,
    val mediaType: MediaType,
    val fileSize: Long,
    val mimeType: String,
    val duration: Long
)

class FileManager(private val context: Context) {

    private val vaultDir: File
        get() = File(context.filesDir, "vault").also { if (!it.exists()) it.mkdirs() }

    private val thumbnailDir: File
        get() = File(context.filesDir, "thumbnails").also { if (!it.exists()) it.mkdirs() }

    suspend fun importMedia(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val originalFileName = getFileName(uri) ?: "unknown"
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val mediaType = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType) ?: originalFileName.substringAfterLast('.', "jpg")
        val uniqueName = "${UUID.randomUUID()}.$extension"

        val destFile = File(vaultDir, uniqueName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

        val thumbnailPath = generateThumbnail(destFile, mediaType, uniqueName)
        val duration = if (mediaType == MediaType.VIDEO) getVideoDuration(destFile) else 0L

        ImportResult(
            vaultPath = destFile.absolutePath,
            thumbnailPath = thumbnailPath,
            fileName = uniqueName,
            originalFileName = originalFileName,
            mediaType = mediaType,
            fileSize = destFile.length(),
            mimeType = mimeType,
            duration = duration
        )
    }

    private fun generateThumbnail(file: File, mediaType: MediaType, baseName: String): String? {
        return try {
            val thumbName = "thumb_${baseName.substringBeforeLast('.')}.jpg"
            val thumbFile = File(thumbnailDir, thumbName)

            val bitmap = when (mediaType) {
                MediaType.IMAGE -> {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)

                    val scaleFactor = maxOf(
                        options.outWidth / 300,
                        options.outHeight / 300,
                        1
                    )
                    val scaleOptions = BitmapFactory.Options().apply {
                        inSampleSize = scaleFactor
                    }
                    BitmapFactory.decodeFile(file.absolutePath, scaleOptions)
                }
                MediaType.VIDEO -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        retriever.getFrameAtTime(1000000)
                    } finally {
                        retriever.release()
                    }
                }
            }

            bitmap?.let {
                FileOutputStream(thumbFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                it.recycle()
                thumbFile.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getVideoDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    fun deleteVaultFile(vaultPath: String, thumbnailPath: String?) {
        File(vaultPath).delete()
        thumbnailPath?.let { File(it).delete() }
    }

    fun getVaultFile(vaultPath: String): File = File(vaultPath)

    fun getVaultStorageUsed(): Long {
        return vaultDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
