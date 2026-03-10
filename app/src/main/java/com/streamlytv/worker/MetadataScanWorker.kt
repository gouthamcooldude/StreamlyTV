package com.streamlytv.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.streamlytv.data.model.ScanQueueItem
import com.streamlytv.data.model.ScanQueueItem.Companion.STATUS_FAILED
import com.streamlytv.data.model.ScanQueueItem.Companion.STATUS_NO_DATA
import com.streamlytv.data.model.ScanQueueItem.Companion.STATUS_SCANNED
import com.streamlytv.data.model.ScanQueueItem.Companion.THIRTY_DAYS_SECS
import com.streamlytv.data.model.StreamMetadata
import com.streamlytv.data.model.XtreamVodItem
import com.streamlytv.data.repository.AppDatabase
import com.streamlytv.data.repository.XtreamRepository
import com.streamlytv.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MetadataScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "MetadataScan"
        const val WORK_NAME = "streamlytv_metadata_scan"
        const val KEY_ITEMS_SCANNED = "items_scanned"
        const val KEY_ITEMS_FOUND_51 = "items_found_51"
        const val KEY_ITEMS_FOUND_4K = "items_found_4k"

        // Delay between API calls to avoid hammering provider server
        const val RATE_LIMIT_MS = 500L

        // Max items per single worker run
        const val BATCH_SIZE = 30

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
                .setRequiresDeviceIdle(true)                   // Only when idle
                .setRequiresBatteryNotLow(true)                // Not on low battery
                .build()

            val request = PeriodicWorkRequestBuilder<MetadataScanWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "Background scan scheduled")
        }

        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<MetadataScanWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "Immediate scan triggered")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    private val db = AppDatabase.getInstance(applicationContext)
    private val scanQueueDao = db.scanQueueDao()
    private val metadataDao = db.streamMetadataDao()
    private val prefs = PrefsManager(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val server = prefs.xtreamServer ?: run {
            Log.d(TAG, "No Xtream server configured, skipping scan")
            return@withContext Result.success()
        }

        val xtream = XtreamRepository(server)

        Log.d(TAG, "Starting metadata scan...")

        // Step 1: Refresh VOD list and populate scan queue with last 30 days
        try {
            refreshScanQueue(xtream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh scan queue: ${e.message}")
            return@withContext Result.retry()
        }

        // Step 2: Process pending items newest-first
        val cutoff = (System.currentTimeMillis() / 1000) - THIRTY_DAYS_SECS
        val batch = scanQueueDao.getNextBatch(cutoff, BATCH_SIZE)

        if (batch.isEmpty()) {
            Log.d(TAG, "No pending items to scan")
            return@withContext Result.success(
                workDataOf(KEY_ITEMS_SCANNED to 0)
            )
        }

        Log.d(TAG, "Scanning ${batch.size} items...")

        var scanned = 0
        var found51 = 0
        var found4K = 0

        for (item in batch) {
            // Check if cancelled
            if (isStopped) break

            try {
                val result = scanItem(xtream, item)
                when (result) {
                    ScanResult.SUCCESS -> { scanned++; }
                    ScanResult.FOUND_51 -> { scanned++; found51++ }
                    ScanResult.FOUND_4K -> { scanned++; found4K++ }
                    ScanResult.FOUND_BOTH -> { scanned++; found51++; found4K++ }
                    ScanResult.NO_DATA -> {} // counted as attempted
                    ScanResult.FAILED -> {} // will retry next run
                }

                // Rate limit — be polite to the server
                delay(RATE_LIMIT_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning item ${item.streamId}: ${e.message}")
                scanQueueDao.updateStatus(item.streamId, STATUS_FAILED)
            }
        }

        Log.d(TAG, "Scan complete: $scanned scanned, $found51 with 5.1, $found4K in 4K")

        Result.success(
            workDataOf(
                KEY_ITEMS_SCANNED to scanned,
                KEY_ITEMS_FOUND_51 to found51,
                KEY_ITEMS_FOUND_4K to found4K
            )
        )
    }

    private suspend fun refreshScanQueue(xtream: XtreamRepository) {
        val nowSecs = System.currentTimeMillis() / 1000
        val cutoffSecs = nowSecs - THIRTY_DAYS_SECS

        // Fetch all VOD items
        val allVod = xtream.getVodStreams()

        // Filter to last 30 days only
        val recentVod = allVod.filter { item ->
            val added = item.added.toLongOrNull() ?: 0L
            added >= cutoffSecs
        }

        Log.d(TAG, "Found ${recentVod.size} VOD items from last 30 days (total: ${allVod.size})")

        // Build scan queue items
        val queueItems = recentVod.map { item ->
            ScanQueueItem(
                streamId = item.streamId,
                title = item.name,
                addedTimestamp = item.added.toLongOrNull() ?: 0L
            )
        }

        // Insert — IGNORE conflict so we don't reset already-scanned items
        if (queueItems.isNotEmpty()) {
            scanQueueDao.insertAll(queueItems)
        }
    }

    private suspend fun scanItem(xtream: XtreamRepository, item: ScanQueueItem): ScanResult {
        val vodInfo = xtream.getVodInfo(item.streamId)

        if (vodInfo == null) {
            scanQueueDao.updateStatus(item.streamId, STATUS_FAILED)
            return ScanResult.FAILED
        }

        // Try to extract from movie_data info section
        val info = vodInfo.info
        val movieData = vodInfo.movieData

        // Parse audio/video from info if available
        var videoCodec = ""
        var videoWidth = 0
        var videoHeight = 0
        var audioCodec = ""
        var audioChannels = 0
        var audioLayout = ""
        var durationSecs = 0L

        if (info != null) {
            durationSecs = info.durationSecs.toLong()
        }

        // Some providers embed video/audio in episode info style
        // Check name-based hints as supplemental signal
        val nameLower = item.title.lowercase()
        val nameHas4K = nameLower.contains("4k") || nameLower.contains("uhd") || nameLower.contains("2160")
        val nameHas51 = nameLower.contains("5.1") || nameLower.contains("ac3") ||
                        nameLower.contains("eac3") || nameLower.contains("dolby") ||
                        nameLower.contains("dts")

        // If provider gives us no structured metadata, fall back to name hints
        val is4K = when {
            videoHeight >= 2160 -> true
            videoHeight == 0 && nameHas4K -> true
            else -> false
        }

        val is51 = when {
            audioChannels >= 6 -> true
            audioChannels == 0 && nameHas51 -> true
            else -> false
        }

        val resolution = when {
            videoHeight >= 2160 -> "4K"
            videoHeight >= 1080 -> "1080p"
            videoHeight >= 720 -> "720p"
            videoHeight > 0 -> "SD"
            is4K -> "4K"
            else -> ""
        }

        // Only save if we have something meaningful
        val hasData = videoHeight > 0 || audioChannels > 0 || is4K || is51 || durationSecs > 0

        if (!hasData) {
            scanQueueDao.updateStatus(item.streamId, STATUS_NO_DATA)
            return ScanResult.NO_DATA
        }

        val metadata = StreamMetadata(
            streamId = "vod_${item.streamId}",
            streamType = "vod",
            videoCodec = videoCodec,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            resolution = resolution,
            audioCodec = audioCodec,
            audioChannels = audioChannels,
            audioLayout = audioLayout.ifEmpty {
                when (audioChannels) {
                    2 -> "Stereo"; 6 -> "5.1"; 8 -> "7.1"
                    else -> if (audioChannels > 0) "${audioChannels}ch" else ""
                }
            },
            durationSecs = durationSecs,
            is4K = is4K,
            is51 = is51
        )

        metadataDao.insert(metadata)
        scanQueueDao.updateStatus(item.streamId, STATUS_SCANNED)

        return when {
            is4K && is51 -> ScanResult.FOUND_BOTH
            is4K -> ScanResult.FOUND_4K
            is51 -> ScanResult.FOUND_51
            else -> ScanResult.SUCCESS
        }
    }

    enum class ScanResult {
        SUCCESS, FOUND_51, FOUND_4K, FOUND_BOTH, NO_DATA, FAILED
    }
}
