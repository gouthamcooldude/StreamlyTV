package com.streamlytv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.streamlytv.R
import com.streamlytv.data.model.Channel
import com.streamlytv.player.PlayerActivity
import com.streamlytv.ui.channels.ChannelAdapter
import com.streamlytv.ui.channels.ChannelItem
import com.streamlytv.ui.settings.SettingsActivity
import com.streamlytv.ui.vod.VodFragment

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        fab = findViewById(R.id.fabAddPlaylist)

        fab.setOnClickListener { showAddPlaylistDialog() }
        findViewById<android.widget.ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bottom nav
        val liveContainer = findViewById<View>(R.id.liveContainer)
        val vodContainer = findViewById<View>(R.id.vodContainer)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_live -> {
                    liveContainer.visibility = View.VISIBLE
                    vodContainer.visibility = View.GONE
                    fab.show()
                    true
                }
                R.id.nav_vod -> {
                    liveContainer.visibility = View.GONE
                    vodContainer.visibility = View.VISIBLE
                    fab.hide()
                    true
                }
                else -> false
            }
        }

        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeViewModel()
        viewModel.loadEpg()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.vodContainer, VodFragment())
                .commit()
        }
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(
            onChannelClick = { openLivePlayer(it) },
            onFavoriteClick = { viewModel.toggleFavorite(it) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = channelAdapter
        }
    }

    private fun setupSearch() {
        findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = false
                override fun onQueryTextChange(q: String?): Boolean {
                    viewModel.setSearchQuery(q ?: ""); return true
                }
            })
    }

    private fun setupFilters() {
        findViewById<Chip>(R.id.chip4K).setOnCheckedChangeListener { _, c -> viewModel.setShow4KOnly(c) }
        findViewById<Chip>(R.id.chip51).setOnCheckedChangeListener { _, c -> viewModel.setShow51Only(c) }
        findViewById<Chip>(R.id.chipAll).setOnCheckedChangeListener { _, c -> if (c) viewModel.setLanguageFilter("All") }
        findViewById<Chip>(R.id.chipEnglish).setOnCheckedChangeListener { _, c -> if (c) viewModel.setLanguageFilter("English") }
        findViewById<Chip>(R.id.chipTelugu).setOnCheckedChangeListener { _, c -> if (c) viewModel.setLanguageFilter("Telugu") }
    }

    private fun observeViewModel() {
        viewModel.channels.observe(this) { channels ->
            val items = channels.map { ChannelItem(it, viewModel.getCurrentProgram(it.epgId)) }
            channelAdapter.submitList(items)
            emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(this) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(this) { msg ->
            msg?.let { Snackbar.make(recyclerView, it, Snackbar.LENGTH_LONG).show(); viewModel.clearError() }
        }
        viewModel.successMessage.observe(this) { msg ->
            msg?.let { Snackbar.make(recyclerView, it, Snackbar.LENGTH_SHORT).show(); viewModel.clearSuccess() }
        }
    }

    private fun showAddPlaylistDialog() {
        val dv = layoutInflater.inflate(R.layout.dialog_add_playlist, null)
        AlertDialog.Builder(this).setTitle("Add M3U Playlist").setView(dv)
            .setPositiveButton("Load") { _, _ ->
                val name = dv.findViewById<EditText>(R.id.inputName).text.toString().trim()
                val url = dv.findViewById<EditText>(R.id.inputUrl).text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty()) viewModel.addPlaylist(name, url)
                else Toast.makeText(this, "Fill in both fields", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun openLivePlayer(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, channel.url)
            putExtra(PlayerActivity.EXTRA_NAME, channel.name)
            putExtra(PlayerActivity.EXTRA_USE_HW, viewModel.prefs.settings.useHardwareDecoder)
            putExtra(PlayerActivity.EXTRA_AUDIO_PASSTHROUGH, viewModel.prefs.settings.audioPassthrough)
            putExtra(PlayerActivity.EXTRA_BUFFER_MS, viewModel.prefs.settings.bufferSizeMs)
        })
    }
}
