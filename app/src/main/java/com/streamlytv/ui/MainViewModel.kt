package com.streamlytv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.streamlytv.data.model.AppSettings
import com.streamlytv.data.model.Channel
import com.streamlytv.data.model.EpgProgram
import com.streamlytv.data.repository.ChannelRepository
import com.streamlytv.utils.PrefsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChannelRepository(application)
    val prefs = PrefsManager(application)

    // ── Search & Filter State ─────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLanguage = MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private val _show4KOnly = MutableStateFlow(false)
    val show4KOnly: StateFlow<Boolean> = _show4KOnly

    private val _show51Only = MutableStateFlow(false)
    val show51Only: StateFlow<Boolean> = _show51Only

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup

    // ── Channels (reactive, filtered) ─────────────────────────────────────────
    val channels: LiveData<List<Channel>> = combine(
        _searchQuery.debounce(300),
        _selectedLanguage,
        _show4KOnly,
        _show51Only
    ) { query, lang, only4K, only51 ->
        FilterState(query, lang, only4K, only51)
    }.flatMapLatest { state ->
        repository.searchChannels(state.query, state.language, state.only4K, state.only51)
    }.asLiveData()

    val favorites: LiveData<List<Channel>> = repository.getFavorites().asLiveData()
    val groups: LiveData<List<String>> = repository.getAllGroups().asLiveData()

    // ── EPG ───────────────────────────────────────────────────────────────────
    private val _epgData = MutableLiveData<Map<String, List<EpgProgram>>>(emptyMap())
    val epgData: LiveData<Map<String, List<EpgProgram>>> = _epgData

    // ── Loading State ─────────────────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>(null)
    val successMessage: LiveData<String?> = _successMessage

    // ── Settings ──────────────────────────────────────────────────────────────
    private val _settings = MutableLiveData(prefs.settings)
    val settings: LiveData<AppSettings> = _settings

    // ── Actions ───────────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setLanguageFilter(language: String) { _selectedLanguage.value = language }

    fun setShow4KOnly(enabled: Boolean) { _show4KOnly.value = enabled }

    fun setShow51Only(enabled: Boolean) { _show51Only.value = enabled }

    fun setSelectedGroup(group: String) { _selectedGroup.value = group }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addPlaylist(name, url)
            _isLoading.value = false
            result.onSuccess { count ->
                _successMessage.value = "Loaded $count channels"
                loadEpg()
            }.onFailure { e ->
                _errorMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun loadEpg() {
        val epgUrl = prefs.settings.epgUrl
        if (epgUrl.isEmpty()) return
        viewModelScope.launch {
            val data = repository.fetchEpg(epgUrl)
            _epgData.value = data
        }
    }

    fun getCurrentProgram(channelEpgId: String): EpgProgram? {
        val programs = _epgData.value?.get(channelEpgId) ?: return null
        return programs.firstOrNull { it.isLive }
    }

    fun getUpcomingPrograms(channelEpgId: String): List<EpgProgram> {
        val now = System.currentTimeMillis()
        return _epgData.value?.get(channelEpgId)
            ?.filter { it.startTime > now }
            ?.take(5) ?: emptyList()
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccess() { _successMessage.value = null }

    private data class FilterState(
        val query: String,
        val language: String,
        val only4K: Boolean,
        val only51: Boolean
    )
}
