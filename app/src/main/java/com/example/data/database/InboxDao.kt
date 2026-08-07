package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class TypeStorageUsage(
    val type: String,
    val count: Int,
    val totalSize: Long
)

@Dao
interface InboxDao {

    @Query("SELECT * FROM inbox_items WHERE archived = 0 ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE favorite = 1 AND archived = 0 ORDER BY createdAt DESC")
    fun getFavoriteItems(): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE archived = 1 ORDER BY createdAt DESC")
    fun getArchivedItems(): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE folderId = :folderId AND archived = 0 ORDER BY createdAt DESC")
    fun getItemsByFolder(folderId: Long): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE type = :type AND archived = 0 ORDER BY createdAt DESC")
    fun getItemsByType(type: String): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE id = :id")
    suspend fun getItemById(id: Long): InboxItem?

    @Query("""
        SELECT * FROM inbox_items 
        WHERE (title LIKE '%' || :query || '%' 
           OR text LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%' 
           OR originalFileName LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%')
          AND (:typeFilter IS NULL OR type = :typeFilter)
          AND (:archived = archived)
        ORDER BY createdAt DESC
    """)
    fun searchItems(query: String, typeFilter: String?, archived: Boolean = false): Flow<List<InboxItem>>

    @Query("SELECT * FROM inbox_items WHERE checksum = :checksum AND checksum IS NOT NULL LIMIT 1")
    suspend fun findByChecksum(checksum: String): InboxItem?

    @Query("SELECT * FROM inbox_items WHERE originalFileName = :fileName AND fileSize = :fileSize LIMIT 1")
    suspend fun findByFileNameAndSize(fileName: String, fileSize: Long): InboxItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InboxItem): Long

    @Update
    suspend fun updateItem(item: InboxItem)

    @Query("DELETE FROM inbox_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM inbox_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>)

    @Query("UPDATE inbox_items SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveItemsToFolder(ids: List<Long>, folderId: Long?)

    @Query("SELECT type, COUNT(*) as count, SUM(fileSize) as totalSize FROM inbox_items GROUP BY type")
    fun getStorageUsageByType(): Flow<List<TypeStorageUsage>>

    @Query("SELECT COUNT(*) FROM inbox_items WHERE archived = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM inbox_items")
    fun getTotalInboxSize(): Flow<Long?>

    // Folders
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<InboxFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: InboxFolder): Long

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<InboxTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: InboxTag): Long
}
