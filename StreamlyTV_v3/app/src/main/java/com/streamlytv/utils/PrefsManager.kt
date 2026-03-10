package com.streamlytv.utils

import android.content.Context
import android.content.SharedPreferences
import com.streamlytv.data.model.AppSettings
import com.streamlytv.data.model.XtreamServer

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("streamlytv_prefs", Context.MODE_PRIVATE)

    var settings: AppSettings
        get() = AppSettings(
            languageFilters = prefs.getStringSet("language_filters", setOf("English", "Telugu")) ?: setOf("English", "Telugu"),
            show4KOnly = prefs.getBoolean("show_4k_only", false),
            show51Only = prefs.getBoolean("show_51_only", false),
            useHardwareDecoder = prefs.getBoolean("hw_decoder", true),
            audioPassthrough = prefs.getBoolean("audio_passthrough", true),
            bufferSizeMs = prefs.getInt("buffer_size_ms", 10000),
            epgUrl = prefs.getString("epg_url", "") ?: ""
        )
        set(value) {
            prefs.edit()
                .putStringSet("language_filters", value.languageFilters)
                .putBoolean("show_4k_only", value.show4KOnly)
                .putBoolean("show_51_only", value.show51Only)
                .putBoolean("hw_decoder", value.useHardwareDecoder)
                .putBoolean("audio_passthrough", value.audioPassthrough)
                .putInt("buffer_size_ms", value.bufferSizeMs)
                .putString("epg_url", value.epgUrl)
                .apply()
        }

    var xtreamServer: XtreamServer?
        get() {
            val url = prefs.getString("xtream_url", "") ?: ""
            val user = prefs.getString("xtream_user", "") ?: ""
            val pass = prefs.getString("xtream_pass", "") ?: ""
            return if (url.isNotEmpty() && user.isNotEmpty()) XtreamServer(url, user, pass) else null
        }
        set(value) {
            prefs.edit()
                .putString("xtream_url", value?.serverUrl ?: "")
                .putString("xtream_user", value?.username ?: "")
                .putString("xtream_pass", value?.password ?: "")
                .apply()
        }

    var hasXtreamServer: Boolean get() = xtreamServer != null; set(_) {}

    var lastChannelId: String
        get() = prefs.getString("last_channel_id", "") ?: ""
        set(value) = prefs.edit().putString("last_channel_id", value).apply()

    var lastChannelName: String
        get() = prefs.getString("last_channel_name", "") ?: ""
        set(value) = prefs.edit().putString("last_channel_name", value).apply()

    fun updateLanguageFilter(language: String, enabled: Boolean) {
        val current = settings.languageFilters.toMutableSet()
        if (enabled) current.add(language) else current.remove(language)
        settings = settings.copy(languageFilters = current)
    }
}
