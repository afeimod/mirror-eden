// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.features.settings.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.features.settings.model.view.HeaderSetting
import org.yuzu.yuzu_emu.features.settings.model.view.RunnableSetting
import org.yuzu.yuzu_emu.features.settings.model.view.SettingsItem
import org.yuzu.yuzu_emu.features.settings.model.view.SwitchSetting
import org.yuzu.yuzu_emu.utils.ThemeManager

class ThemeSettingsViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _items = MutableLiveData<List<SettingsItem>>()
    val items: LiveData<List<SettingsItem>> = _items

    private val _currentThemeName = MutableLiveData<String>()
    val currentThemeName: LiveData<String> = _currentThemeName

    private val _backgroundEnabled = MutableLiveData<Boolean>()
    val backgroundEnabled: LiveData<Boolean> = _backgroundEnabled

    // Callbacks to be set by the fragment
    var onSelectCustomTheme: (() -> Unit)? = null
    var onResetToDefault: (() -> Unit)? = null

    init {
        loadSettings()
    }

    fun refresh() {
        loadSettings()
    }

    private fun loadSettings() {
        val settingsList = mutableListOf<SettingsItem>()

        // Current Theme Header
        settingsList.add(
            HeaderSetting(
                titleId = R.string.current_theme
            )
        )

        // Background Toggle
        settingsList.add(
            SwitchSetting(
                setting = object : org.yuzu.yuzu_emu.features.settings.model.AbstractSetting {
                    override val key: String = "theme_background_enabled"
                    override val defaultValue: Boolean = true
                    override val isSaveable: Boolean = true
                    override fun getValueAsString(needsGlobal: Boolean): String = 
                        (_backgroundEnabled.value ?: true).toString()
                    override fun reset() {
                        setBackgroundEnabled(true)
                    }
                },
                titleId = R.string.theme_background,
                descriptionId = R.string.theme_background_description,
                isChecked = _backgroundEnabled.value ?: true,
                onCheckedChange = { enabled ->
                    setBackgroundEnabled(enabled)
                }
            )
        )

        // Select Custom Theme
        settingsList.add(
            RunnableSetting(
                titleId = R.string.select_custom_theme,
                descriptionId = R.string.select_custom_theme_description,
                isRunnable = true,
                runnable = {
                    onSelectCustomTheme?.invoke()
                }
            )
        )

        // Reset to Default
        settingsList.add(
            RunnableSetting(
                titleId = R.string.reset_to_default_theme,
                descriptionId = R.string.reset_to_default_theme_description,
                isRunnable = true,
                runnable = {
                    onResetToDefault?.invoke()
                }
            )
        )

        _items.value = settingsList
    }

    fun setCurrentThemeName(name: String) {
        _currentThemeName.value = name
    }

    fun setBackgroundEnabled(enabled: Boolean) {
        _backgroundEnabled.value = enabled
        ThemeManager.getInstance().setBackgroundEnabled(app, enabled)
    }

    fun getCurrentThemeNameValue(): String {
        return _currentThemeName.value ?: ""
    }

    suspend fun applyCustomTheme(uri: Uri): Boolean {
        return try {
            val themeManager = ThemeManager.getInstance()
            themeManager.loadCustomTheme(app, uri)
            setCurrentThemeName(
                app.getString(
                    R.string.current_theme_custom,
                    Uri.parse(uri.toString()).lastPathSegment ?: "Custom"
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetToDefault() {
        val themeManager = ThemeManager.getInstance()
        themeManager.resetToDefault(app)
        setCurrentThemeName(app.getString(R.string.current_theme_default))
    }
}
