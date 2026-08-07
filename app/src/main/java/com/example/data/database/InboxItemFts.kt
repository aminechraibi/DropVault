package com.example.data.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = InboxItem::class)
@Entity(tableName = "inbox_items_fts")
data class InboxItemFts(
    @PrimaryKey
    val rowid: Int,
    val title: String,
    val text: String,
    val url: String,
    val originalFileName: String?,
    val tags: String
)
