// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.features.settings.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.databinding.ActivityThemeSettingsBinding
import org.yuzu.yuzu_emu.utils.ThemeHelper

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeSettingsBinding
    private val viewModel: ThemeSettingsViewModel by viewModels()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.setTheme(this)

        super.onCreate(savedInstanceState)

        binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ThemeSettingsFragment())
                .commit()
        }
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, ThemeSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
