package com.streamlytv.ui.vod

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.streamlytv.R
import com.streamlytv.data.model.XtreamServer
import com.streamlytv.player.PlayerActivity
import com.streamlytv.utils.PrefsManager

import com.streamlytv.worker.MetadataScanWorker
import com.streamlytv.worker.ScanStatusManager

class VodFragment : Fragment() {

    private val viewModel: VodViewModel by activityViewModels()
    private lateinit var adapter: VodAdapter
    private lateinit var prefs: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_vod, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PrefsManager(requireContext())

        setupRecyclerView(view)
        setupSearch(view)
        setupFilters(view)
        observeViewModel(view)
        observeScanStatus(view)

        // Show server setup if not configured
        if (!prefs.hasXtreamServer) {
            showServerSetupDialog()
        } else {
            viewModel.loadCategories()
            viewModel.loadVodCategory(null)
        }
    }

    private fun setupRecyclerView(view: View) {
        adapter = VodAdapter(
            onItemClick = { item -> openPlayer(item) },
            onLikeClick = { item ->
                viewModel.toggleLike(item.streamId, item.vodItem.name, item.vodItem.streamIcon)
            },
            onDislikeClick = { item ->
                viewModel.toggleDislike(item.streamId, item.vodItem.name, item.vodItem.streamIcon)
            }
        )
        view.findViewById<RecyclerView>(R.id.vodRecyclerView).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@VodFragment.adapter
        }
    }

    private fun setupSearch(view: View) {
        view.findViewById<SearchView>(R.id.vodSearchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = false
                override fun onQueryTextChange(q: String?): Boolean {
                    viewModel.setSearch(q ?: ""); return true
                }
            })
    }

    private fun setupFilters(view: View) {
        view.findViewById<Chip>(R.id.chipVod4K).setOnCheckedChangeListener { _, c ->
            viewModel.setShow4KOnly(c)
        }
        view.findViewById<Chip>(R.id.chipVodHdr).setOnCheckedChangeListener { _, c ->
            viewModel.setShowHdrOnly(c)
        }
        view.findViewById<Chip>(R.id.chipVod51).setOnCheckedChangeListener { _, c ->
            viewModel.setShow51Only(c)
        }
        view.findViewById<Chip>(R.id.chipVodLiked).setOnCheckedChangeListener { _, c ->
            viewModel.setShowLikedOnly(c)
        }
        view.findViewById<Chip>(R.id.chipVodUnwatched).setOnCheckedChangeListener { _, c ->
            viewModel.setShowUnwatchedOnly(c)
        }

        // Category button
        view.findViewById<Button>(R.id.btnVodCategory).setOnClickListener {
            showCategoryDialog()
        }
    }

    private fun observeScanStatus(view: View) {
        val scanBar = view.findViewById<View>(R.id.scanStatusBar)
        val scanText = view.findViewById<TextView>(R.id.scanStatusText)
        val scanSpinner = view.findViewById<ProgressBar>(R.id.scanProgressSpinner)
        val btnScanNow = view.findViewById<Button>(R.id.btnScanNow)

        // Always show scan bar if server configured
        if (prefs.hasXtreamServer) {
            scanBar.visibility = View.VISIBLE
        }

        // Manual trigger
        btnScanNow.setOnClickListener {
            MetadataScanWorker.scheduleImmediate(requireContext())
            scanText.text = "Scan triggered — will run shortly…"
            scanSpinner.visibility = View.VISIBLE
            btnScanNow.isEnabled = false
        }

        // Observe WorkManager state
        ScanStatusManager.getScanWorkInfo(requireContext())
            .observe(viewLifecycleOwner) { workInfos ->
                val isRunning = ScanStatusManager.isScanning(workInfos)
                val lastResult = ScanStatusManager.lastScanResult(workInfos)

                scanSpinner.visibility = if (isRunning) View.VISIBLE else View.GONE
                btnScanNow.isEnabled = !isRunning

                scanText.text = when {
                    isRunning -> "Scanning new content for 4K & 5.1…"
                    lastResult != null -> "Last scan: $lastResult"
                    prefs.hasXtreamServer -> "Tap 'Scan Now' to detect 4K & 5.1 · Auto-scans when idle on WiFi"
                    else -> "Add server to enable scanning"
                }

                // After scan completes, refresh VOD list to show new badges
                if (!isRunning && workInfos.any { it.state.isFinished }) {
                    viewModel.loadVodCategory(null)
                }
            }
    }

    private fun observeViewModel(view: View) {
        val progressBar = view.findViewById<ProgressBar>(R.id.vodProgressBar)
        val emptyView = view.findViewById<TextView>(R.id.vodEmptyView)

        viewModel.filteredVodItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showCategoryDialog() {
        val categories = viewModel.vodCategories.value ?: return
        val names = (listOf("All") + categories.map { it.categoryName }).toTypedArray()
        val ids = (listOf(null) + categories.map { it.categoryId })

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Category")
            .setItems(names) { _, which ->
                viewModel.loadVodCategory(ids[which])
            }
            .show()
    }

    private fun showServerSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_xtream_setup, null)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Connect IPTV Server")
            .setView(dialogView)
            .setPositiveButton("Connect") { _, _ ->
                val url = dialogView.findViewById<EditText>(R.id.inputServerUrl).text.toString().trim()
                val user = dialogView.findViewById<EditText>(R.id.inputUsername).text.toString().trim()
                val pass = dialogView.findViewById<EditText>(R.id.inputPassword).text.toString().trim()

                if (url.isNotEmpty() && user.isNotEmpty()) {
                    prefs.xtreamServer = XtreamServer(url, user, pass)
                    // Start background scanning now that we have a server
                    MetadataScanWorker.schedule(requireContext())
                    MetadataScanWorker.scheduleImmediate(requireContext())
                    viewModel.loadCategories()
                    viewModel.loadVodCategory(null)
                } else {
                    Toast.makeText(requireContext(), "Please fill in server URL and username", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openPlayer(item: VodDisplayItem) {
        val server = prefs.xtreamServer ?: return
        val url = server.vodStreamUrl(item.vodItem.streamId)

        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_NAME, item.vodItem.name)
            putExtra(PlayerActivity.EXTRA_STREAM_ID, item.streamId)
            putExtra(PlayerActivity.EXTRA_POSTER_URL, item.vodItem.streamIcon)
            putExtra(PlayerActivity.EXTRA_RESUME_MS, item.state?.resumePositionMs ?: 0L)
            putExtra(PlayerActivity.EXTRA_USE_HW, prefs.settings.useHardwareDecoder)
            putExtra(PlayerActivity.EXTRA_AUDIO_PASSTHROUGH, prefs.settings.audioPassthrough)
            putExtra(PlayerActivity.EXTRA_BUFFER_MS, prefs.settings.bufferSizeMs)
        }
        startActivity(intent)
    }
}
