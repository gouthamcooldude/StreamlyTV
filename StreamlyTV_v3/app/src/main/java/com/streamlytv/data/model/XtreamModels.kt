package com.streamlytv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// ─── Xtream Codes Server Config ───────────────────────────────────────────────
data class XtreamServer(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    val baseUrl: String get() = serverUrl.trimEnd('/')
    fun apiUrl(action: String) = "$baseUrl/player_api.php?username=$username&password=$password&action=$action"
    fun vodStreamUrl(streamId: Int) = "$baseUrl/movie/$username/$password/$streamId.mp4"
    fun seriesStreamUrl(streamId: Int) = "$baseUrl/series/$username/$password/$streamId.mp4"
    fun liveStreamUrl(streamId: Int) = "$baseUrl/live/$username/$password/$streamId.m3u8"
}

// ─── VOD / Movie ──────────────────────────────────────────────────────────────
data class XtreamVodItem(
    @SerializedName("num") val num: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("stream_type") val streamType: String = "movie",
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("stream_icon") val streamIcon: String = "",
    @SerializedName("rating") val rating: String = "",
    @SerializedName("rating_5based") val rating5: Double = 0.0,
    @SerializedName("added") val added: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4",
    @SerializedName("custom_sid") val customSid: String = "",
    @SerializedName("direct_source") val directSource: String = ""
)

data class XtreamVodInfo(
    @SerializedName("info") val info: VodInfo? = null,
    @SerializedName("movie_data") val movieData: MovieData? = null
)

data class VodInfo(
    @SerializedName("movie_image") val movieImage: String = "",
    @SerializedName("tmdb_id") val tmdbId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("o_name") val originalName: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("genre") val genre: String = "",
    @SerializedName("releasedate") val releaseDate: String = "",
    @SerializedName("youtube_trailer") val youtubeTrailer: String = "",
    @SerializedName("director") val director: String = "",
    @SerializedName("actors") val actors: String = "",
    @SerializedName("cast") val cast: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("duration_secs") val durationSecs: Int = 0,
    @SerializedName("rating") val rating: String = ""
)

data class MovieData(
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("added") val added: String = "",
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4"
)

// ─── Series ───────────────────────────────────────────────────────────────────
data class XtreamSeriesItem(
    @SerializedName("num") val num: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("series_id") val seriesId: Int = 0,
    @SerializedName("cover") val cover: String = "",
    @SerializedName("plot") val plot: String = "",
    @SerializedName("cast") val cast: String = "",
    @SerializedName("director") val director: String = "",
    @SerializedName("genre") val genre: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("rating") val rating: String = "",
    @SerializedName("rating_5based") val rating5: Double = 0.0,
    @SerializedName("backdrop_path") val backdropPath: List<String> = emptyList(),
    @SerializedName("youtube_trailer") val youtubeTrailer: String = "",
    @SerializedName("episode_run_time") val episodeRunTime: String = "",
    @SerializedName("category_id") val categoryId: String = ""
)

data class XtreamSeriesInfo(
    @SerializedName("info") val info: SeriesInfo? = null,
    @SerializedName("episodes") val episodes: Map<String, List<SeriesEpisode>> = emptyMap(),
    @SerializedName("seasons") val seasons: List<SeasonInfo> = emptyList()
)

data class SeriesInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("plot") val plot: String = "",
    @SerializedName("cast") val cast: String = "",
    @SerializedName("director") val director: String = "",
    @SerializedName("genre") val genre: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("rating") val rating: String = ""
)

data class SeasonInfo(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("cover") val cover: String = "",
    @SerializedName("cover_big") val coverBig: String = "",
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("air_date") val airDate: String = ""
)

data class SeriesEpisode(
    @SerializedName("id") val id: String = "",
    @SerializedName("episode_num") val episodeNum: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("container_extension") val containerExtension: String = "mp4",
    @SerializedName("info") val info: EpisodeInfo? = null,
    @SerializedName("added") val added: String = "",
    @SerializedName("season") val season: Int = 0,
    @SerializedName("direct_source") val directSource: String = ""
)

data class EpisodeInfo(
    @SerializedName("tmdb_id") val tmdbId: String = "",
    @SerializedName("releasedate") val releaseDate: String = "",
    @SerializedName("plot") val plot: String = "",
    @SerializedName("duration_secs") val durationSecs: Int = 0,
    @SerializedName("duration") val duration: String = "",
    @SerializedName("movie_image") val movieImage: String = "",
    @SerializedName("video") val video: VideoInfo? = null,
    @SerializedName("audio") val audio: AudioInfo? = null
)

data class VideoInfo(
    @SerializedName("codec_name") val codecName: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

data class AudioInfo(
    @SerializedName("codec_name") val codecName: String = "",
    @SerializedName("channels") val channels: Int = 0,
    @SerializedName("channel_layout") val channelLayout: String = ""
)

// ─── Category ─────────────────────────────────────────────────────────────────
data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("category_name") val categoryName: String = "",
    @SerializedName("parent_id") val parentId: Int = 0
)

// ─── Detected Stream Metadata (stored in DB after first play) ─────────────────
@Entity(tableName = "stream_metadata")
data class StreamMetadata(
    @PrimaryKey
    val streamId: String,              // "vod_12345" or "series_ep_67890"
    val streamType: String = "vod",    // "vod", "series", "live"
    val videoCodec: String = "",       // "H.264", "H.265", "AV1"
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val resolution: String = "",       // "4K", "1080p", "720p", "SD"
    val audioCodec: String = "",       // "AAC", "AC3", "EAC3", "DTS"
    val audioChannels: Int = 0,        // 2, 6, 8
    val audioLayout: String = "",      // "Stereo", "5.1", "7.1"
    val isHdr: Boolean = false,
    val hdrType: String = "",          // "HDR10", "Dolby Vision", "HLG"
    val durationSecs: Long = 0,
    val is4K: Boolean = false,
    val is51: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis()
)

// ─── User VOD State (stored in DB) ────────────────────────────────────────────
@Entity(tableName = "vod_state")
data class VodState(
    @PrimaryKey
    val streamId: String,
    val streamType: String = "vod",
    val title: String = "",
    val posterUrl: String = "",
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isWatched: Boolean = false,
    val resumePositionMs: Long = 0,
    val lastPlayedAt: Long = 0,
    val totalDurationMs: Long = 0
) {
    val progressPercent: Int
        get() = if (totalDurationMs > 0)
            ((resumePositionMs.toFloat() / totalDurationMs) * 100).toInt().coerceIn(0, 100)
        else 0
}
