package com.example.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.data.database.ItemType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID

class FileStorageManager(private val context: Context) {

    private val inboxDir: File = File(context.filesDir, "inbox").apply {
        if (!exists()) mkdirs()
    }

    fun getSubDir(type: String): File {
        val folderName = when (type) {
            ItemType.IMAGE -> "images"
            ItemType.AUDIO -> "audio"
            ItemType.VIDEO -> "video"
            ItemType.PDF -> "pdf"
            ItemType.DOCUMENT -> "documents"
            else -> "files"
        }
        return File(inboxDir, folderName).apply {
            if (!exists()) mkdirs()
        }
    }

    fun detectItemType(mimeType: String?, fileName: String?, textContent: String?): String {
        val mime = mimeType?.lowercase() ?: ""
        val name = fileName?.lowercase() ?: ""

        if (textContent != null && (textContent.startsWith("http://") || textContent.startsWith("https://"))) {
            return ItemType.URL
        }

        return when {
            mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") -> ItemType.IMAGE
            mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ogg") -> ItemType.AUDIO
            mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkdirv") || name.endsWith(".webm") || name.endsWith(".mov") -> ItemType.VIDEO
            mime == "application/pdf" || name.endsWith(".pdf") -> ItemType.PDF
            mime.contains("msword") || mime.contains("officedocument") || mime.contains("text/csv") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".md") -> ItemType.DOCUMENT
            textContent != null && textContent.isNotBlank() -> ItemType.TEXT
            else -> ItemType.FILE
        }
    }

    data class CopyResult(
        val file: File,
        val size: Long,
        val checksum: String,
        val originalName: String,
        val mimeType: String
    )

    fun copyUriToStorage(contentResolver: ContentResolver, uri: Uri, overrideMime: String? = null): CopyResult {
        var fileName = getFileNameFromUri(contentResolver, uri) ?: "file_${System.currentTimeMillis()}"
        var mime = overrideMime ?: contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)

        val itemType = detectItemType(mime, fileName, null)
        val targetDir = getSubDir(itemType)

        val safeFileName = "${UUID.randomUUID().toString().take(8)}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
        val targetFile = File(targetDir, safeFileName)

        val digest = MessageDigest.getInstance("SHA-256")
        var fileSize = 0L

        contentResolver.openInputStream(uri)?.use { inputStream ->
            DigestInputStream(inputStream, digest).use { digestStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (digestStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        fileSize += bytesRead
                    }
                }
            }
        }

        val checksum = digest.digest().joinToString("") { "%02x".format(it) }

        return CopyResult(
            file = targetFile,
            size = fileSize,
            checksum = checksum,
            originalName = fileName,
            mimeType = mime
        )
    }

    fun saveInputStream(inputStream: InputStream, originalFileName: String, mimeType: String): CopyResult {
        val itemType = detectItemType(mimeType, originalFileName, null)
        val targetDir = getSubDir(itemType)
        val safeFileName = "${UUID.randomUUID().toString().take(8)}_${originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
        val targetFile = File(targetDir, safeFileName)

        val digest = MessageDigest.getInstance("SHA-256")
        var fileSize = 0L

        DigestInputStream(inputStream, digest).use { digestStream ->
            FileOutputStream(targetFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (digestStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    fileSize += bytesRead
                }
            }
        }

        val checksum = digest.digest().joinToString("") { "%02x".format(it) }

        return CopyResult(
            file = targetFile,
            size = fileSize,
            checksum = checksum,
            originalName = originalFileName,
            mimeType = mimeType
        )
    }

    fun deleteFile(filePath: String?): Boolean {
        if (filePath.isNull_or_empty()) return false
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    fun getCacheSize(): Long {
        return getFolderSize(context.cacheDir)
    }

    fun clearCache(): Boolean {
        return deleteContents(context.cacheDir)
    }

    private fun getFolderSize(file: File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { size += getFolderSize(it) }
        } else {
            size = file.length()
        }
        return size
    }

    private fun deleteContents(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteContents(it) }
        }
        return file.delete()
    }

    private fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { File(it).name }
        }
        return name
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
