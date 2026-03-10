package com.streamlytv.player

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.streamlytv.R
import com.streamlytv.data.model.StreamMetadata
import com.streamlytv.data.repository.VodRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_STREAM_ID = "extra_stream_id"
        const val EXTRA_POSTER_URL = "extra_poster_url"
        const val EXTRA_RESUME_MS = "extra_resume_ms"
        const val EXTRA_USE_HW = "extra_use_hw"
        const val EXTRA_AUDIO_PASSTHROUGH = "extra_audio_passthrough"
        const val EXTRA_BUFFER_MS = "extra_buffer_ms"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var channelName: TextView
    private lateinit var metadataOverlay: TextView

    private val streamUrl by lazy { intent.getStringExtra(EXTRA_URL) ?: "" }
    private val streamName by lazy { intent.getStringExtra(EXTRA_NAME) ?: "" }
    private val streamId by lazy { intent.getStringExtra(EXTRA_STREAM_ID) ?: "" }
    private val resumeMs by lazy { intent.getLongExtra(EXTRA_RESUME_MS, 0L) }
    private val useHwDecoder by lazy { intent.getBooleanExtra(EXTRA_USE_HW, true) }
    private val audioPassthrough by lazy { intent.getBooleanExtra(EXTRA_AUDIO_PASSTHROUGH, true) }
    private val bufferMs by lazy { intent.getIntExtra(EXTRA_BUFFER_MS, 10000) }

    private var vodRepo: VodRepository? = null
    private var metadataSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.playerView)
        channelName = findViewById(R.id.channelName)
        metadataOverlay = findViewById(R.id.metadataOverlay)
        channelName.text = streamName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        if (streamId.isNotEmpty()) vodRepo = VodRepository(this)
        hideSystemUI()
        initializePlayer()
        startProgressSaver()
    }

    private fun initializePlayer() {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val renderersFactory = DefaultRenderersFactory(this).apply {
            if (useHwDecoder) setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(OkHttpDataSource.Factory(httpClient)))
            .setTrackSelector(trackSelector)
            .build()
            .also { exo ->
                playerView.player = exo
                exo.setAudioAttributes(audioAttributes, audioPassthrough)
                exo.setMediaItem(MediaItem.fromUri(streamUrl))
                exo.prepare()
                if (resumeMs > 0) exo.seekTo(resumeMs)
                exo.playWhenReady = true
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> showBuffering(true)
                            Player.STATE_READY -> {
                                showBuffering(false)
                                if (!metadataSaved && streamId.isNotEmpty()) captureMetadata(exo)
                            }
                            Player.STATE_ENDED -> { saveProgress(exo); finish() }
                            else -> {}
                        }
                    }
                })
            }
    }

    private fun captureMetadata(exo: ExoPlayer) {
        lifecycleScope.launch {
            delay(2000)
            var videoCodec = ""; var videoWidth = 0; var videoHeight = 0
            var audioCodec = ""; var audioChannels = 0; var audioLayout = ""
            var isHdr = false; var hdrType = ""

            for (group in exo.currentTracks.groups) {
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> for (i in 0 until group.length) {
                        val f = group.getTrackFormat(i)
                        if (f.width > 0) videoWidth = f.width
                        if (f.height > 0) videoHeight = f.height
                        videoCodec = when {
                            f.sampleMimeType?.contains("hevc", true) == true -> "H.265"
                            f.sampleMimeType?.contains("avc", true) == true -> "H.264"
                            f.sampleMimeType?.contains("av01", true) == true -> "AV1"
                            f.sampleMimeType?.contains("vp9", true) == true -> "VP9"
                            else -> f.sampleMimeType ?: ""
                        }
                        f.colorInfo?.let { c ->
                            when (c.colorTransfer) {
                                C.COLOR_TRANSFER_ST2084 -> { isHdr = true; hdrType = "HDR10" }
                                C.COLOR_TRANSFER_HLG -> { isHdr = true; hdrType = "HLG" }
                            }
                        }
                    }
                    C.TRACK_TYPE_AUDIO -> for (i in 0 until group.length) {
                        val f = group.getTrackFormat(i)
                        audioChannels = f.channelCount
                        audioLayout = when (f.channelCount) { 2 -> "Stereo"; 6 -> "5.1"; 8 -> "7.1"; else -> "${f.channelCount}ch" }
                        audioCodec = when {
                            f.sampleMimeType?.contains("eac3", true) == true -> "EAC3"
                            f.sampleMimeType?.contains("ac3", true) == true -> "AC3"
                            f.sampleMimeType?.contains("aac", true) == true -> "AAC"
                            f.sampleMimeType?.contains("dts", true) == true -> "DTS"
                            f.sampleMimeType?.contains("truehd", true) == true -> "TrueHD"
                            else -> f.sampleMimeType ?: ""
                        }
                    }
                }
            }

            val resolution = when { videoHeight >= 2160 -> "4K"; videoHeight >= 1080 -> "1080p"; videoHeight >= 720 -> "720p"; videoHeight > 0 -> "SD"; else -> "" }

            vodRepo?.saveMetadata(StreamMetadata(
                streamId = streamId, videoCodec = videoCodec, videoWidth = videoWidth, videoHeight = videoHeight,
                resolution = resolution, audioCodec = audioCodec, audioChannels = audioChannels, audioLayout = audioLayout,
                isHdr = isHdr, hdrType = hdrType, durationSecs = exo.duration / 1000,
                is4K = videoHeight >= 2160, is51 = audioChannels >= 6
            ))
            metadataSaved = true

            val parts = listOfNotNull(
                resolution.takeIf { it.isNotEmpty() }, videoCodec.takeIf { it.isNotEmpty() },
                hdrType.takeIf { isHdr }, audioCodec.takeIf { it.isNotEmpty() }, audioLayout.takeIf { it.isNotEmpty() }
            )
            if (parts.isNotEmpty()) {
                metadataOverlay.text = parts.joinToString(" · ")
                metadataOverlay.visibility = View.VISIBLE
                delay(6000)
                metadataOverlay.visibility = View.GONE
            }
        }
    }

    private fun startProgressSaver() {
        if (streamId.isEmpty()) return
        lifecycleScope.launch {
            while (true) {
                delay(15000)
                player?.let { saveProgress(it) }
            }
        }
    }

    private fun saveProgress(exo: ExoPlayer) {
        val pos = exo.currentPosition; val dur = exo.duration
        if (pos > 0 && dur > 0 && streamId.isNotEmpty()) {
            lifecycleScope.launch { vodRepo?.updateProgress(streamId, pos, dur) }
        }
    }

    private fun showBuffering(show: Boolean) {
        findViewById<View>(R.id.bufferingIndicator)?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onStop() { super.onStop(); player?.let { saveProgress(it) }; player?.pause() }
    override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
}
