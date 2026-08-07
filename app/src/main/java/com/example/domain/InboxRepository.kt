package com.example.data.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.data.database.*
import com.example.data.preferences.SettingsRepository
import com.example.data.storage.FileStorageManager
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

sealed class SaveResult {
    data class Success(val item: InboxItem) : SaveResult()
    data class Duplicate(val existingItem: InboxItem) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class InboxRepository(
    private val context: Context,
    private val inboxDao: InboxDao,
    private val fileStorageManager: FileStorageManager,
    private val settingsRepository: SettingsRepository
) {

    fun getAllItems(): Flow<List<InboxItem>> = inboxDao.getAllItems()

    fun getFavoriteItems(): Flow<List<InboxItem>> = inboxDao.getFavoriteItems()

    fun getArchivedItems(): Flow<List<InboxItem>> = inboxDao.getArchivedItems()

    fun getItemsByFolder(folderId: Long): Flow<List<InboxItem>> = inboxDao.getItemsByFolder(folderId)

    fun getItemsByType(type: String): Flow<List<InboxItem>> = inboxDao.getItemsByType(type)

    fun searchItems(query: String, typeFilter: String?, archived: Boolean = false): Flow<List<InboxItem>> =
        inboxDao.searchItems(query, typeFilter, archived)

    suspend fun getItemById(id: Long): InboxItem? = inboxDao.getItemById(id)

    suspend fun saveTextOrUrl(text: String, sourceApp: String = "", folderId: Long? = null, tags: String = ""): InboxItem {
        val trimmed = text.trim()
        val isUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://")
        val type = if (isUrl) ItemType.URL else ItemType.TEXT

        val title = if (isUrl) {
            try {
                val host = java.net.URI(trimmed).host
                if (!host.isNull_or_empty()) "Link: $host" else trimmed.take(50)
            } catch (e: Exception) {
                trimmed.take(50)
            }
        } else {
            val lines = trimmed.lines()
            if (lines.isNotEmpty() && lines[0].isNotBlank()) {
                lines[0].take(60)
            } else {
                "Note " + java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            }
        }

        val item = InboxItem(
            type = type,
            title = title,
            text = if (!isUrl) trimmed else "",
            url = if (isUrl) trimmed else "",
            sourceApp = sourceApp,
            folderId = folderId,
            tags = tags
        )

        val id = inboxDao.insertItem(item)
        return item.copy(id = id)
    }

    suspend fun saveUri(
        contentResolver: ContentResolver,
        uri: Uri,
        sourceApp: String = "",
        overrideMime: String? = null,
        forceKeep: Boolean = false
    ): SaveResult {
        return try {
            val copyResult = fileStorageManager.copyUriToStorage(contentResolver, uri, overrideMime)

            if (!forceKeep) {
                val existing = inboxDao.findByChecksum(copyResult.checksum)
                if (existing != null) {
                    // Delete temp copy since duplicate
                    fileStorageManager.deleteFile(copyResult.file.absolutePath)
                    return SaveResult.Duplicate(existing)
                }
            }

            val itemType = fileStorageManager.detectItemType(copyResult.mimeType, copyResult.originalName, null)
            val title = copyResult.originalName

            val item = InboxItem(
                type = itemType,
                title = title,
                localFilePath = copyResult.file.absolutePath,
                mimeType = copyResult.mimeType,
                originalFileName = copyResult.originalName,
                fileSize = copyResult.size,
                checksum = copyResult.checksum,
                sourceApp = sourceApp
            )

            val id = inboxDao.insertItem(item)
            SaveResult.Success(item.copy(id = id))
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Failed to save file")
        }
    }

    suspend fun saveInputStream(
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        sourceApp: String = "Web Access"
    ): SaveResult {
        return try {
            val copyResult = fileStorageManager.saveInputStream(inputStream, fileName, mimeType)
            val existing = inboxDao.findByChecksum(copyResult.checksum)
            if (existing != null) {
                fileStorageManager.deleteFile(copyResult.file.absolutePath)
                return SaveResult.Duplicate(existing)
            }

            val itemType = fileStorageManager.detectItemType(copyResult.mimeType, copyResult.originalName, null)
            val item = InboxItem(
                type = itemType,
                title = copyResult.originalName,
                localFilePath = copyResult.file.absolutePath,
                mimeType = copyResult.mimeType,
                originalFileName = copyResult.originalName,
                fileSize = copyResult.size,
                checksum = copyResult.checksum,
                sourceApp = sourceApp
            )

            val id = inboxDao.insertItem(item)
            SaveResult.Success(item.copy(id = id))
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Failed to save stream")
        }
    }

    suspend fun deleteItem(item: InboxItem) {
        fileStorageManager.deleteFile(item.localFilePath)
        inboxDao.deleteItemById(item.id)
    }

    suspend fun deleteItems(items: List<InboxItem>) {
        items.forEach { fileStorageManager.deleteFile(it.localFilePath) }
        inboxDao.deleteItemsByIds(items.map { it.id })
    }

    suspend fun updateItem(item: InboxItem) {
        inboxDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavorite(item: InboxItem) {
        updateItem(item.copy(favorite = !item.favorite))
    }

    suspend fun toggleArchive(item: InboxItem) {
        updateItem(item.copy(archived = !item.archived))
    }

    suspend fun moveItemsToFolder(ids: List<Long>, folderId: Long?) {
        inboxDao.moveItemsToFolder(ids, folderId)
    }

    // Storage and Stats
    fun getStorageUsageByType(): Flow<List<TypeStorageUsage>> = inboxDao.getStorageUsageByType()
    fun getTotalCount(): Flow<Int> = inboxDao.getTotalCount()
    fun getTotalInboxSize(): Flow<Long?> = inboxDao.getTotalInboxSize()

    // Folders & Tags
    fun getAllFolders(): Flow<List<InboxFolder>> = inboxDao.getAllFolders()
    suspend fun createFolder(name: String, icon: String = "folder"): Long =
        inboxDao.insertFolder(InboxFolder(name = name, icon = icon))
    suspend fun deleteFolder(folderId: Long) = inboxDao.deleteFolder(folderId)

    fun getAllTags(): Flow<List<InboxTag>> = inboxDao.getAllTags()
    suspend fun createTag(name: String): Long = inboxDao.insertTag(InboxTag(name = name))

    fun clearCache() = fileStorageManager.clearCache()
    fun getCacheSize() = fileStorageManager.getCacheSize()
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
