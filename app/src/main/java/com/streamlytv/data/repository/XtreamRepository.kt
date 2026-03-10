package com.streamlytv.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamlytv.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XtreamRepository(private val server: XtreamServer) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── Categories ────────────────────────────────────────────────────────────

    suspend fun getVodCategories(): List<XtreamCategory> = withContext(Dispatchers.IO) {
        fetch(server.apiUrl("get_vod_categories"))
    }

    suspend fun getSeriesCategories(): List<XtreamCategory> = withContext(Dispatchers.IO) {
        fetch(server.apiUrl("get_series_categories"))
    }

    suspend fun getLiveCategories(): List<XtreamCategory> = withContext(Dispatchers.IO) {
        fetch(server.apiUrl("get_live_categories"))
    }

    // ── VOD ───────────────────────────────────────────────────────────────────

    suspend fun getVodStreams(categoryId: String? = null): List<XtreamVodItem> = withContext(Dispatchers.IO) {
        val url = if (categoryId != null)
            server.apiUrl("get_vod_streams") + "&category_id=$categoryId"
        else
            server.apiUrl("get_vod_streams")
        fetch(url)
    }

    suspend fun getVodInfo(streamId: Int): XtreamVodInfo? = withContext(Dispatchers.IO) {
        fetchSingle(server.apiUrl("get_vod_info") + "&vod_id=$streamId")
    }

    // ── Series ────────────────────────────────────────────────────────────────

    suspend fun getSeriesStreams(categoryId: String? = null): List<XtreamSeriesItem> = withContext(Dispatchers.IO) {
        val url = if (categoryId != null)
            server.apiUrl("get_series") + "&category_id=$categoryId"
        else
            server.apiUrl("get_series")
        fetch(url)
    }

    suspend fun getSeriesInfo(seriesId: Int): XtreamSeriesInfo? = withContext(Dispatchers.IO) {
        fetchSingle(server.apiUrl("get_series_info") + "&series_id=$seriesId")
    }

    // ── Live ──────────────────────────────────────────────────────────────────

    suspend fun getLiveStreams(categoryId: String? = null): List<XtreamVodItem> = withContext(Dispatchers.IO) {
        val url = if (categoryId != null)
            server.apiUrl("get_live_streams") + "&category_id=$categoryId"
        else
            server.apiUrl("get_live_streams")
        fetch(url)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private inline fun <reified T> fetch(url: String): List<T> {
        return try {
            val json = httpGet(url)
            val type = TypeToken.getParameterized(List::class.java, T::class.java).type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> fetchSingle(url: String): T? {
        return try {
            val json = httpGet(url)
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        return response.body?.string() ?: throw Exception("Empty response")
    }
}
