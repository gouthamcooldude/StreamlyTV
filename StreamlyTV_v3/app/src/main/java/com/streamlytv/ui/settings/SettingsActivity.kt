package com.streamlytv.ui.settings

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import com.streamlytv.R
import com.streamlytv.utils.PrefsManager
import com.streamlytv.worker.MetadataScanWorker
import com.streamlytv.worker.ScanStatusManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PrefsManager(this)
        val settings = prefs.settings

        // Hardware decoder toggle
        val switchHwDecoder = findViewById<Switch>(R.id.switchHwDecoder)
        switchHwDecoder.isChecked = settings.useHardwareDecoder
        switchHwDecoder.setOnCheckedChangeListener { _, checked ->
            prefs.settings = prefs.settings.copy(useHardwareDecoder = checked)
        }

        // Audio passthrough toggle (for Dolby 5.1 to soundbar)
        val switchPassthrough = findViewById<Switch>(R.id.switchAudioPassthrough)
        switchPassthrough.isChecked = settings.audioPassthrough
        switchPassthrough.setOnCheckedChangeListener { _, checked ->
            prefs.settings = prefs.settings.copy(audioPassthrough = checked)
        }

        // Language filters
        val checkEnglish = findViewById<CheckBox>(R.id.checkEnglish)
        val checkTelugu = findViewById<CheckBox>(R.id.checkTelugu)
        checkEnglish.isChecked = settings.languageFilters.contains("English")
        checkTelugu.isChecked = settings.languageFilters.contains("Telugu")

        checkEnglish.setOnCheckedChangeListener { _, checked ->
            prefs.updateLanguageFilter("English", checked)
        }
        checkTelugu.setOnCheckedChangeListener { _, checked ->
            prefs.updateLanguageFilter("Telugu", checked)
        }

        // Buffer size
        val bufferSeekBar = findViewById<SeekBar>(R.id.seekBarBuffer)
        val bufferLabel = findViewById<TextView>(R.id.labelBuffer)
        val bufferSeconds = settings.bufferSizeMs / 1000
        bufferSeekBar.progress = bufferSeconds
        bufferLabel.text = "Buffer: ${bufferSeconds}s"
        bufferSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val secs = progress.coerceAtLeast(3)
                bufferLabel.text = "Buffer: ${secs}s"
                prefs.settings = prefs.settings.copy(bufferSizeMs = secs * 1000)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Scan controls
        val scanStatusLabel = findViewById<TextView>(R.id.scanStatusLabel)
        val btnScanNow = findViewById<Button>(R.id.btnScanNow)
        val btnCancelScan = findViewById<Button>(R.id.btnCancelScan)

        ScanStatusManager.getScanWorkInfo(this).observe(this) { workInfos ->
            val isRunning = ScanStatusManager.isScanning(workInfos)
            val lastResult = ScanStatusManager.lastScanResult(workInfos)
            scanStatusLabel.text = when {
                isRunning -> "🔍 Scanning new content…"
                lastResult != null -> "✅ Last scan: $lastResult"
                else -> "Idle — runs automatically when Fire TV is idle on WiFi"
            }
            btnScanNow.isEnabled = !isRunning
        }

        btnScanNow.setOnClickListener {
            MetadataScanWorker.scheduleImmediate(this)
            android.widget.Toast.makeText(this, "Scan triggered", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnCancelScan.setOnClickListener {
            MetadataScanWorker.cancel(this)
            android.widget.Toast.makeText(this, "Scan cancelled", android.widget.Toast.LENGTH_SHORT).show()
        }

        // EPG URL
        val epgInput = findViewById<EditText>(R.id.inputEpgUrl)
        epgInput.setText(settings.epgUrl)
        findViewById<Button>(R.id.btnSaveEpg).setOnClickListener {
            val url = epgInput.text.toString().trim()
            prefs.settings = prefs.settings.copy(epgUrl = url)
            Toast.makeText(this, "EPG URL saved", Toast.LENGTH_SHORT).show()
        }

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }
}
