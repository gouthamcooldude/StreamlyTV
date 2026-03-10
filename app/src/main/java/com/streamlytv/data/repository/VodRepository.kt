package com.streamlytv.data.repository

import android.content.Context
import com.streamlytv.data.model.*
import com.streamlytv.utils.PrefsManager
import kotlinx.coroutines.flow.Flow

class VodRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val metadataDao = db.streamMetadataDao()
    private val vodStateDao = db.vodStateDao()
    private val prefs = PrefsManager(context)

    fun getXtreamRepo(): XtreamRepository? {
        val s = prefs.xtreamServer ?: return null
        return XtreamRepository(s)
    }

    // ── Metadata (auto-detected on first play) ────────────────────────────────

    suspend fun getMetadata(streamId: String): StreamMetadata? =
        metadataDao.get(streamId)

    suspend fun saveMetadata(metadata: StreamMetadata) =
        metadataDao.insert(metadata)

    fun get4KStreams(): Flow<List<StreamMetadata>> = metadataDao.getAll4K()
    fun get51Streams(): Flow<List<StreamMetadata>> = metadataDao.getAll51()

    // ── VOD State ─────────────────────────────────────────────────────────────

    suspend fun getOrCreateState(streamId: String, title: String, posterUrl: String): VodState {
        return vodStateDao.get(streamId) ?: VodState(
            streamId = streamId,
            title = title,
            posterUrl = posterUrl
        ).also { vodStateDao.insert(it) }
    }

    suspend fun updateProgress(streamId: String, posMs: Long, durMs: Long) {
        vodStateDao.updateProgress(streamId, posMs, durMs, System.currentTimeMillis())
        // Auto-mark watched if >90% complete
        if (durMs > 0 && posMs.toFloat() / durMs > 0.90f) {
            vodStateDao.markWatched(streamId)
        }
    }

    suspend fun toggleLike(streamId: String, title: String, posterUrl: String) {
        val state = getOrCreateState(streamId, title, posterUrl)
        val newLiked = !state.isLiked
        vodStateDao.setRating(streamId, liked = newLiked, disliked = false)
    }

    suspend fun toggleDislike(streamId: String, title: String, posterUrl: String) {
        val state = getOrCreateState(streamId, title, posterUrl)
        val newDisliked = !state.isDisliked
        vodStateDao.setRating(streamId, liked = false, disliked = newDisliked)
    }

    suspend fun markWatched(streamId: String) = vodStateDao.markWatched(streamId)

    fun observeState(streamId: String): Flow<VodState?> = vodStateDao.observe(streamId)

    fun getLiked(): Flow<List<VodState>> = vodStateDao.getLiked()
    fun getWatched(): Flow<List<VodState>> = vodStateDao.getWatched()
    fun getRecent(): Flow<List<VodState>> = vodStateDao.getRecent()
}
