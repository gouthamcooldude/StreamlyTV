package com.streamlytv.ui.vod

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.streamlytv.data.model.*
import com.streamlytv.data.repository.VodRepository
import com.streamlytv.utils.PrefsManager
import kotlinx.coroutines.launch

class VodViewModel(application: Application) : AndroidViewModel(application) {

    val vodRepo = VodRepository(application)
    val prefs = PrefsManager(application)

    // ── State ─────────────────────────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // ── Categories ────────────────────────────────────────────────────────────
    private val _vodCategories = MutableLiveData<List<XtreamCategory>>(emptyList())
    val vodCategories: LiveData<List<XtreamCategory>> = _vodCategories

    private val _seriesCategories = MutableLiveData<List<XtreamCategory>>(emptyList())
    val seriesCategories: LiveData<List<XtreamCategory>> = _seriesCategories

    // ── VOD Items ─────────────────────────────────────────────────────────────
    private val _allVodItems = MutableLiveData<List<XtreamVodItem>>(emptyList())

    private val _filteredVodItems = MutableLiveData<List<VodDisplayItem>>(emptyList())
    val filteredVodItems: LiveData<List<VodDisplayItem>> = _filteredVodItems

    // ── Series ────────────────────────────────────────────────────────────────
    private val _seriesItems = MutableLiveData<List<XtreamSeriesItem>>(emptyList())
    val seriesItems: LiveData<List<XtreamSeriesItem>> = _seriesItems

    // ── Filters ───────────────────────────────────────────────────────────────
    private val _searchQuery = MutableLiveData("")
    private val _show4KOnly = MutableLiveData(false)
    private val _showHdrOnly = MutableLiveData(false)
    private val _show51Only = MutableLiveData(false)
    private val _showLikedOnly = MutableLiveData(false)
    private val _showUnwatchedOnly = MutableLiveData(false)
    private val _selectedCategoryId = MutableLiveData<String?>(null)

    val show4KOnly: LiveData<Boolean> = _show4KOnly
    val showHdrOnly: LiveData<Boolean> = _showHdrOnly
    val show51Only: LiveData<Boolean> = _show51Only
    val showLikedOnly: LiveData<Boolean> = _showLikedOnly
    val showUnwatchedOnly: LiveData<Boolean> = _showUnwatchedOnly

    // Metadata + state caches
    private val metadataCache = mutableMapOf<String, StreamMetadata>()
    private val stateCache = mutableMapOf<String, VodState>()

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadVodCategory(categoryId: String?) {
        val repo = vodRepo.getXtreamRepo() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategoryId.value = categoryId
            try {
                val items = repo.getVodStreams(categoryId)
                _allVodItems.value = items
                applyFilters()
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadCategories() {
        val repo = vodRepo.getXtreamRepo() ?: return
        viewModelScope.launch {
            try {
                _vodCategories.value = repo.getVodCategories()
                _seriesCategories.value = repo.getSeriesCategories()
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    fun loadSeries(categoryId: String?) {
        val repo = vodRepo.getXtreamRepo() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _seriesItems.value = repo.getSeriesStreams(categoryId)
            } catch (e: Exception) {
                _error.value = "Failed to load series: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    fun setSearch(query: String) { _searchQuery.value = query; applyFilters() }
    fun setShow4KOnly(v: Boolean) { _show4KOnly.value = v; applyFilters() }
    fun setShowHdrOnly(v: Boolean) { _showHdrOnly.value = v; applyFilters() }
    fun setShow51Only(v: Boolean) { _show51Only.value = v; applyFilters() }
    fun setShowLikedOnly(v: Boolean) { _showLikedOnly.value = v; applyFilters() }
    fun setShowUnwatchedOnly(v: Boolean) { _showUnwatchedOnly.value = v; applyFilters() }

    private fun applyFilters() {
        val items = _allVodItems.value ?: return
        val query = _searchQuery.value ?: ""
        val only4K = _show4KOnly.value == true
        val onlyHdr = _showHdrOnly.value == true
        val only51 = _show51Only.value == true
        val onlyLiked = _showLikedOnly.value == true
        val onlyUnwatched = _showUnwatchedOnly.value == true

        val filtered = items.filter { item ->
            val streamId = "vod_${item.streamId}"
            val meta = metadataCache[streamId]
            val state = stateCache[streamId]

            // Name search
            if (query.isNotEmpty() && !item.name.contains(query, ignoreCase = true)) return@filter false

            // Metadata filters — only apply if we have data
            if (meta != null) {
                if (only4K && !meta.is4K) return@filter false
                if (onlyHdr && !meta.isHdr) return@filter false
                if (only51 && !meta.is51) return@filter false
            } else {
                // If no metadata yet, show item unless strict filtering
                if (only4K) {
                    // Check name-based 4K hints as fallback
                    val nameHas4K = item.name.contains("4K", ignoreCase = true) ||
                                    item.name.contains("UHD", ignoreCase = true) ||
                                    item.name.contains("2160", ignoreCase = true)
                    if (!nameHas4K) return@filter false
                }
            }

            // State filters
            if (onlyLiked && state?.isLiked != true) return@filter false
            if (onlyUnwatched && state?.isWatched == true) return@filter false

            true
        }.map { item ->
            val streamId = "vod_${item.streamId}"
            VodDisplayItem(
                vodItem = item,
                metadata = metadataCache[streamId],
                state = stateCache[streamId]
            )
        }

        _filteredVodItems.value = filtered
    }

    // Called after playback to update cache and refresh list
    fun refreshItemState(streamId: String) {
        viewModelScope.launch {
            val meta = vodRepo.getMetadata(streamId)
            val state = vodRepo.getOrCreateState(streamId, "", "")
            if (meta != null) metadataCache[streamId] = meta
            stateCache[streamId] = state
            applyFilters()
        }
    }

    // ── Like / Dislike ────────────────────────────────────────────────────────

    fun toggleLike(streamId: String, title: String, posterUrl: String) {
        viewModelScope.launch {
            vodRepo.toggleLike(streamId, title, posterUrl)
            refreshItemState(streamId)
        }
    }

    fun toggleDislike(streamId: String, title: String, posterUrl: String) {
        viewModelScope.launch {
            vodRepo.toggleDislike(streamId, title, posterUrl)
            refreshItemState(streamId)
        }
    }

    fun clearError() { _error.value = null }
}

data class VodDisplayItem(
    val vodItem: XtreamVodItem,
    val metadata: StreamMetadata? = null,
    val state: VodState? = null
) {
    val streamId = "vod_${vodItem.streamId}"
    val is4K: Boolean get() = metadata?.is4K
        ?: (vodItem.name.contains("4K", true) || vodItem.name.contains("UHD", true))
    val isHdr: Boolean get() = metadata?.isHdr ?: false
    val is51: Boolean get() = metadata?.is51 ?: false
    val isWatched: Boolean get() = state?.isWatched ?: false
    val isLiked: Boolean get() = state?.isLiked ?: false
    val isDisliked: Boolean get() = state?.isDisliked ?: false
    val resumePercent: Int get() = state?.progressPercent ?: 0
    val audioLabel: String get() = when {
        metadata == null -> ""
        metadata.audioCodec.isNotEmpty() && metadata.audioLayout.isNotEmpty() ->
            "${metadata.audioCodec} ${metadata.audioLayout}"
        metadata.audioCodec.isNotEmpty() -> metadata.audioCodec
        else -> ""
    }
    val resolutionLabel: String get() = metadata?.resolution ?: ""
    val videoCodecLabel: String get() = metadata?.videoCodec ?: ""
    val hdrLabel: String get() = metadata?.hdrType ?: ""
}
