package com.streamlytv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val language: String = "",
    val epgId: String = "",
    val isFavorite: Boolean = false,
    val is4K: Boolean = false,
    val is51: Boolean = false
) {
    companion object {
        // Detect 4K from channel name or group
        fun detect4K(name: String, group: String, url: String): Boolean {
            val combined = "$name $group $url".uppercase()
            return combined.contains("4K") ||
                   combined.contains("UHD") ||
                   combined.contains("2160") ||
                   combined.contains("ULTRA HD")
        }

        // Detect 5.1 from channel name or group
        fun detect51(name: String, group: String): Boolean {
            val combined = "$name $group".uppercase()
            return combined.contains("5.1") ||
                   combined.contains("AC3") ||
                   combined.contains("EAC3") ||
                   combined.contains("DOLBY") ||
                   combined.contains("SURROUND") ||
                   combined.contains("DTS")
        }

        // Detect language from channel name or group
        fun detectLanguage(name: String, group: String): String {
            val combined = "$name $group".lowercase()
            return when {
                combined.contains("telugu") || combined.contains("తెలుగు") -> "Telugu"
                combined.contains("hindi") -> "Hindi"
                combined.contains("tamil") -> "Tamil"
                combined.contains("kannada") -> "Kannada"
                combined.contains("malayalam") -> "Malayalam"
                combined.contains("france") || combined.contains("french") || combined.contains("fr |") -> "French"
                combined.contains("arabic") || combined.contains("arab") -> "Arabic"
                combined.contains("spanish") || combined.contains("espanol") -> "Spanish"
                combined.contains("german") || combined.contains("deutsch") -> "German"
                combined.contains("| us") || combined.contains("usa") ||
                combined.contains("| uk") || combined.contains("canada") ||
                combined.contains("english") -> "English"
                else -> "English" // Default to English
            }
        }
    }
}
