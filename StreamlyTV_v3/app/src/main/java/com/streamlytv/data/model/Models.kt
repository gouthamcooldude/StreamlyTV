package com.streamlytv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val url: String,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val category: String = ""
) {
    val isLive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startTime..endTime
        }

    val progressPercent: Int
        get() {
            val now = System.currentTimeMillis()
            if (now < startTime) return 0
            if (now > endTime) return 100
            val total = endTime - startTime
            val elapsed = now - startTime
            return ((elapsed.toFloat() / total.toFloat()) * 100).toInt()
        }
}

data class AppSettings(
    val languageFilters: Set<String> = setOf("English", "Telugu"),
    val show4KOnly: Boolean = false,
    val show51Only: Boolean = false,
    val useHardwareDecoder: Boolean = true,
    val audioPassthrough: Boolean = true,
    val bufferSizeMs: Int = 10000,
    val epgUrl: String = ""
)
