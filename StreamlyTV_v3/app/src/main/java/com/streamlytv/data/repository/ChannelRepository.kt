package com.streamlytv.data.repository

import android.content.Context
import com.streamlytv.data.model.Channel
import com.streamlytv.data.model.EpgProgram
import com.streamlytv.data.model.Playlist
import com.streamlytv.utils.EpgParser
import com.streamlytv.utils.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val channelDao = db.channelDao()
    private val playlistDao = db.playlistDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── Channels ──────────────────────────────────────────────────────────────

    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()

    fun getFavorites(): Flow<List<Channel>> = channelDao.getFavorites()

    fun getAllGroups(): Flow<List<String>> = channelDao.getAllGroups()

    fun searchChannels(
        query: String = "",
        language: String = "All",
        only4K: Boolean = false,
        only51: Boolean = false
    ): Flow<List<Channel>> = channelDao.searchChannels(query, language, only4K, only51)

    suspend fun toggleFavorite(channel: Channel) {
        channelDao.setFavorite(channel.id, !channel.isFavorite)
    }

    // ── Playlists ─────────────────────────────────────────────────────────────

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun addPlaylist(name: String, url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = fetchUrl(url)
            val channels = M3UParser.parse(content)
            if (channels.isEmpty()) {
                return@withContext Result.failure(Exception("No channels found in playlist"))
            }
            channelDao.deleteAll()
            channelDao.insertAll(channels)
            val playlist = Playlist(name = name, url = url)
            val id = playlistDao.insert(playlist).toInt()
            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshPlaylist(playlist: Playlist): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = fetchUrl(playlist.url)
            val channels = M3UParser.parse(content)
            channelDao.deleteAll()
            channelDao.insertAll(channels)
            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
        channelDao.deleteAll()
    }

    // ── EPG ───────────────────────────────────────────────────────────────────

    suspend fun fetchEpg(url: String): Map<String, List<EpgProgram>> = withContext(Dispatchers.IO) {
        try {
            val content = fetchUrl(url)
            EpgParser.parse(content)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fetchUrl(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        return response.body?.string() ?: throw Exception("Empty response")
    }

    suspend fun getChannelCount(): Int = channelDao.count()
}
