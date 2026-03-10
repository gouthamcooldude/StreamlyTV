package com.streamlytv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_queue")
data class ScanQueueItem(
    @PrimaryKey
    val streamId: Int,             // VOD stream ID from Xtream
    val title: String = "",
    val addedTimestamp: Long = 0,  // Unix timestamp from Xtream "added" field
    val scanStatus: String = STATUS_PENDING, // pending, scanned, failed, no_data
    val scanAttempts: Int = 0,
    val lastAttemptAt: Long = 0
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SCANNED = "scanned"
        const val STATUS_FAILED = "failed"
        const val STATUS_NO_DATA = "no_data"

        // 30 days in seconds
        const val THIRTY_DAYS_SECS = 30L * 24 * 60 * 60
    }
}
