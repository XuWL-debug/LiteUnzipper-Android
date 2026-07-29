package com.lite.unzipper.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lite.unzipper.BuildConfig
import com.lite.unzipper.R
import com.lite.unzipper.databinding.ActivitySettingsBinding
import com.lite.unzipper.settings.AppSettings
import com.lite.unzipper.settings.DarkMode

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupDarkMode()
        setupThreadCount()
        setupAbout()
    }

    private fun setupDarkMode() {
        binding.darkModeValue.text = settings.darkMode.label
        binding.cardDarkMode.setOnClickListener {
            val items = DarkMode.entries.map { it.label }.toTypedArray()
            val current = DarkMode.entries.indexOf(settings.darkMode)
            MaterialAlertDialogBuilder(this).setTitle(R.string.settings_dark_mode)
                .setSingleChoiceItems(items, current) { dialog, which ->
                    val mode = DarkMode.entries[which]
                    settings.setDarkMode(mode)
                    binding.darkModeValue.text = mode.label
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel, null).show()
        }
    }

    private fun setupThreadCount() {
        val current = settings.threadCount
        binding.threadSlider.valueFrom = 1.0f
        binding.threadSlider.valueTo = 4.0f
        binding.threadSlider.value = current.toFloat()
        updateThreadDisplay(current)
        binding.threadSlider.addOnChangeListener { _, value, _ ->
            val count = value.toInt()
            settings.threadCount = count
            updateThreadDisplay(count)
        }
    }

    private fun updateThreadDisplay(count: Int) {
        binding.threadValue.text = getString(R.string.thread_auto, count)
        binding.threadSummary.text = getString(R.string.settings_thread_count_summary, count)
    }

    private fun setupAbout() {
        binding.aboutText.text = getString(R.string.settings_about_summary, BuildConfig.VERSION_NAME)
    }
}
