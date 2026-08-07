package com.example.device

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class SystemStorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
)

class StorageInfoProvider(private val context: Context) {

    fun getSystemStorageStats(): SystemStorageStats {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = total - free

            SystemStorageStats(totalBytes = total, usedBytes = used, freeBytes = free)
        } catch (e: Exception) {
            SystemStorageStats(0L, 0L, 0L)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = String.format("%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }
}
