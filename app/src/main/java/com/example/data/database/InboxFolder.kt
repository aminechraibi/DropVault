package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class InboxFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tags")
data class InboxTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
