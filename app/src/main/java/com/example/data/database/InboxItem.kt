package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

object ItemType {
    const val TEXT = "TEXT"
    const val URL = "URL"
    const val IMAGE = "IMAGE"
    const val PDF = "PDF"
    const val AUDIO = "AUDIO"
    const val VIDEO = "VIDEO"
    const val DOCUMENT = "DOCUMENT"
    const val FILE = "FILE"
}

@Entity(tableName = "inbox_items")
data class InboxItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // TEXT, URL, IMAGE, PDF, AUDIO, VIDEO, DOCUMENT, FILE
    val title: String,
    val text: String = "",
    val url: String = "",
    val localFilePath: String? = null,
    val mimeType: String = "text/plain",
    val originalFileName: String? = null,
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceApp: String = "",
    val folderId: Long? = null,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val checksum: String? = null,
    val tags: String = "" // comma separated tag names
)
